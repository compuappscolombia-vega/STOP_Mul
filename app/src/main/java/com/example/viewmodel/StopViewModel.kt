package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.FirebaseRepository
import com.example.data.FirestoreMappers
import com.example.data.extractRootFirestoreMessage
import com.example.game.GameRules
import com.example.model.ActivePlayerStripItem
import com.example.model.AuthProvider
import com.example.model.AuthUser
import com.example.model.BotProgressState
import com.example.model.Category
import com.example.model.DailyChallenge
import com.example.model.DailyChallengeCompletion
import com.example.model.Difficulty
import com.example.model.GameRoom
import com.example.model.LeaderboardEntry
import com.example.model.LeaderboardPeriod
import com.example.model.PlayerRoundScore
import com.example.model.PlayerRoundSubmission
import com.example.model.PublicRoomSummary
import com.example.model.RoomPhase
import com.example.model.RoomSettings
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.max
import kotlin.random.Random

private const val VM_TAG = "StopViewModel"

enum class AppRoute {
    TITLE,
    PRACTICE,
    DAILY_PLAY,
    ONLINE_LOBBY,
    ONLINE_ROOM
}

data class StopUiState(
    val route: AppRoute = AppRoute.TITLE,
    val profile: UserProfile = UserProfile(
        id = "loading",
        name = "Player",
        avatar = "⚡",
        syncCode = "STOP-0000"
    ),
    val currentUser: AuthUser? = null,
    val isMuted: Boolean = false,
    val selectedDifficulty: Difficulty = Difficulty.MEDIUM,
    val botSpeedSec: Int = 4,
    val toastMessage: String? = null,

    // Modals
    val showProfileModal: Boolean = false,
    val showAuthModal: Boolean = false,
    val showLeaderboardsModal: Boolean = false,
    val showDailyModal: Boolean = false,
    val isPaused: Boolean = false,

    // STOP Celebration & Applause Stage
    val showStopCelebration: Boolean = false,
    val celebrationFilledCount: Int = 0,
    val celebrationTotalCount: Int = 6,

    // Practice / Daily Play
    val practiceRound: Int = 1,
    val totalPracticeRounds: Int = 3,
    val practiceTotalTimeSec: Int = 60,
    val practiceLetter: String = "S",
    val practiceCategories: List<Category> = GameRules.DEFAULT_CATEGORIES.take(6),
    val practiceAnswers: Map<String, String> = emptyMap(),
    val practiceTimeRemaining: Int = 60,
    val isSpinningPractice: Boolean = false,
    val isPracticeSummary: Boolean = false,
    val practiceScores: List<PlayerRoundScore> = emptyList(),
    val practiceStopperId: String? = null,
    val isDailyChallengeRun: Boolean = false,
    val lastDailyBonusAwarded: Int = 0,
    val botProgress: Map<String, BotProgressState> = emptyMap(),

    // Leaderboards & Daily Challenge
    val leaderboardPeriod: LeaderboardPeriod = LeaderboardPeriod.ALL,
    val leaderboardEntries: List<LeaderboardEntry> = emptyList(),
    val isLeaderboardLoading: Boolean = true,
    val dailyChallenge: DailyChallenge = GameRules.getDailyChallenge(),
    val dailyCompletions: List<DailyChallengeCompletion> = emptyList(),

    // Multiplayer Lobby & Room
    val publicRooms: List<PublicRoomSummary> = emptyList(),
    val activeRoom: GameRoom? = null,
    val roomLocalAnswers: Map<String, String> = emptyMap(),
    val roomTimeRemaining: Int = 60,
    val isSpinningRoom: Boolean = false,
    val roomCalculatedScores: List<PlayerRoundScore> = emptyList()
)

