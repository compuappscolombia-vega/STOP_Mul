package com.example.data

import com.example.game.GameRules
import com.example.model.ChatMessage
import com.example.model.Difficulty
import com.example.model.GameRoom
import com.example.model.LeaderboardEntry
import com.example.model.PlayerState
import com.example.model.PublicRoomSummary
import com.example.model.RoomPhase
import com.example.model.RoomSettings
import com.example.model.UserProfile
import com.example.model.UserStats
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

object FirestoreMappers {

    fun userProfileToCreateMap(profile: UserProfile, uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to uid,
            "name" to profile.name.trim().ifEmpty { "Player" }.take(30),
            "avatar" to profile.avatar.ifEmpty { "⚡" }.take(16),
            "syncCode" to profile.syncCode.ifEmpty { "STOP-0000" }.take(16),
            "title" to profile.title.ifEmpty { "Rookie Rusher" }.take(40),
            "email" to profile.email?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
            "gamesPlayed" to profile.stats.gamesPlayed.coerceAtLeast(0),
            "wins" to profile.stats.wins.coerceAtLeast(0),
            "stopsCalled" to profile.stats.stopsCalled.coerceAtLeast(0),
            "totalPoints" to profile.stats.totalPoints.coerceAtLeast(0),
            "weeklyPoints" to profile.stats.weeklyPoints.coerceAtLeast(0),
            "bestRoundScore" to profile.stats.bestRoundScore.coerceAtLeast(0),
            "currentStreak" to profile.stats.currentStreak.coerceAtLeast(0),
            "dailyChallengesCompleted" to profile.stats.dailyChallengesCompleted.take(365),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }

    fun userProfileToUpdateMap(profile: UserProfile): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "name" to profile.name.trim().ifEmpty { "Player" }.take(30),
            "avatar" to profile.avatar.ifEmpty { "⚡" }.take(16),
            "title" to profile.title.ifEmpty { "Rookie Rusher" }.take(40),
            "email" to profile.email?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
            "gamesPlayed" to profile.stats.gamesPlayed.coerceAtLeast(0),
            "wins" to profile.stats.wins.coerceAtLeast(0),
            "stopsCalled" to profile.stats.stopsCalled.coerceAtLeast(0),
            "totalPoints" to profile.stats.totalPoints.coerceAtLeast(0),
            "weeklyPoints" to profile.stats.weeklyPoints.coerceAtLeast(0),
            "bestRoundScore" to profile.stats.bestRoundScore.coerceAtLeast(0),
            "currentStreak" to profile.stats.currentStreak.coerceAtLeast(0),
            "dailyChallengesCompleted" to profile.stats.dailyChallengesCompleted.take(365),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }

    fun snapshotToUserProfile(doc: DocumentSnapshot): UserProfile? {
        if (!doc.exists()) return null
        val uid = doc.getString("userId") ?: doc.id
        val totalPts = (doc.getLong("totalPoints") ?: 0L).toInt()
        val dailyList = (doc.get("dailyChallengesCompleted") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        val createdTs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val tierTitle = GameRules.getPlayerTier(totalPts).rankTitle
        return UserProfile(
            id = uid,
            name = doc.getString("name") ?: "Player",
            avatar = doc.getString("avatar") ?: "⚡",
            syncCode = doc.getString("syncCode") ?: "STOP-0000",
            title = doc.getString("title") ?: tierTitle,
            stats = UserStats(
                gamesPlayed = (doc.getLong("gamesPlayed") ?: 0L).toInt(),
                wins = (doc.getLong("wins") ?: 0L).toInt(),
                stopsCalled = (doc.getLong("stopsCalled") ?: 0L).toInt(),
                totalPoints = totalPts,
                weeklyPoints = (doc.getLong("weeklyPoints") ?: 0L).toInt(),
                bestRoundScore = (doc.getLong("bestRoundScore") ?: 0L).toInt(),
                currentStreak = (doc.getLong("currentStreak") ?: 0L).toInt(),
                dailyChallengesCompleted = dailyList
            ),
            createdAt = (createdTs ?: Timestamp.now()).toDate().toInstant().toString(),
            email = doc.getString("email"),
            isRegistered = true
        )
    }

    fun leaderboardCreateMap(profile: UserProfile, uid: String): Map<String, Any> {
        val tier = GameRules.getPlayerTier(profile.stats.totalPoints)
        return mapOf(
            "userId" to uid,
            "name" to profile.name.trim().ifEmpty { "Player" }.take(30),
            "avatar" to profile.avatar.ifEmpty { "⚡" }.take(16),
            "title" to tier.rankTitle.take(40),
            "totalPoints" to profile.stats.totalPoints.coerceAtLeast(0),
            "weeklyPoints" to profile.stats.weeklyPoints.coerceAtLeast(0),
            "wins" to profile.stats.wins.coerceAtLeast(0),
            "stopsCalled" to profile.stats.stopsCalled.coerceAtLeast(0),
            "streak" to profile.stats.currentStreak.coerceAtLeast(0),
            "tier" to tier.tier.take(20),
            "isPublic" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun leaderboardUpdateMap(profile: UserProfile): Map<String, Any> {
        val tier = GameRules.getPlayerTier(profile.stats.totalPoints)
        return mapOf(
            "name" to profile.name.trim().ifEmpty { "Player" }.take(30),
            "avatar" to profile.avatar.ifEmpty { "⚡" }.take(16),
            "title" to tier.rankTitle.take(40),
            "totalPoints" to profile.stats.totalPoints.coerceAtLeast(0),
            "weeklyPoints" to profile.stats.weeklyPoints.coerceAtLeast(0),
            "wins" to profile.stats.wins.coerceAtLeast(0),
            "stopsCalled" to profile.stats.stopsCalled.coerceAtLeast(0),
            "streak" to profile.stats.currentStreak.coerceAtLeast(0),
            "tier" to tier.tier.take(20),
            "isPublic" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    data class RawLeaderboardRecord(
        val id: String,
        val name: String,
        val avatar: String,
        val title: String,
        val totalPoints: Int,
        val weeklyPoints: Int,
        val wins: Int,
        val stopsCalled: Int,
        val tier: String,
        val streak: Int
    )

    fun snapshotToRawLeaderboard(doc: DocumentSnapshot): RawLeaderboardRecord? {
        if (!doc.exists()) return null
        val totalPts = (doc.getLong("totalPoints") ?: 0L).toInt()
        return RawLeaderboardRecord(
            id = doc.getString("userId") ?: doc.id,
            name = doc.getString("name") ?: "Player",
            avatar = doc.getString("avatar") ?: "⚡",
            title = doc.getString("title") ?: GameRules.getPlayerTier(totalPts).rankTitle,
            totalPoints = totalPts,
            weeklyPoints = (doc.getLong("weeklyPoints") ?: 0L).toInt(),
            wins = (doc.getLong("wins") ?: 0L).toInt(),
            stopsCalled = (doc.getLong("stopsCalled") ?: 0L).toInt(),
            tier = doc.getString("tier") ?: GameRules.getPlayerTier(totalPts).tier,
            streak = (doc.getLong("streak") ?: 0L).toInt()
        )
    }

    fun roomCreateMap(
        code: String,
        name: String,
        hostId: String,
        hostName: String,
        settings: RoomSettings,
        memberUids: List<String>
    ): Map<String, Any> {
        val catIds = settings.categories.map { it.id }.ifEmpty {
            GameRules.DEFAULT_CATEGORIES.take(6).map { it.id }
        }
        return mapOf(
            "code" to code,
            "name" to name.trim().ifEmpty { "Party Room" }.take(40),
            "hostId" to hostId,
            "hostName" to hostName.trim().ifEmpty { "Host" }.take(30),
            "roundCount" to settings.roundCount.coerceIn(1, 10),
            "roundDurationSec" to settings.roundDurationSec.coerceIn(15, 180),
            "categoryIds" to catIds.take(12),
            "isPrivate" to settings.isPrivate,
            "difficulty" to settings.difficulty.name,
            "phase" to RoomPhase.LOBBY.name,
            "currentRound" to 0,
            "currentLetter" to "",
            "stopperPlayerId" to "",
            "stopperName" to "",
            "memberUids" to memberUids.distinct().take(16),
            "playerCount" to memberUids.distinct().size.coerceIn(1, 16),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun snapshotToRoom(
        doc: DocumentSnapshot,
        players: List<PlayerState> = emptyList(),
        chat: List<ChatMessage> = emptyList()
    ): GameRoom? {
        if (!doc.exists()) return null
        val code = doc.getString("code") ?: doc.id
        val rawCatIds = (doc.get("categoryIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val categories = rawCatIds.mapNotNull { cid ->
            GameRules.DEFAULT_CATEGORIES.firstOrNull { it.id == cid }
        }.ifEmpty { GameRules.DEFAULT_CATEGORIES.take(6) }

        val rawDiff = doc.getString("difficulty") ?: Difficulty.MEDIUM.name
        val difficulty = enumValues<Difficulty>().firstOrNull { it.name == rawDiff } ?: Difficulty.MEDIUM

        val rawPhase = doc.getString("phase") ?: RoomPhase.LOBBY.name
        val phase = enumValues<RoomPhase>().firstOrNull { it.name == rawPhase } ?: RoomPhase.LOBBY

        val roundDurationSec = (doc.getLong("roundDurationSec") ?: 60L).toInt()
        val stopperId = doc.getString("stopperPlayerId").orEmpty().ifBlank { null }
        val stopperName = doc.getString("stopperName").orEmpty().ifBlank { null }
        val memberUids = (doc.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val updatedTs = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        return GameRoom(
            code = code,
            name = doc.getString("name") ?: "Room $code",
            hostId = doc.getString("hostId") ?: "",
            settings = RoomSettings(
                roundCount = (doc.getLong("roundCount") ?: 3L).toInt(),
                roundDurationSec = roundDurationSec,
                categories = categories,
                isPrivate = doc.getBoolean("isPrivate") ?: false,
                difficulty = difficulty
            ),
            phase = phase,
            currentRound = (doc.getLong("currentRound") ?: 0L).toInt(),
            currentLetter = doc.getString("currentLetter") ?: "",
            phaseTimeRemaining = roundDurationSec,
            phaseStartTime = updatedTs?.toDate()?.time ?: System.currentTimeMillis(),
            stopperPlayerId = stopperId,
            stopperName = stopperName,
            memberUids = memberUids,
            players = players,
            chat = chat
        )
    }

    fun snapshotToPublicSummary(doc: DocumentSnapshot): PublicRoomSummary? {
        if (!doc.exists()) return null
        val code = doc.getString("code") ?: doc.id
        return PublicRoomSummary(
            code = code,
            name = doc.getString("name") ?: "Room $code",
            playerCount = (doc.getLong("playerCount") ?: 1L).toInt(),
            maxRounds = (doc.getLong("roundCount") ?: 3L).toInt(),
            duration = (doc.getLong("roundDurationSec") ?: 60L).toInt(),
            hostName = doc.getString("hostName") ?: "Host"
        )
    }

    fun roomPlayerCreateMap(
        roomCode: String,
        playerId: String,
        name: String,
        avatar: String,
        isHost: Boolean,
        isBot: Boolean,
        isPublic: Boolean,
        memberUids: List<String>
    ): Map<String, Any> {
        return mapOf(
            "roomCode" to roomCode,
            "playerId" to playerId,
            "name" to name.trim().ifEmpty { "Player" }.take(30),
            "avatar" to avatar.ifEmpty { "⚡" }.take(16),
            "isHost" to isHost,
            "isBot" to isBot,
            "isReady" to true,
            "isPublic" to isPublic,
            "memberUids" to memberUids.distinct().take(16),
            "totalScore" to 0,
            "roundScore" to 0,
            "answers" to emptyMap<String, String>(),
            "hasStopped" to false,
            "stopElapsedSec" to -1,
            "filledCount" to 0,
            "statusText" to "Ready",
            "answeredRound" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun snapshotToPlayerState(doc: DocumentSnapshot): PlayerState? {
        if (!doc.exists()) return null
        val rawAnswers = (doc.get("answers") as? Map<*, *>)?.entries
            ?.mapNotNull { (k, v) ->
                val ks = k as? String
                val vs = v as? String
                if (ks != null && vs != null) ks to vs else null
            }?.toMap().orEmpty()
        val stopSecRaw = (doc.getLong("stopElapsedSec") ?: -1L).toInt()
        return PlayerState(
            id = doc.getString("playerId") ?: doc.id,
            name = doc.getString("name") ?: "Player",
            avatar = doc.getString("avatar") ?: "⚡",
            isHost = doc.getBoolean("isHost") ?: false,
            isBot = doc.getBoolean("isBot") ?: false,
            isReady = doc.getBoolean("isReady") ?: true,
            totalScore = (doc.getLong("totalScore") ?: 0L).toInt(),
            roundScore = (doc.getLong("roundScore") ?: 0L).toInt(),
            answers = rawAnswers,
            hasStopped = doc.getBoolean("hasStopped") ?: false,
            stopElapsedSec = if (stopSecRaw >= 0) stopSecRaw else null,
            filledCount = (doc.getLong("filledCount") ?: 0L).toInt(),
            statusText = doc.getString("statusText") ?: "Ready",
            answeredRound = (doc.getLong("answeredRound") ?: 0L).toInt()
        )
    }

    fun roomMessageCreateMap(
        roomCode: String,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        text: String,
        isSystem: Boolean,
        isPublic: Boolean,
        memberUids: List<String>
    ): Map<String, Any> {
        return mapOf(
            "roomCode" to roomCode,
            "senderId" to senderId,
            "senderName" to senderName.trim().ifEmpty { "Player" }.take(30),
            "senderAvatar" to senderAvatar.ifEmpty { "⚡" }.take(16),
            "text" to text.trim().take(240),
            "isSystem" to isSystem,
            "isPublic" to isPublic,
            "memberUids" to memberUids.distinct().take(16),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    fun snapshotToChatMessage(doc: DocumentSnapshot): ChatMessage? {
        if (!doc.exists()) return null
        val ts = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        return ChatMessage(
            id = doc.id,
            senderId = doc.getString("senderId") ?: "",
            senderName = doc.getString("senderName") ?: "Player",
            senderAvatar = doc.getString("senderAvatar") ?: "⚡",
            text = doc.getString("text") ?: "",
            timestamp = ts?.toDate()?.time ?: System.currentTimeMillis(),
            isSystem = doc.getBoolean("isSystem") ?: false
        )
    }

    fun dailyChallengeCreateMap(
        challenge: com.example.model.DailyChallenge,
        createdByUid: String
    ): Map<String, Any> {
        val catIds = challenge.categories.map { it.id }.ifEmpty {
            GameRules.DEFAULT_CATEGORIES.take(5).map { it.id }
        }.take(12)
        val desc = challenge.taskDescription.trim()
            .ifEmpty { "Score ${challenge.targetParScore}+ pts with letter '${challenge.letter.uppercase()}'!" }
            .take(180)
        return mapOf(
            "date" to challenge.date.take(20),
            "title" to challenge.title.trim().ifEmpty { "Daily Sprint" }.take(60),
            "taskDescription" to desc,
            "letter" to challenge.letter.uppercase().take(4),
            "categoryIds" to catIds,
            "targetParScore" to challenge.targetParScore.coerceIn(10, 500),
            "timeLimitSec" to challenge.timeLimitSec.coerceIn(15, 180),
            "bonusPoints" to challenge.bonusPoints.coerceIn(10, 500),
            "isActive" to true,
            "createdBy" to createdByUid.take(128),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    fun snapshotToDailyChallenge(doc: DocumentSnapshot): com.example.model.DailyChallenge? {
        if (!doc.exists()) return null
        val date = doc.getString("date") ?: doc.id
        val rawCatIds = (doc.get("categoryIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val categories = rawCatIds.mapNotNull { cid ->
            GameRules.DEFAULT_CATEGORIES.firstOrNull { it.id == cid }
        }.ifEmpty { GameRules.DEFAULT_CATEGORIES.take(5) }
        val letter = doc.getString("letter") ?: "S"
        val par = (doc.getLong("targetParScore") ?: 45L).toInt()
        return com.example.model.DailyChallenge(
            date = date,
            letter = letter,
            categories = categories,
            targetParScore = par,
            timeLimitSec = (doc.getLong("timeLimitSec") ?: 45L).toInt(),
            title = doc.getString("title") ?: "Daily Sprint",
            taskDescription = doc.getString("taskDescription")
                ?: "Score $par+ pts with letter '${letter.uppercase()}'!",
            bonusPoints = (doc.getLong("bonusPoints") ?: 50L).toInt()
        )
    }

    fun dailyCompletionCreateMap(
        uid: String,
        completion: com.example.model.DailyChallengeCompletion
    ): Map<String, Any> {
        return mapOf(
            "userId" to uid.take(128),
            "date" to completion.date.take(20),
            "challengeTitle" to completion.challengeTitle.trim().ifEmpty { "Daily Sprint" }.take(60),
            "letter" to completion.letter.uppercase().take(4),
            "roundScore" to completion.roundScore.coerceIn(0, 10000),
            "bonusPointsAwarded" to completion.bonusPointsAwarded.coerceIn(0, 500),
            "totalAwarded" to completion.totalAwarded.coerceIn(0, 10500),
            "parAchieved" to completion.parAchieved,
            "completedAt" to FieldValue.serverTimestamp()
        )
    }

    fun snapshotToDailyCompletion(doc: DocumentSnapshot): com.example.model.DailyChallengeCompletion? {
        if (!doc.exists()) return null
        val ts = doc.getTimestamp("completedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        return com.example.model.DailyChallengeCompletion(
            date = doc.getString("date") ?: doc.id,
            challengeTitle = doc.getString("challengeTitle") ?: "Daily Sprint",
            letter = doc.getString("letter") ?: "S",
            roundScore = (doc.getLong("roundScore") ?: 0L).toInt(),
            bonusPointsAwarded = (doc.getLong("bonusPointsAwarded") ?: 0L).toInt(),
            totalAwarded = (doc.getLong("totalAwarded") ?: 0L).toInt(),
            parAchieved = doc.getBoolean("parAchieved") ?: false,
            completedAtMillis = ts?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
