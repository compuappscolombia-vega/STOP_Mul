package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.game.GameRules
import com.example.model.ChatMessage
import com.example.model.DailyChallenge
import com.example.model.DailyChallengeCompletion
import com.example.model.Difficulty
import com.example.model.GameRoom
import com.example.model.LeaderboardEntry
import com.example.model.LeaderboardPeriod
import com.example.model.PlayerState
import com.example.model.PublicRoomSummary
import com.example.model.RoomPhase
import com.example.model.RoomSettings
import com.example.model.UserProfile
import com.example.model.UserStats
import java.time.LocalDate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import kotlin.random.Random

class FirebaseRepository(
    private val db: FirebaseFirestore,
    private val prefs: SharedPreferences,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        prefs = context.applicationContext.getSharedPreferences("stop_rush_prefs", Context.MODE_PRIVATE)
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Firestore." }
        return uid
    }

    fun getBotSpeedSec(): Int = prefs.getInt("stop_game_bot_speed", 4).coerceIn(1, 6)

    fun saveBotSpeedSec(speedSec: Int) {
        prefs.edit().putInt("stop_game_bot_speed", speedSec.coerceIn(1, 6)).apply()
    }

    // ==================== USER PROFILE & LEADERBOARD SYNC ====================

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String?,
        email: String?
    ): Result<UserProfile> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in UID mismatch." }
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val existing = FirestoreMappers.snapshotToUserProfile(snap)
            if (existing != null) {
                ensureLeaderboardEntry(existing, uid)
                return Result.success(existing)
            }

            val cleanName = displayName?.trim()?.take(30)?.ifEmpty { null }
                ?: email?.substringBefore('@')?.take(30)?.ifEmpty { null }
                ?: "Player_${Random.nextInt(1000, 9999)}"
            val avatar = GameRules.AVATAR_OPTIONS.random()
            val suffix = Random.nextInt(1000, 9999)
            val created = UserProfile(
                id = uid,
                name = cleanName,
                avatar = avatar,
                syncCode = "STOP-$suffix",
                title = GameRules.getPlayerTier(0).rankTitle,
                stats = UserStats(),
                createdAt = Instant.now().toString(),
                email = email,
                isRegistered = true
            )
            userRef.set(FirestoreMappers.userProfileToCreateMap(created, uid)).await()
            ensureLeaderboardEntry(created, uid)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun ensureLeaderboardEntry(profile: UserProfile, uid: String) {
        val lbRef = db.collection("leaderboards").document(uid)
        val snap = lbRef.get().await()
        if (!snap.exists()) {
            lbRef.set(FirestoreMappers.leaderboardCreateMap(profile, uid)).await()
        } else {
            lbRef.update(FirestoreMappers.leaderboardUpdateMap(profile)).await()
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val currentUid = requireUserId()
        check(currentUid == uid) { "Cannot observe another user's profile." }
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap -> FirestoreMappers.snapshotToUserProfile(snap) }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<UserProfile> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            val tier = GameRules.getPlayerTier(profile.stats.totalPoints)
            val normalized = profile.copy(
                id = activeUid,
                name = profile.name.trim().ifEmpty { "Player" }.take(30),
                avatar = profile.avatar.ifEmpty { "⚡" }.take(16),
                title = tier.rankTitle,
                isRegistered = true
            )
            val userRef = db.collection("users").document(activeUid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                userRef.update(FirestoreMappers.userProfileToUpdateMap(normalized)).await()
            } else {
                userRef.set(FirestoreMappers.userProfileToCreateMap(normalized, activeUid)).await()
            }

            val lbRef = db.collection("leaderboards").document(activeUid)
            val lbSnap = lbRef.get().await()
            if (lbSnap.exists()) {
                lbRef.update(FirestoreMappers.leaderboardUpdateMap(normalized)).await()
            } else {
                lbRef.set(FirestoreMappers.leaderboardCreateMap(normalized, activeUid)).await()
            }
            Result.success(normalized)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureTodayDailyChallenge(dateStr: String = LocalDate.now().toString()): Result<DailyChallenge> {
        val path = "dailyChallenges/$dateStr"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("dailyChallenges").document(dateStr)
            val snap = docRef.get().await()
            val existing = FirestoreMappers.snapshotToDailyChallenge(snap)
            if (existing != null) {
                return Result.success(existing)
            }

            val generated = GameRules.getDailyChallenge(dateStr)
            try {
                docRef.set(FirestoreMappers.dailyChallengeCreateMap(generated, uid)).await()
                Result.success(generated)
            } catch (writeErr: CancellationException) {
                throw writeErr
            } catch (writeErr: Exception) {
                // In case another player created today's challenge concurrently, try reading it back
                val retrySnap = docRef.get().await()
                val concurrent = FirestoreMappers.snapshotToDailyChallenge(retrySnap)
                if (concurrent != null) {
                    Result.success(concurrent)
                } else {
                    throw writeErr
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeDailyCompletions(uid: String): Flow<List<DailyChallengeCompletion>> = flow {
        val currentUid = requireUserId()
        check(currentUid == uid) { "Cannot observe another user's daily completions." }
        emitAll(
            db.collection("users").document(uid)
                .collection("dailyCompletions")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { FirestoreMappers.snapshotToDailyCompletion(it) }
                        .sortedByDescending { it.date }
                }
        )
    }

    suspend fun recordDailyChallengeCompletion(
        updatedProfileAfterRound: UserProfile,
        challenge: DailyChallenge,
        roundScore: Int
    ): Result<Pair<UserProfile, Int>> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/dailyCompletions/${challenge.date}"
        return try {
            val uid = requireUserId()
            // Ensure parent daily challenge document exists in Firestore (required by security rules)
            ensureTodayDailyChallenge(challenge.date)

            val completionRef = db.collection("users").document(uid)
                .collection("dailyCompletions").document(challenge.date)
            val existingSnap = completionRef.get().await()

            var bonusAwardedThisRun = 0
            if (!existingSnap.exists()) {
                val parAchieved = roundScore >= challenge.targetParScore
                bonusAwardedThisRun = when {
                    parAchieved -> challenge.bonusPoints
                    roundScore > 0 -> (challenge.bonusPoints / 2).coerceAtLeast(10)
                    else -> 0
                }
                val completion = DailyChallengeCompletion(
                    date = challenge.date,
                    challengeTitle = challenge.title,
                    letter = challenge.letter,
                    roundScore = roundScore,
                    bonusPointsAwarded = bonusAwardedThisRun,
                    totalAwarded = roundScore + bonusAwardedThisRun,
                    parAchieved = parAchieved
                )
                completionRef.set(FirestoreMappers.dailyCompletionCreateMap(uid, completion)).await()
            }

            val updatedDailyList = (updatedProfileAfterRound.stats.dailyChallengesCompleted + challenge.date)
                .distinct()
                .take(365)

            val profileWithBonus = updatedProfileAfterRound.copy(
                stats = updatedProfileAfterRound.stats.copy(
                    totalPoints = updatedProfileAfterRound.stats.totalPoints + bonusAwardedThisRun,
                    weeklyPoints = updatedProfileAfterRound.stats.weeklyPoints + bonusAwardedThisRun,
                    dailyChallengesCompleted = updatedDailyList
                )
            )
            val savedProfile = saveUserProfile(profileWithBonus).getOrThrow()
            Result.success(savedProfile to bonusAwardedThisRun)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ==================== CROSS-DEVICE LEADERBOARDS ====================

    fun observeLeaderboards(): Flow<List<FirestoreMappers.RawLeaderboardRecord>> = flow {
        requireUserId()
        emitAll(
            db.collection("leaderboards")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        FirestoreMappers.snapshotToRawLeaderboard(doc)
                    }
                }
        )
    }

    fun rankLeaderboardEntries(
        cloudRecords: List<FirestoreMappers.RawLeaderboardRecord>,
        myProfile: UserProfile,
        period: LeaderboardPeriod
    ): List<LeaderboardEntry> {
        val merged = linkedMapOf<String, FirestoreMappers.RawLeaderboardRecord>()
        cloudRecords.forEach { rec -> merged[rec.id] = rec }

        if (!merged.containsKey(myProfile.id) && myProfile.id != "loading") {
            val tier = GameRules.getPlayerTier(myProfile.stats.totalPoints)
            merged[myProfile.id] = FirestoreMappers.RawLeaderboardRecord(
                id = myProfile.id,
                name = myProfile.name,
                avatar = myProfile.avatar,
                title = tier.rankTitle,
                totalPoints = myProfile.stats.totalPoints,
                weeklyPoints = myProfile.stats.weeklyPoints,
                wins = myProfile.stats.wins,
                stopsCalled = myProfile.stats.stopsCalled,
                tier = tier.tier,
                streak = myProfile.stats.currentStreak
            )
        }

        val sorted = merged.values.sortedByDescending { rec ->
            if (period == LeaderboardPeriod.WEEKLY) rec.weeklyPoints else rec.totalPoints
        }
        return sorted.mapIndexed { idx, rec ->
            LeaderboardEntry(
                id = rec.id,
                name = rec.name,
                avatar = rec.avatar,
                title = rec.title,
                totalPoints = if (period == LeaderboardPeriod.WEEKLY) rec.weeklyPoints else rec.totalPoints,
                wins = rec.wins,
                stopsCalled = rec.stopsCalled,
                tier = rec.tier,
                rank = idx + 1,
                streak = rec.streak
            )
        }
    }

    // ==================== MULTIPLAYER ROOMS ====================

    fun observePublicRooms(): Flow<List<PublicRoomSummary>> = flow {
        requireUserId()
        emitAll(
            db.collection("rooms")
                .whereEqualTo("isPrivate", false)
                .snapshots()
                .map { snapshot ->
                    val cloudRooms = snapshot.documents
                        .mapNotNull { FirestoreMappers.snapshotToPublicSummary(it) }
                    val existingCodes = cloudRooms.map { it.code }.toSet()
                    val defaults = buildList {
                        if (!existingCodes.contains("STOP-ARENA")) {
                            add(
                                PublicRoomSummary(
                                    code = "STOP-ARENA",
                                    name = "Global Blitz Arena",
                                    playerCount = 1,
                                    maxRounds = 3,
                                    duration = 60,
                                    hostName = "Open Arena"
                                )
                            )
                        }
                        if (!existingCodes.contains("STOP-FAST")) {
                            add(
                                PublicRoomSummary(
                                    code = "STOP-FAST",
                                    name = "Speed Word Sprint",
                                    playerCount = 1,
                                    maxRounds = 5,
                                    duration = 45,
                                    hostName = "Open Arena"
                                )
                            )
                        }
                    }
                    cloudRooms + defaults
                }
        )
    }

    fun observeRoom(roomCode: String): Flow<GameRoom?> = flow {
        requireUserId()
        emitAll(
            db.collection("rooms").document(roomCode)
                .snapshots()
                .map { snap -> FirestoreMappers.snapshotToRoom(snap) }
        )
    }

    fun observeRoomPlayers(roomCode: String, isPrivate: Boolean): Flow<List<PlayerState>> = flow {
        val uid = requireUserId()
        val baseQuery = db.collection("rooms").document(roomCode).collection("players")
        val alignedQuery = if (isPrivate) {
            baseQuery.whereArrayContains("memberUids", uid)
        } else {
            baseQuery.whereEqualTo("isPublic", true)
        }
        emitAll(
            alignedQuery.snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { FirestoreMappers.snapshotToPlayerState(it) }
                        .sortedWith(compareByDescending<PlayerState> { it.isHost }.thenByDescending { it.totalScore }.thenBy { it.name })
                }
        )
    }

    fun observeRoomMessages(roomCode: String, isPrivate: Boolean): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val baseQuery = db.collection("rooms").document(roomCode).collection("messages")
        val alignedQuery = if (isPrivate) {
            baseQuery.whereArrayContains("memberUids", uid)
        } else {
            baseQuery.whereEqualTo("isPublic", true)
        }
        emitAll(
            alignedQuery.snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { FirestoreMappers.snapshotToChatMessage(it) }
                        .sortedBy { it.timestamp }
                        .takeLast(40)
                }
        )
    }

    suspend fun createRoom(
        profile: UserProfile,
        roomName: String,
        settings: RoomSettings,
        forcedCode: String? = null,
        addPracticeBot: Boolean = false
    ): Result<GameRoom> {
        var roomPath = "rooms"
        return try {
            val uid = requireUserId()
            val code = forcedCode ?: GameRules.generateRoomCode()
            roomPath = "rooms/$code"
            val roomRef = db.collection("rooms").document(code)

            val memberUids = listOf(uid)
            val isPublic = !settings.isPrivate
            val batch = db.batch()
            batch.set(
                roomRef,
                FirestoreMappers.roomCreateMap(
                    code = code,
                    name = roomName,
                    hostId = uid,
                    hostName = profile.name,
                    settings = settings,
                    memberUids = memberUids
                )
            )
            batch.set(
                roomRef.collection("players").document(uid),
                FirestoreMappers.roomPlayerCreateMap(
                    roomCode = code,
                    playerId = uid,
                    name = profile.name,
                    avatar = profile.avatar,
                    isHost = true,
                    isBot = false,
                    isPublic = isPublic,
                    memberUids = memberUids
                )
            )
            if (addPracticeBot) {
                batch.set(
                    roomRef.collection("players").document("bot_vicky"),
                    FirestoreMappers.roomPlayerCreateMap(
                        roomCode = code,
                        playerId = "bot_vicky",
                        name = "Vocabulary Vicky",
                        avatar = "🦉",
                        isHost = false,
                        isBot = true,
                        isPublic = isPublic,
                        memberUids = memberUids
                    )
                )
            }
            val msgId = "sys_${System.currentTimeMillis()}"
            batch.set(
                roomRef.collection("messages").document(msgId),
                FirestoreMappers.roomMessageCreateMap(
                    roomCode = code,
                    senderId = "system",
                    senderName = "System",
                    senderAvatar = "📢",
                    text = "Room $code created by ${profile.name}. Share the code to invite friends!",
                    isSystem = true,
                    isPublic = isPublic,
                    memberUids = memberUids
                )
            )
            batch.commit().await()

            val snap = roomRef.get().await()
            val room = FirestoreMappers.snapshotToRoom(snap)
                ?: throw IllegalStateException("Created room could not be read back.")
            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, roomPath)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(rawCode: String, profile: UserProfile): Result<GameRoom> {
        val code = rawCode.trim().uppercase()
        val roomPath = "rooms/$code"
        return try {
            val uid = requireUserId()
            require(code.length in 4..20 && code.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                "Invalid room code format."
            }

            val roomRef = db.collection("rooms").document(code)
            val snap = roomRef.get().await()
            if (!snap.exists()) {
                // If joining one of the preset open arenas for the first time, spin it up in Firestore
                if (code == "STOP-ARENA") {
                    return createRoom(
                        profile = profile,
                        roomName = "Global Blitz Arena",
                        settings = RoomSettings(
                            roundCount = 3,
                            roundDurationSec = 60,
                            categories = GameRules.DEFAULT_CATEGORIES.take(6),
                            isPrivate = false,
                            difficulty = Difficulty.MEDIUM
                        ),
                        forcedCode = "STOP-ARENA",
                        addPracticeBot = true
                    )
                }
                if (code == "STOP-FAST") {
                    return createRoom(
                        profile = profile,
                        roomName = "Speed Word Sprint",
                        settings = RoomSettings(
                            roundCount = 5,
                            roundDurationSec = 45,
                            categories = GameRules.DEFAULT_CATEGORIES.take(5),
                            isPrivate = false,
                            difficulty = Difficulty.EASY
                        ),
                        forcedCode = "STOP-FAST",
                        addPracticeBot = true
                    )
                }
                throw IllegalArgumentException("Room $code was not found.")
            }

            val room = FirestoreMappers.snapshotToRoom(snap)
                ?: throw IllegalStateException("Invalid room data.")
            val isPrivate = room.settings.isPrivate
            val isPublic = !isPrivate
            if (!room.memberUids.contains(uid) && room.memberUids.size >= 16) {
                throw IllegalStateException("Room $code is full (max 16 players).")
            }
            val updatedMembers = (room.memberUids + uid).distinct().take(16)

            if (!room.memberUids.contains(uid)) {
                roomRef.update(
                    mapOf(
                        "memberUids" to updatedMembers,
                        "playerCount" to updatedMembers.size.coerceIn(1, 16),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val playerRef = roomRef.collection("players").document(uid)
            val myPlayerSnap = playerRef.get().await()
            if (!myPlayerSnap.exists()) {
                playerRef.set(
                    FirestoreMappers.roomPlayerCreateMap(
                        roomCode = code,
                        playerId = uid,
                        name = profile.name,
                        avatar = profile.avatar,
                        isHost = room.hostId == uid,
                        isBot = false,
                        isPublic = isPublic,
                        memberUids = updatedMembers
                    )
                ).await()

                if (isPrivate) {
                    // Add joining member to existing member player docs (plus optional bot) without hostId list query
                    val candidateIds = (room.memberUids.filter { it != uid } + "bot_vicky").distinct()
                    candidateIds.forEach { targetPlayerId ->
                        try {
                            roomRef.collection("players").document(targetPlayerId)
                                .update(
                                    mapOf(
                                        "memberUids" to FieldValue.arrayUnion(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                                .await()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Optional bot doc may not exist in custom private room
                        }
                    }
                }

                val joinMsgId = "sys_join_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}"
                roomRef.collection("messages").document(joinMsgId).set(
                    FirestoreMappers.roomMessageCreateMap(
                        roomCode = code,
                        senderId = "system",
                        senderName = "System",
                        senderAvatar = "👋",
                        text = "${profile.name} joined the room!",
                        isSystem = true,
                        isPublic = isPublic,
                        memberUids = updatedMembers
                    )
                ).await()
            }

            val latest = FirestoreMappers.snapshotToRoom(roomRef.get().await()) ?: room
            Result.success(latest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, roomPath)
            Result.failure(e)
        }
    }

    suspend fun startRound(
        room: GameRoom,
        nextRound: Int,
        letter: String
    ): Result<Unit> {
        val roomPath = "rooms/${room.code}"
        return try {
            val uid = requireUserId()
            check(room.hostId == uid) { "Only the host can start the round." }
            val roomRef = db.collection("rooms").document(room.code)
            val isPublic = !room.settings.isPrivate

            val batch = db.batch()
            room.players.forEach { player ->
                val pRef = roomRef.collection("players").document(player.id)
                batch.update(
                    pRef,
                    mapOf(
                        "answers" to emptyMap<String, String>(),
                        "hasStopped" to false,
                        "stopElapsedSec" to -1,
                        "filledCount" to 0,
                        "statusText" to "Ready",
                        "roundScore" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                roomRef,
                mapOf(
                    "phase" to RoomPhase.ROUND_ACTIVE.name,
                    "currentRound" to nextRound,
                    "currentLetter" to letter,
                    "stopperPlayerId" to "",
                    "stopperName" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val msgId = "sys_round_${nextRound}_${System.currentTimeMillis()}"
            batch.set(
                roomRef.collection("messages").document(msgId),
                FirestoreMappers.roomMessageCreateMap(
                    roomCode = room.code,
                    senderId = "system",
                    senderName = "System",
                    senderAvatar = "🎯",
                    text = "Round $nextRound started with letter '$letter'! Fill the categories fast!",
                    isSystem = true,
                    isPublic = isPublic,
                    memberUids = room.memberUids.ifEmpty { listOf(uid) }
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomPath)
            Result.failure(e)
        }
    }

    suspend fun syncMyPlayerAnswers(
        roomCode: String,
        answers: Map<String, String>,
        filledCount: Int,
        statusText: String,
        answeredRound: Int,
        hasStopped: Boolean = false,
        stopElapsedSec: Int? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val playerPath = "rooms/$roomCode/players/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            val sanitizedAnswers = answers.entries
                .take(16)
                .associate { (k, v) -> k.take(40) to v.take(60) }
            db.collection("rooms").document(roomCode)
                .collection("players").document(activeUid)
                .update(
                    mapOf(
                        "answers" to sanitizedAnswers,
                        "filledCount" to filledCount.coerceIn(0, 16),
                        "statusText" to statusText.take(120),
                        "answeredRound" to answeredRound.coerceIn(0, 20),
                        "hasStopped" to hasStopped,
                        "stopElapsedSec" to (stopElapsedSec ?: -1).coerceIn(-1, 300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, playerPath)
            Result.failure(e)
        }
    }

    suspend fun syncMyRoundScore(
        roomCode: String,
        roundScore: Int,
        totalScore: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val playerPath = "rooms/$roomCode/players/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            db.collection("rooms").document(roomCode)
                .collection("players").document(activeUid)
                .update(
                    mapOf(
                        "roundScore" to roundScore.coerceIn(0, 10000),
                        "totalScore" to totalScore.coerceIn(0, 1000000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, playerPath)
            Result.failure(e)
        }
    }

    suspend fun updateBotPlayerState(
        roomCode: String,
        botId: String,
        answers: Map<String, String>,
        filledCount: Int,
        statusText: String,
        answeredRound: Int,
        hasStopped: Boolean = false,
        stopElapsedSec: Int? = null,
        roundScore: Int? = null,
        totalScore: Int? = null
    ): Result<Unit> {
        val path = "rooms/$roomCode/players/$botId"
        return try {
            requireUserId()
            val payload = mutableMapOf<String, Any>(
                "answers" to answers,
                "filledCount" to filledCount.coerceIn(0, 16),
                "statusText" to statusText.take(120),
                "answeredRound" to answeredRound.coerceIn(0, 20),
                "hasStopped" to hasStopped,
                "stopElapsedSec" to (stopElapsedSec ?: -1).coerceIn(-1, 300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (roundScore != null) payload["roundScore"] = roundScore.coerceIn(0, 10000)
            if (totalScore != null) payload["totalScore"] = totalScore.coerceIn(0, 1000000)
            db.collection("rooms").document(roomCode)
                .collection("players").document(botId)
                .update(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun callRoomStop(
        room: GameRoom,
        stopperId: String,
        stopperName: String
    ): Result<Unit> {
        val roomPath = "rooms/${room.code}"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("rooms").document(room.code)
            val transitioned = db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (!snap.exists()) return@runTransaction false
                val currentPhase = snap.getString("phase") ?: ""
                if (currentPhase != RoomPhase.ROUND_ACTIVE.name) return@runTransaction false
                tx.update(
                    roomRef,
                    mapOf(
                        "phase" to RoomPhase.STOP_CALLED.name,
                        "stopperPlayerId" to stopperId.take(128),
                        "stopperName" to stopperName.take(40),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                true
            }.await()

            if (transitioned) {
                val msgId = "sys_stop_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}"
                roomRef.collection("messages").document(msgId).set(
                    FirestoreMappers.roomMessageCreateMap(
                        roomCode = room.code,
                        senderId = "system",
                        senderName = "System",
                        senderAvatar = "🛑",
                        text = "🚨 STOP CALLED BY ${stopperName.uppercase()}! Pens down!",
                        isSystem = true,
                        isPublic = !room.settings.isPrivate,
                        memberUids = room.memberUids.ifEmpty { listOf(uid) }
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomPath)
            Result.failure(e)
        }
    }

    suspend fun sendRoomMessage(
        room: GameRoom,
        senderName: String,
        senderAvatar: String,
        text: String
    ): Result<Unit> {
        val msgId = "msg_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}"
        val path = "rooms/${room.code}/messages/$msgId"
        return try {
            val uid = requireUserId()
            val clean = text.trim().take(240)
            require(clean.isNotEmpty()) { "Message cannot be empty." }
            db.collection("rooms").document(room.code)
                .collection("messages").document(msgId)
                .set(
                    FirestoreMappers.roomMessageCreateMap(
                        roomCode = room.code,
                        senderId = uid,
                        senderName = senderName,
                        senderAvatar = senderAvatar,
                        text = clean,
                        isSystem = false,
                        isPublic = !room.settings.isPrivate,
                        memberUids = room.memberUids.ifEmpty { listOf(uid) }
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(room: GameRoom): Result<Unit> {
        val roomPath = "rooms/${room.code}"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("rooms").document(room.code)
            val remainingHumans = room.players.filter { !it.isBot && it.id != uid }
            val remainingMembers = room.memberUids.filter { it != uid }

            // Delete my player doc first while still a member
            roomRef.collection("players").document(uid).delete().await()

            if (remainingMembers.isEmpty()) {
                // Clean up bots & messages before deleting empty room
                room.players.filter { it.isBot }.forEach { bot ->
                    roomRef.collection("players").document(bot.id).delete().await()
                }
                if (room.hostId == uid) {
                    roomRef.delete().await()
                }
            } else {
                val nextHost = remainingHumans.firstOrNull()
                val updateMap = mutableMapOf<String, Any>(
                    "memberUids" to remainingMembers,
                    "playerCount" to remainingMembers.size.coerceIn(1, 16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (room.hostId == uid && nextHost != null) {
                    updateMap["hostId"] = nextHost.id
                    updateMap["hostName"] = nextHost.name
                    roomRef.collection("players").document(nextHost.id)
                        .update(mapOf("isHost" to true, "updatedAt" to FieldValue.serverTimestamp()))
                        .await()
                }
                roomRef.update(updateMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, roomPath)
            Result.failure(e)
        }
    }
}