class StopViewModel(
    private val repository: FirebaseRepository,
    val soundEngine: SoundEngine,
    private val currentUserId: String,
    initialDisplayName: String?,
    initialEmail: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StopUiState(
            isMuted = soundEngine.isMuted(),
            botSpeedSec = repository.getBotSpeedSec(),
            dailyChallenge = GameRules.getDailyChallenge(),
            currentUser = AuthUser(
                id = currentUserId,
                email = initialEmail ?: "Google Account",
                name = initialDisplayName ?: "Player",
                avatar = "⚡",
                authProvider = AuthProvider.GOOGLE,
                createdAt = Instant.now().toString()
            ),
            profile = UserProfile(
                id = currentUserId,
                name = initialDisplayName ?: "Player",
                avatar = "⚡",
                syncCode = "STOP-SYNC",
                email = initialEmail,
                isRegistered = true
            )
        )
    )
    val uiState: StateFlow<StopUiState> = _uiState.asStateFlow()

    private var profileObserverJob: Job? = null
    private var leaderboardObserverJob: Job? = null
    private var publicRoomsObserverJob: Job? = null
    private var dailyCompletionsObserverJob: Job? = null

    private var roomObserverJob: Job? = null
    private var roomPlayersObserverJob: Job? = null
    private var roomMessagesObserverJob: Job? = null
    private var answerSyncJob: Job? = null

    private var practiceTimerJob: Job? = null
    private var practiceBotJob: Job? = null
    private var roomTimerJob: Job? = null
    private var roomBotJob: Job? = null
    private var toastJob: Job? = null

    private var latestLeaderboardRecords: List<FirestoreMappers.RawLeaderboardRecord> = emptyList()
    private var lastObservedRoomRound: Int = 0
    private var lastObservedRoomPhase: RoomPhase? = null
    private var finalizedRoomRound: Int = 0
    private var lastCommittedMyRoundScore: Int = -1
    private var announcedPracticeAllFilled: Boolean = false
    private var announcedRoomAllFilled: Boolean = false

    private data class BotSimRuntime(
        val id: String,
        val name: String,
        val avatar: String,
        val difficulty: Difficulty,
        var filledCount: Int = 0,
        var statusText: String = "Pensando...",
        val answers: MutableMap<String, String> = mutableMapOf(),
        var fieldStartTimeMs: Long = 0L,
        var fieldDurationMs: Long = 4000L,
        var hasStopped: Boolean = false
    )

    private val practiceBotsRuntime = linkedMapOf<String, BotSimRuntime>()
    private val roomBotsRuntime = linkedMapOf<String, BotSimRuntime>()

    init {
        viewModelScope.launch {
            val initResult = repository.ensureUserProfile(currentUserId, initialDisplayName, initialEmail)
            initResult.onSuccess { profile ->
                _uiState.update { state ->
                    state.copy(
                        profile = profile,
                        currentUser = state.currentUser?.copy(
                            name = profile.name,
                            avatar = profile.avatar
                        )
                    )
                }
            }.onFailure { err ->
                showToast(extractRootFirestoreMessage(err, "Failed to sync cloud profile."))
            }

            repository.ensureTodayDailyChallenge().onSuccess { cloudDaily ->
                _uiState.update { it.copy(dailyChallenge = cloudDaily) }
            }

            startCloudObservers()
        }
    }

    private fun startCloudObservers() {
        profileObserverJob?.cancel()
        profileObserverJob = viewModelScope.launch {
            repository.observeUserProfile(currentUserId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Profile stream error", fsErr ?: error)
                }
                .collect { profile ->
                    if (profile != null) {
                        _uiState.update { state ->
                            val ranked = repository.rankLeaderboardEntries(
                                latestLeaderboardRecords,
                                profile,
                                state.leaderboardPeriod
                            )
                            state.copy(
                                profile = profile,
                                currentUser = state.currentUser?.copy(
                                    name = profile.name,
                                    avatar = profile.avatar
                                ),
                                leaderboardEntries = ranked
                            )
                        }
                    }
                }
        }

        leaderboardObserverJob?.cancel()
        leaderboardObserverJob = viewModelScope.launch {
            repository.observeLeaderboards()
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val root = fsErr ?: error
                    Log.w(VM_TAG, "Leaderboard stream error", root)
                    _uiState.update { it.copy(isLeaderboardLoading = false) }
                    showToast(extractRootFirestoreMessage(root, "Could not load rankings."))
                }
                .collect { records ->
                    latestLeaderboardRecords = records
                    _uiState.update { state ->
                        val ranked = repository.rankLeaderboardEntries(
                            records,
                            state.profile,
                            state.leaderboardPeriod
                        )
                        state.copy(
                            leaderboardEntries = ranked,
                            isLeaderboardLoading = false
                        )
                    }
                }
        }

        publicRoomsObserverJob?.cancel()
        publicRoomsObserverJob = viewModelScope.launch {
            repository.observePublicRooms()
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val root = fsErr ?: error
                    Log.w(VM_TAG, "Public rooms stream error", root)
                }
                .collect { rooms ->
                    _uiState.update { it.copy(publicRooms = rooms) }
                }
        }

        dailyCompletionsObserverJob?.cancel()
        dailyCompletionsObserverJob = viewModelScope.launch {
            repository.observeDailyCompletions(currentUserId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Daily completions stream error", fsErr ?: error)
                }
                .collect { completions ->
                    _uiState.update { it.copy(dailyCompletions = completions) }
                }
        }
    }

    fun clearListenersBeforeSignOut() {
        stopPracticeLoops()
        stopRoomLoops()
        stopRoomObservers()
        profileObserverJob?.cancel()
        leaderboardObserverJob?.cancel()
        publicRoomsObserverJob?.cancel()
        dailyCompletionsObserverJob?.cancel()
        profileObserverJob = null
        leaderboardObserverJob = null
        publicRoomsObserverJob = null
        dailyCompletionsObserverJob = null
    }

    override fun onCleared() {
        super.onCleared()
        clearListenersBeforeSignOut()
        soundEngine.shutdown()
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(3500)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    fun toggleMute() {
        val muted = soundEngine.toggleMute()
        _uiState.update { it.copy(isMuted = muted) }
        if (!muted) soundEngine.playLetterReveal()
    }

    fun setDifficulty(difficulty: Difficulty) {
        soundEngine.playKeyType()
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun updateBotSpeed(newSpeed: Int) {
        val clamped = newSpeed.coerceIn(1, 6)
        repository.saveBotSpeedSec(clamped)
        _uiState.update { it.copy(botSpeedSec = clamped) }
        val state = _uiState.value
        val now = System.currentTimeMillis()
        practiceBotsRuntime.values.forEach { sim ->
            if (!sim.hasStopped && sim.filledCount < state.practiceCategories.size) {
                val diff = if (sim.id == "bot_vicky") Difficulty.MEDIUM else state.selectedDifficulty
                sim.fieldDurationMs = GameRules.calculateBotFieldDelayMs(clamped, diff, false)
                sim.fieldStartTimeMs = now
            }
        }
    }

    fun cycleBotSpeed() {
        val current = _uiState.value.botSpeedSec
        val next = if (current >= 6) 1 else current + 1
        soundEngine.playKeyType()
        updateBotSpeed(next)
    }

    // Modal controls
    fun setShowProfile(show: Boolean) {
        _uiState.update { it.copy(showProfileModal = show) }
    }

    fun setShowAuth(show: Boolean) {
        _uiState.update { it.copy(showAuthModal = show) }
    }

    fun setShowLeaderboards(show: Boolean) {
        _uiState.update { it.copy(showLeaderboardsModal = show) }
    }

    fun setShowDaily(show: Boolean) {
        _uiState.update { it.copy(showDailyModal = show) }
        if (show) {
            viewModelScope.launch {
                repository.ensureTodayDailyChallenge().onSuccess { cloudDaily ->
                    _uiState.update { it.copy(dailyChallenge = cloudDaily) }
                }
            }
        }
    }

    fun updateProfileDetails(name: String, avatar: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val current = _uiState.value.profile
            val res = repository.saveUserProfile(
                current.copy(
                    name = name.trim().ifEmpty { current.name },
                    avatar = avatar
                )
            )
            res.onSuccess { updated ->
                _uiState.update { it.copy(profile = updated) }
                onDone()
            }.onFailure { err ->
                showToast(extractRootFirestoreMessage(err, "Could not update cloud profile."))
            }
        }
    }

    fun selectLeaderboardPeriod(period: LeaderboardPeriod) {
        soundEngine.playKeyType()
        _uiState.update { state ->
            val ranked = repository.rankLeaderboardEntries(latestLeaderboardRecords, state.profile, period)
            state.copy(leaderboardPeriod = period, leaderboardEntries = ranked)
        }
    }

    // ==================== PRACTICE & DAILY GAMEPLAY ====================

    fun startPractice(
        difficulty: Difficulty = _uiState.value.selectedDifficulty,
        speedSec: Int = _uiState.value.botSpeedSec
    ) {
        repository.saveBotSpeedSec(speedSec)
        val cats = GameRules.DEFAULT_CATEGORIES.take(6)
        val letter = GameRules.VALID_LETTERS.random()
        announcedPracticeAllFilled = false
        initPracticeBots(cats, speedSec, difficulty)

        _uiState.update {
            it.copy(
                route = AppRoute.PRACTICE,
                selectedDifficulty = difficulty,
                botSpeedSec = speedSec,
                practiceRound = 1,
                totalPracticeRounds = 3,
                practiceTotalTimeSec = 60,
                practiceLetter = letter,
                practiceCategories = cats,
                practiceAnswers = emptyMap(),
                practiceTimeRemaining = 60,
                isSpinningPractice = true,
                showStopCelebration = false,
                isPracticeSummary = false,
                practiceScores = emptyList(),
                practiceStopperId = null,
                isDailyChallengeRun = false,
                lastDailyBonusAwarded = 0,
                isPaused = false
            )
        }
    }

    fun startDailyChallenge(challenge: DailyChallenge) {
        val cats = challenge.categories
        announcedPracticeAllFilled = false
        initPracticeBots(cats, _uiState.value.botSpeedSec, Difficulty.MEDIUM)
        _uiState.update {
            it.copy(
                showDailyModal = false,
                route = AppRoute.DAILY_PLAY,
                practiceRound = 1,
                totalPracticeRounds = 1,
                practiceTotalTimeSec = challenge.timeLimitSec,
                practiceLetter = challenge.letter,
                practiceCategories = cats,
                practiceAnswers = emptyMap(),
                practiceTimeRemaining = challenge.timeLimitSec,
                isSpinningPractice = true,
                showStopCelebration = false,
                isPracticeSummary = false,
                practiceScores = emptyList(),
                practiceStopperId = null,
                isDailyChallengeRun = true,
                lastDailyBonusAwarded = 0,
                isPaused = false
            )
        }
    }

    fun nextPracticeRound() {
        val state = _uiState.value
        val nextR = state.practiceRound + 1
        val cats = GameRules.DEFAULT_CATEGORIES.take(6)
        val letter = GameRules.VALID_LETTERS.random()
        announcedPracticeAllFilled = false
        initPracticeBots(cats, state.botSpeedSec, state.selectedDifficulty)

        _uiState.update {
            it.copy(
                practiceRound = nextR,
                practiceLetter = letter,
                practiceCategories = cats,
                practiceAnswers = emptyMap(),
                practiceTimeRemaining = 60,
                isSpinningPractice = true,
                showStopCelebration = false,
                isPracticeSummary = false,
                practiceScores = emptyList(),
                practiceStopperId = null,
                lastDailyBonusAwarded = 0,
                isPaused = false
            )
        }
    }

    fun onPracticeSpinComplete() {
        val now = System.currentTimeMillis()
        practiceBotsRuntime.values.forEach { sim -> sim.fieldStartTimeMs = now }
        _uiState.update { it.copy(isSpinningPractice = false) }
        startPracticeLoops()
    }

    fun updatePracticeAnswer(categoryId: String, value: String) {
        soundEngine.playKeyType()
        val state = _uiState.value
        val updatedAnswers = state.practiceAnswers + (categoryId to value)
        _uiState.update { it.copy(practiceAnswers = updatedAnswers) }

        val filled = updatedAnswers.values.count { it.trim().isNotEmpty() }
        if (!announcedPracticeAllFilled && filled >= state.practiceCategories.size && state.practiceCategories.isNotEmpty()) {
            announcedPracticeAllFilled = true
            soundEngine.playScoreChime(10)
            soundEngine.speakVoice("¡Completaste todo! ¡Presiona STOP!", rate = 1.30f, pitch = 1.24f, flush = true)
        }
    }

    fun pausePractice() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumePractice() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun quitPracticeToTitle() {
        stopPracticeLoops()
        _uiState.update {
            it.copy(
                isPaused = false,
                isSpinningPractice = false,
                showStopCelebration = false,
                isPracticeSummary = false,
                lastDailyBonusAwarded = 0,
                route = AppRoute.TITLE
            )
        }
    }

    fun dismissStopCelebration() {
        _uiState.update { state ->
            if (state.route == AppRoute.ONLINE_ROOM) {
                state.copy(showStopCelebration = false)
            } else {
                state.copy(
                    showStopCelebration = false,
                    isPracticeSummary = true
                )
            }
        }
    }

    fun finishPracticeRound(callerId: String) {
        stopPracticeLoops()
        val state = _uiState.value
        if (state.isPracticeSummary || state.showStopCelebration) return

        val vickyAnswers = practiceBotsRuntime["bot_vicky"]?.answers?.toMap().orEmpty()
        val samAnswers = practiceBotsRuntime["bot_sam"]?.answers?.toMap().orEmpty()
        val isPlayerStopper = callerId == state.profile.id || callerId == "player"
        val elapsedSec = max(1, state.practiceTotalTimeSec - state.practiceTimeRemaining)
        val filledCount = state.practiceAnswers.values.count { it.trim().isNotEmpty() }

        val submissions = buildList {
            add(
                PlayerRoundSubmission(
                    playerId = state.profile.id,
                    answers = state.practiceAnswers,
                    hasStopped = isPlayerStopper,
                    stopElapsedSec = if (isPlayerStopper) elapsedSec else null
                )
            )
            if (!state.isDailyChallengeRun) {
                add(
                    PlayerRoundSubmission(
                        playerId = "bot_vicky",
                        answers = vickyAnswers,
                        hasStopped = callerId == "bot_vicky",
                        stopElapsedSec = if (callerId == "bot_vicky") elapsedSec else null
                    )
                )
                add(
                    PlayerRoundSubmission(
                        playerId = "bot_sam",
                        answers = samAnswers,
                        hasStopped = callerId == "bot_sam",
                        stopElapsedSec = if (callerId == "bot_sam") elapsedSec else null
                    )
                )
            }
        }

        val scores = GameRules.calculateRoundScores(submissions, state.practiceCategories, state.practiceLetter)
        val myScore = scores.firstOrNull { it.playerId == state.profile.id }
        val roundPts = myScore?.totalRoundScore ?: 0
        val isWin = scores.all { it.playerId == state.profile.id || it.totalRoundScore <= roundPts }

        if (!isPlayerStopper) {
            val announcerMsg = if (callerId == "timer") {
                "¡Tiempo agotado! ¡Veamos el puntaje!"
            } else {
                val botName = practiceBotsRuntime[callerId]?.name ?: "Un rival"
                "¡$botName presionó STOP!"
            }
            soundEngine.speakVoice(announcerMsg, rate = 1.24f, pitch = 1.16f, flush = true)
        }

        _uiState.update {
            it.copy(
                practiceStopperId = callerId,
                showStopCelebration = isPlayerStopper,
                celebrationFilledCount = filledCount,
                celebrationTotalCount = state.practiceCategories.size,
                isPracticeSummary = !isPlayerStopper,
                practiceScores = scores
            )
        }

        viewModelScope.launch {
            val current = _uiState.value.profile
            val matchFinished = state.practiceRound >= state.totalPracticeRounds
            val updatedStats = current.stats.copy(
                gamesPlayed = current.stats.gamesPlayed + if (matchFinished) 1 else 0,
                wins = current.stats.wins + if (matchFinished && isWin) 1 else 0,
                stopsCalled = current.stats.stopsCalled + if (isPlayerStopper) 1 else 0,
                totalPoints = current.stats.totalPoints + roundPts,
                weeklyPoints = current.stats.weeklyPoints + roundPts,
                bestRoundScore = max(current.stats.bestRoundScore, roundPts),
                currentStreak = if (isWin) current.stats.currentStreak + 1 else current.stats.currentStreak
            )
            val updated = current.copy(stats = updatedStats)
            if (state.isDailyChallengeRun) {
                repository.recordDailyChallengeCompletion(updated, state.dailyChallenge, roundPts)
                    .onSuccess { (saved, bonusAwarded) ->
                        _uiState.update {
                            it.copy(
                                profile = saved,
                                lastDailyBonusAwarded = bonusAwarded
                            )
                        }
                    }
                    .onFailure { err ->
                        showToast(extractRootFirestoreMessage(err, "Could not sync daily challenge."))
                    }
            } else {
                repository.saveUserProfile(updated)
                    .onSuccess { saved ->
                        _uiState.update { it.copy(profile = saved) }
                    }
                    .onFailure { err ->
                        showToast(extractRootFirestoreMessage(err, "Could not sync round score."))
                    }
            }
        }
    }

    private fun initPracticeBots(categories: List<Category>, speedSec: Int, difficulty: Difficulty) {
        val now = System.currentTimeMillis()
        val firstName = categories.firstOrNull()?.name ?: "Categoría"
        val vicky = BotSimRuntime(
            id = "bot_vicky",
            name = "Vocabulary Vicky",
            avatar = "🦉",
            difficulty = Difficulty.MEDIUM,
            statusText = "Pensando $firstName... (1/${categories.size})",
            fieldStartTimeMs = now,
            fieldDurationMs = GameRules.calculateBotFieldDelayMs(speedSec, Difficulty.MEDIUM, true)
        )
        val sam = BotSimRuntime(
            id = "bot_sam",
            name = "Speedy Sam",
            avatar = "⚡",
            difficulty = difficulty,
            statusText = "Pensando $firstName... (1/${categories.size})",
            fieldStartTimeMs = now,
            fieldDurationMs = GameRules.calculateBotFieldDelayMs(speedSec, difficulty, true)
        )
        practiceBotsRuntime.clear()
        practiceBotsRuntime["bot_vicky"] = vicky
        practiceBotsRuntime["bot_sam"] = sam
        publishPracticeBotProgress()
    }

    private fun publishPracticeBotProgress() {
        _uiState.update { state ->
            state.copy(
                botProgress = practiceBotsRuntime.mapValues { (_, sim) ->
                    BotProgressState(
                        id = sim.id,
                        name = sim.name,
                        avatar = sim.avatar,
                        difficulty = sim.difficulty,
                        filledCount = sim.filledCount,
                        statusText = sim.statusText,
                        answers = sim.answers.toMap(),
                        hasStopped = sim.hasStopped
                    )
                }
            )
        }
    }

    private fun startPracticeLoops() {
        stopPracticeLoops()
        practiceTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                if (state.isPaused || state.isSpinningPractice || state.isPracticeSummary) continue
                if (state.route != AppRoute.PRACTICE && state.route != AppRoute.DAILY_PLAY) break

                val next = state.practiceTimeRemaining - 1
                if (next <= 0) {
                    _uiState.update { it.copy(practiceTimeRemaining = 0) }
                    soundEngine.playStopAlarm()
                    finishPracticeRound("timer")
                    break
                } else {
                    if (next == 10) {
                        soundEngine.speakVoice("¡Diez segundos! ¡Último empujón!", rate = 1.28f, pitch = 1.22f, flush = false)
                    }
                    if (next <= 10) soundEngine.playTimerAlert()
                    _uiState.update { it.copy(practiceTimeRemaining = next) }
                }
            }
        }

        if (!_uiState.value.isDailyChallengeRun) {
            practiceBotJob = viewModelScope.launch {
                while (true) {
                    delay(200L)
                    val state = _uiState.value
                    if (state.isPaused || state.isSpinningPractice || state.isPracticeSummary) continue
                    if (state.route != AppRoute.PRACTICE) break

                    val now = System.currentTimeMillis()
                    var changed = false
                    var stoppedByBot: String? = null

                    for (sim in practiceBotsRuntime.values) {
                        if (sim.hasStopped) continue
                        val totalCats = state.practiceCategories.size
                        if (sim.filledCount < totalCats) {
                            if (now - sim.fieldStartTimeMs >= sim.fieldDurationMs) {
                                val cat = state.practiceCategories[sim.filledCount]
                                val diff = if (sim.id == "bot_vicky") Difficulty.MEDIUM else state.selectedDifficulty
                                sim.answers[cat.id] = GameRules.generateBotAnswer(cat.id, state.practiceLetter, diff)
                                sim.filledCount += 1

                                if (sim.filledCount < totalCats) {
                                    val nextCat = state.practiceCategories[sim.filledCount]
                                    sim.fieldStartTimeMs = now
                                    sim.fieldDurationMs = GameRules.calculateBotFieldDelayMs(state.botSpeedSec, diff, false)
                                    sim.statusText = "Pensando ${nextCat.name}... (${sim.filledCount + 1}/$totalCats)"
                                } else {
                                    sim.fieldStartTimeMs = now
                                    sim.fieldDurationMs = 1600L + Random.nextLong(800L)
                                    sim.statusText = "¡Completó todos los campos! Preparando STOP..."
                                }
                                changed = true
                            }
                        } else if (now - sim.fieldStartTimeMs >= sim.fieldDurationMs) {
                            sim.hasStopped = true
                            changed = true
                            stoppedByBot = sim.id
                            break
                        }
                    }

                    if (changed) publishPracticeBotProgress()
                    if (stoppedByBot != null) {
                        soundEngine.playStopAlarm()
                        finishPracticeRound(stoppedByBot)
                        break
                    }
                }
            }
        }
    }

    private fun stopPracticeLoops() {
        practiceTimerJob?.cancel()
        practiceBotJob?.cancel()
        practiceTimerJob = null
        practiceBotJob = null
    }

    fun getPracticeActivePlayersStrip(): List<ActivePlayerStripItem> {
        val state = _uiState.value
        val total = state.practiceCategories.size
        val humanFilled = state.practiceAnswers.values.count { it.trim().isNotEmpty() }
        val humanItem = ActivePlayerStripItem(
            id = state.profile.id,
            name = state.profile.name,
            avatar = state.profile.avatar,
            hasStopped = state.practiceStopperId == state.profile.id,
            filledCount = humanFilled,
            totalCategories = total,
            statusText = "$humanFilled de $total completados",
            isHuman = true
        )
        if (state.isDailyChallengeRun) return listOf(humanItem)

        val botItems = practiceBotsRuntime.values.map { bot ->
            val progress = state.botProgress[bot.id]
            ActivePlayerStripItem(
                id = bot.id,
                name = bot.name,
                avatar = bot.avatar,
                hasStopped = state.practiceStopperId == bot.id || (progress?.hasStopped == true),
                filledCount = progress?.filledCount ?: 0,
                totalCategories = total,
                statusText = progress?.statusText ?: "Pensando...",
                isHuman = false
            )
        }
        return listOf(humanItem) + botItems
    }

    // ==================== CLOUD MULTIPLAYER LOBBY & ROOMS ====================

    fun openMultiplayerLobby() {
        _uiState.update { it.copy(route = AppRoute.ONLINE_LOBBY) }
    }

    fun refreshPublicRooms() {
        // Real-time Firestore listener automatically refreshes; trigger UI feedback
        showToast("Live rooms synced with cloud.")
    }

    fun backFromLobbyToTitle() {
        _uiState.update { it.copy(route = AppRoute.TITLE) }
    }

    fun createRoom(name: String, settings: RoomSettings, addPracticeBot: Boolean) {
        viewModelScope.launch {
            val res = repository.createRoom(
                profile = _uiState.value.profile,
                roomName = name,
                settings = settings,
                addPracticeBot = addPracticeBot
            )
            res.onSuccess { room ->
                enterObservedRoom(room)
            }.onFailure { err ->
                showToast(extractRootFirestoreMessage(err, "Could not create room."))
            }
        }
    }

    fun joinRoom(code: String) {
        viewModelScope.launch {
            val res = repository.joinRoom(code, _uiState.value.profile)
            res.onSuccess { room ->
                enterObservedRoom(room)
            }.onFailure { err ->
                showToast(extractRootFirestoreMessage(err, "Could not join room. Check the code."))
            }
        }
    }

    private fun enterObservedRoom(room: GameRoom) {
        stopRoomLoops()
        stopRoomObservers()
        lastObservedRoomRound = room.currentRound
        lastObservedRoomPhase = room.phase
        finalizedRoomRound = 0
        lastCommittedMyRoundScore = -1

        _uiState.update {
            it.copy(
                activeRoom = room,
                route = AppRoute.ONLINE_ROOM,
                roomLocalAnswers = emptyMap(),
                roomTimeRemaining = room.settings.roundDurationSec,
                isSpinningRoom = room.phase == RoomPhase.ROUND_ACTIVE,
                roomCalculatedScores = emptyList()
            )
        }
        startRoomObservers(room.code, room.settings.isPrivate)
    }

    private fun startRoomObservers(roomCode: String, isPrivate: Boolean) {
        roomObserverJob = viewModelScope.launch {
            repository.observeRoom(roomCode)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Room stream error", fsErr ?: error)
                }
                .collect { remoteRoom ->
                    if (remoteRoom == null) {
                        // Room was closed/deleted by host
                        stopRoomLoops()
                        stopRoomObservers()
                        _uiState.update {
                            it.copy(
                                activeRoom = null,
                                isSpinningRoom = false,
                                route = AppRoute.ONLINE_LOBBY
                            )
                        }
                        showToast("The host closed the room.")
                        return@collect
                    }

                    val currentState = _uiState.value
                    val preservedPlayers = currentState.activeRoom?.players.orEmpty()
                    val preservedChat = currentState.activeRoom?.chat.orEmpty()
                    val mergedRoom = remoteRoom.copy(
                        players = preservedPlayers,
                        chat = preservedChat
                    )

                    val phaseChanged = lastObservedRoomPhase != mergedRoom.phase
                    val roundChanged = lastObservedRoomRound != mergedRoom.currentRound

                    if (mergedRoom.phase == RoomPhase.ROUND_ACTIVE && (phaseChanged || roundChanged)) {
                        lastObservedRoomRound = mergedRoom.currentRound
                        lastObservedRoomPhase = mergedRoom.phase
                        finalizedRoomRound = 0
                        lastCommittedMyRoundScore = -1
                        announcedRoomAllFilled = false

                        if (mergedRoom.hostId == currentUserId) {
                            initRoomBots(mergedRoom)
                        } else {
                            roomBotsRuntime.clear()
                        }

                        _uiState.update {
                            it.copy(
                                activeRoom = mergedRoom,
                                roomLocalAnswers = emptyMap(),
                                roomTimeRemaining = mergedRoom.settings.roundDurationSec,
                                isSpinningRoom = true,
                                showStopCelebration = false,
                                roomCalculatedScores = emptyList()
                            )
                        }
                    } else if (mergedRoom.phase == RoomPhase.STOP_CALLED && phaseChanged) {
                        lastObservedRoomPhase = mergedRoom.phase
                        stopRoomLoops()
                        if (mergedRoom.stopperPlayerId != currentUserId) {
                            soundEngine.playStopAlarm()
                            soundEngine.speakVoice(
                                "¡${mergedRoom.stopperName ?: "Alguien"} gritó STOP!",
                                rate = 1.26f,
                                pitch = 1.18f,
                                flush = true
                            )
                        }

                        _uiState.update {
                            it.copy(
                                activeRoom = mergedRoom,
                                isSpinningRoom = false
                            )
                        }
                        flushMyAnswersAndFinalizeRound(mergedRoom)
                    } else {
                        lastObservedRoomRound = mergedRoom.currentRound
                        lastObservedRoomPhase = mergedRoom.phase
                        _uiState.update { it.copy(activeRoom = mergedRoom) }
                    }
                }
        }

        roomPlayersObserverJob = viewModelScope.launch {
            repository.observeRoomPlayers(roomCode, isPrivate)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Room players stream error", fsErr ?: error)
                }
                .collect { players ->
                    val state = _uiState.value
                    val currentRoom = state.activeRoom ?: return@collect
                    val updatedRoom = currentRoom.copy(players = players)

                    if (updatedRoom.phase == RoomPhase.STOP_CALLED || updatedRoom.phase == RoomPhase.ROUND_SCORES) {
                        val recalculated = calculateRoomRoundScores(updatedRoom, state.roomLocalAnswers)
                        val myScore = recalculated.firstOrNull { it.playerId == currentUserId }?.totalRoundScore ?: 0
                        _uiState.update {
                            it.copy(
                                activeRoom = updatedRoom,
                                roomCalculatedScores = recalculated
                            )
                        }
                        if (finalizedRoomRound == updatedRoom.currentRound && myScore != lastCommittedMyRoundScore) {
                            val delta = myScore - max(0, lastCommittedMyRoundScore)
                            lastCommittedMyRoundScore = myScore
                            val myPrevTotal = updatedRoom.players.firstOrNull { it.id == currentUserId }?.totalScore ?: 0
                            viewModelScope.launch {
                                repository.syncMyRoundScore(
                                    roomCode = updatedRoom.code,
                                    roundScore = myScore,
                                    totalScore = max(0, myPrevTotal + delta)
                                )
                            }
                        }
                    } else {
                        _uiState.update { it.copy(activeRoom = updatedRoom) }
                    }
                }
        }

        roomMessagesObserverJob = viewModelScope.launch {
            repository.observeRoomMessages(roomCode, isPrivate)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Room chat stream error", fsErr ?: error)
                }
                .collect { messages ->
                    _uiState.update { state ->
                        val room = state.activeRoom ?: return@update state
                        state.copy(activeRoom = room.copy(chat = messages))
                    }
                }
        }
    }

    private fun stopRoomObservers() {
        roomObserverJob?.cancel()
        roomPlayersObserverJob?.cancel()
        roomMessagesObserverJob?.cancel()
        answerSyncJob?.cancel()
        roomObserverJob = null
        roomPlayersObserverJob = null
        roomMessagesObserverJob = null
        answerSyncJob = null
    }

    fun leaveRoom() {
        val room = _uiState.value.activeRoom
        stopRoomLoops()
        stopRoomObservers()
        _uiState.update {
            it.copy(
                activeRoom = null,
                isSpinningRoom = false,
                showStopCelebration = false,
                roomCalculatedScores = emptyList(),
                route = AppRoute.TITLE
            )
        }
        if (room != null) {
            viewModelScope.launch {
                repository.leaveRoom(room)
            }
        }
    }

    fun startOnlineRound() {
        val room = _uiState.value.activeRoom ?: return
        if (room.hostId != currentUserId) {
            showToast("Only the room host can start the round.")
            return
        }
        val letter = GameRules.VALID_LETTERS.random()
        val nextRound = room.currentRound + 1
        viewModelScope.launch {
            val res = repository.startRound(room, nextRound, letter)
            res.onFailure { err ->
                showToast(extractRootFirestoreMessage(err, "Could not start round."))
            }
        }
    }

    fun onRoomSpinComplete() {
        val now = System.currentTimeMillis()
        roomBotsRuntime.values.forEach { it.fieldStartTimeMs = now }
        _uiState.update { it.copy(isSpinningRoom = false) }
        startRoomLoops()
    }

    fun updateRoomAnswer(categoryId: String, value: String) {
        soundEngine.playKeyType()
        val state = _uiState.value
        val updatedAnswers = state.roomLocalAnswers + (categoryId to value)
        _uiState.update { it.copy(roomLocalAnswers = updatedAnswers) }

        val room = state.activeRoom ?: return
        if (room.phase != RoomPhase.ROUND_ACTIVE) return

        val totalCats = room.settings.categories.size
        val filled = updatedAnswers.values.count { it.trim().isNotEmpty() }
        if (!announcedRoomAllFilled && filled >= totalCats && totalCats > 0) {
            announcedRoomAllFilled = true
            soundEngine.playScoreChime(10)
            soundEngine.speakVoice("¡Completaste todo! ¡Presiona STOP!", rate = 1.30f, pitch = 1.24f, flush = true)
        }

        answerSyncJob?.cancel()
        answerSyncJob = viewModelScope.launch {
            delay(350L)
            repository.syncMyPlayerAnswers(
                roomCode = room.code,
                answers = updatedAnswers,
                filledCount = filled,
                statusText = "$filled de $totalCats completados",
                answeredRound = room.currentRound
            )
        }
    }

    fun sendRoomChat(text: String) {
        val room = _uiState.value.activeRoom ?: return
        val profile = _uiState.value.profile
        val trimmed = text.trim().take(200)
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val res = repository.sendRoomMessage(
                room = room,
                senderName = profile.name,
                senderAvatar = profile.avatar,
                text = trimmed
            )
            res.onFailure { err ->
                showToast(extractRootFirestoreMessage(err, "Failed to send message."))
            }
        }
    }

    fun callOnlineStop(callerId: String = currentUserId) {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        if (room.phase != RoomPhase.ROUND_ACTIVE) return
        stopRoomLoops()

        val elapsedSec = max(1, room.settings.roundDurationSec - state.roomTimeRemaining)
        val totalCats = room.settings.categories.size
        val filled = state.roomLocalAnswers.values.count { it.trim().isNotEmpty() }
        val isLocalPlayerCaller = callerId == currentUserId
        if (isLocalPlayerCaller) {
            _uiState.update {
                it.copy(
                    showStopCelebration = true,
                    celebrationFilledCount = filled,
                    celebrationTotalCount = totalCats
                )
            }
        }

        val stopper = room.players.firstOrNull { it.id == callerId }
        val stopperName = when {
            callerId == "timer" -> "Time Limit"
            stopper != null -> stopper.name
            else -> state.profile.name
        }

        viewModelScope.launch {
            repository.syncMyPlayerAnswers(
                roomCode = room.code,
                answers = state.roomLocalAnswers,
                filledCount = filled,
                statusText = if (isLocalPlayerCaller) "STOP!" else "$filled de $totalCats completados",
                answeredRound = room.currentRound,
                hasStopped = isLocalPlayerCaller,
                stopElapsedSec = if (isLocalPlayerCaller) elapsedSec else null
            )

            // Sync bot answers if host triggered STOP
            if (room.hostId == currentUserId) {
                roomBotsRuntime.values.forEach { sim ->
                    repository.updateBotPlayerState(
                        roomCode = room.code,
                        botId = sim.id,
                        answers = sim.answers.toMap(),
                        filledCount = sim.filledCount,
                        statusText = sim.statusText,
                        answeredRound = room.currentRound,
                        hasStopped = callerId == sim.id,
                        stopElapsedSec = if (callerId == sim.id) elapsedSec else null
                    )
                }
            }

            val res = repository.callRoomStop(room, callerId, stopperName)
            res.onFailure { err ->
                showToast(extractRootFirestoreMessage(err, "Could not call STOP."))
            }
        }
    }

    private fun flushMyAnswersAndFinalizeRound(room: GameRoom) {
        val state = _uiState.value
        val totalCats = room.settings.categories.size
        val filled = state.roomLocalAnswers.values.count { it.trim().isNotEmpty() }
        val elapsedSec = max(1, room.settings.roundDurationSec - state.roomTimeRemaining)
        val isPlayerStopper = room.stopperPlayerId == currentUserId

        val initialScores = calculateRoomRoundScores(room, state.roomLocalAnswers)
        _uiState.update { it.copy(roomCalculatedScores = initialScores) }

        if (finalizedRoomRound == room.currentRound) return
        finalizedRoomRound = room.currentRound

        val myRoundScore = initialScores.firstOrNull { it.playerId == currentUserId }?.totalRoundScore ?: 0
        lastCommittedMyRoundScore = myRoundScore
        val myExistingTotal = room.players.firstOrNull { it.id == currentUserId }?.totalScore ?: 0
        val newRoomTotal = myExistingTotal + myRoundScore

        viewModelScope.launch {
            repository.syncMyPlayerAnswers(
                roomCode = room.code,
                answers = state.roomLocalAnswers,
                filledCount = filled,
                statusText = if (isPlayerStopper) "STOP!" else "$filled de $totalCats completados",
                answeredRound = room.currentRound,
                hasStopped = isPlayerStopper,
                stopElapsedSec = if (isPlayerStopper) elapsedSec else null
            )
            repository.syncMyRoundScore(
                roomCode = room.code,
                roundScore = myRoundScore,
                totalScore = newRoomTotal
            )

            if (room.hostId == currentUserId) {
                room.players.filter { it.isBot }.forEach { bot ->
                    val botScore = initialScores.firstOrNull { it.playerId == bot.id }?.totalRoundScore ?: 0
                    val botAnswers = roomBotsRuntime[bot.id]?.answers?.toMap() ?: bot.answers
                    repository.updateBotPlayerState(
                        roomCode = room.code,
                        botId = bot.id,
                        answers = botAnswers,
                        filledCount = botAnswers.size,
                        statusText = if (room.stopperPlayerId == bot.id) "STOP!" else "Finalized",
                        answeredRound = room.currentRound,
                        hasStopped = room.stopperPlayerId == bot.id,
                        stopElapsedSec = if (room.stopperPlayerId == bot.id) elapsedSec else null,
                        roundScore = botScore,
                        totalScore = bot.totalScore + botScore
                    )
                }
            }

            // Persist user stats & cross-device leaderboard entry
            val isMatchFinished = room.currentRound >= room.settings.roundCount
            val isWin = initialScores.all { it.playerId == currentUserId || it.totalRoundScore <= myRoundScore }
            val prof = _uiState.value.profile
            val res = repository.saveUserProfile(
                prof.copy(
                    stats = prof.stats.copy(
                        gamesPlayed = prof.stats.gamesPlayed + if (isMatchFinished) 1 else 0,
                        wins = prof.stats.wins + if (isMatchFinished && isWin) 1 else 0,
                        stopsCalled = prof.stats.stopsCalled + if (isPlayerStopper) 1 else 0,
                        totalPoints = prof.stats.totalPoints + myRoundScore,
                        weeklyPoints = prof.stats.weeklyPoints + myRoundScore,
                        bestRoundScore = max(prof.stats.bestRoundScore, myRoundScore),
                        currentStreak = if (isWin) prof.stats.currentStreak + 1 else prof.stats.currentStreak
                    )
                )
            )
            res.onSuccess { updatedProf ->
                _uiState.update { it.copy(profile = updatedProf) }
            }
        }
    }

    private fun calculateRoomRoundScores(
        room: GameRoom,
        myLocalAnswers: Map<String, String>
    ): List<PlayerRoundScore> {
        val submissions = room.players.map { p ->
            val ans = when {
                p.id == currentUserId && myLocalAnswers.isNotEmpty() -> myLocalAnswers
                p.isBot && roomBotsRuntime[p.id] != null -> roomBotsRuntime[p.id]!!.answers.toMap()
                else -> p.answers
            }
            PlayerRoundSubmission(
                playerId = p.id,
                answers = ans,
                hasStopped = room.stopperPlayerId == p.id || p.hasStopped,
                stopElapsedSec = p.stopElapsedSec
            )
        }
        return GameRules.calculateRoundScores(submissions, room.settings.categories, room.currentLetter)
    }

    private fun initRoomBots(room: GameRoom) {
        roomBotsRuntime.clear()
        val now = System.currentTimeMillis()
        val cats = room.settings.categories
        val firstCat = cats.firstOrNull()?.name ?: "Categoría"
        room.players.filter { it.isBot }.forEach { bot ->
            roomBotsRuntime[bot.id] = BotSimRuntime(
                id = bot.id,
                name = bot.name,
                avatar = bot.avatar,
                difficulty = room.settings.difficulty,
                statusText = "Pensando $firstCat... (1/${cats.size})",
                fieldStartTimeMs = now,
                fieldDurationMs = GameRules.calculateBotFieldDelayMs(_uiState.value.botSpeedSec, room.settings.difficulty, true)
            )
        }
    }

    private fun startRoomLoops() {
        stopRoomLoops()
        roomTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                val room = state.activeRoom ?: break
                if (state.route != AppRoute.ONLINE_ROOM || room.phase != RoomPhase.ROUND_ACTIVE || state.isSpinningRoom) {
                    break
                }
                val next = state.roomTimeRemaining - 1
                if (next <= 0) {
                    _uiState.update { it.copy(roomTimeRemaining = 0) }
                    if (room.hostId == currentUserId) {
                        callOnlineStop("timer")
                    }
                    break
                } else {
                    if (next == 10) {
                        soundEngine.speakVoice("¡Diez segundos! ¡Último empujón!", rate = 1.28f, pitch = 1.22f, flush = false)
                    }
                    if (next <= 10) soundEngine.playTimerAlert()
                    _uiState.update { it.copy(roomTimeRemaining = next) }
                }
            }
        }

        // Only the room host simulates & syncs any bots in the room
        val initialRoom = _uiState.value.activeRoom
        if (initialRoom != null && initialRoom.hostId == currentUserId && roomBotsRuntime.isNotEmpty()) {
            roomBotJob = viewModelScope.launch {
                while (true) {
                    delay(450L)
                    val state = _uiState.value
                    val room = state.activeRoom ?: break
                    if (state.route != AppRoute.ONLINE_ROOM || room.phase != RoomPhase.ROUND_ACTIVE || state.isSpinningRoom) {
                        break
                    }
                    val now = System.currentTimeMillis()
                    val cats = room.settings.categories
                    var stoppedByBot: String? = null

                    for (sim in roomBotsRuntime.values) {
                        if (sim.hasStopped) continue
                        if (sim.filledCount < cats.size) {
                            if (now - sim.fieldStartTimeMs >= sim.fieldDurationMs) {
                                val cat = cats[sim.filledCount]
                                sim.answers[cat.id] = GameRules.generateBotAnswer(cat.id, room.currentLetter, sim.difficulty)
                                sim.filledCount += 1
                                if (sim.filledCount < cats.size) {
                                    val nextCat = cats[sim.filledCount]
                                    sim.fieldStartTimeMs = now
                                    sim.fieldDurationMs = GameRules.calculateBotFieldDelayMs(state.botSpeedSec, sim.difficulty, false)
                                    sim.statusText = "Pensando ${nextCat.name}... (${sim.filledCount + 1}/${cats.size})"
                                } else {
                                    sim.fieldStartTimeMs = now
                                    sim.fieldDurationMs = 1700L + Random.nextLong(900L)
                                    sim.statusText = "¡Completó todos los campos! Preparando STOP..."
                                }
                                repository.updateBotPlayerState(
                                    roomCode = room.code,
                                    botId = sim.id,
                                    answers = sim.answers.toMap(),
                                    filledCount = sim.filledCount,
                                    statusText = sim.statusText,
                                    answeredRound = room.currentRound
                                )
                            }
                        } else if (now - sim.fieldStartTimeMs >= sim.fieldDurationMs) {
                            sim.hasStopped = true
                            stoppedByBot = sim.id
                            break
                        }
                    }

                    if (stoppedByBot != null) {
                        soundEngine.playStopAlarm()
                        callOnlineStop(stoppedByBot)
                        break
                    }
                }
            }
        }
    }

    private fun stopRoomLoops() {
        roomTimerJob?.cancel()
        roomBotJob?.cancel()
        roomTimerJob = null
        roomBotJob = null
    }
}
