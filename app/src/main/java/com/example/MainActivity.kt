package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.audio.SoundEngine
import com.example.data.FirebaseRepository
import com.example.data.authStateFlow
import com.example.data.signOutFromFirebase
import com.example.ui.components.AuthDialog
import com.example.ui.components.DailyChallengeDialog
import com.example.ui.components.LeaderboardDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.MultiplayerLobbyScreen
import com.example.ui.screens.RoomViewScreen
import com.example.ui.screens.TitleScreen
import com.example.ui.theme.StopColors
import com.example.ui.theme.StopRushTheme
import com.example.viewmodel.AppRoute
import com.example.viewmodel.StopViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private const val ROUTE_TITLE = "title"
private const val ROUTE_PRACTICE = "practice"
private const val ROUTE_DAILY = "daily"
private const val ROUTE_ONLINE_LOBBY = "online_lobby"
private const val ROUTE_ONLINE_ROOM = "online_room"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StopRushTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    Scaffold(
        containerColor = StopColors.Slate950,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val user = currentUser
            if (user == null) {
                AuthScreen(onAuthSuccess = {})
            } else {
                val uid = user.uid
                key(uid) {
                    StopRushAuthenticatedApp(
                        currentUserId = uid,
                        displayName = user.displayName,
                        email = user.email
                    )
                }
            }
        }
    }
}

@Composable
private fun StopRushAuthenticatedApp(
    currentUserId: String,
    displayName: String?,
    email: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StopViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                StopViewModel(
                    repository = FirebaseRepository(app),
                    soundEngine = SoundEngine(app),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(state.route) {
        val targetRoute = when (state.route) {
            AppRoute.TITLE -> ROUTE_TITLE
            AppRoute.PRACTICE -> ROUTE_PRACTICE
            AppRoute.DAILY_PLAY -> ROUTE_DAILY
            AppRoute.ONLINE_LOBBY -> ROUTE_ONLINE_LOBBY
            AppRoute.ONLINE_ROOM -> ROUTE_ONLINE_ROOM
        }
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(ROUTE_TITLE) { inclusive = (targetRoute == ROUTE_TITLE) }
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_TITLE,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(ROUTE_TITLE) {
                TitleScreen(
                    profile = state.profile,
                    currentUser = state.currentUser,
                    selectedDifficulty = state.selectedDifficulty,
                    botSpeedSec = state.botSpeedSec,
                    isMuted = state.isMuted,
                    onSelectDifficulty = viewModel::setDifficulty,
                    onSelectBotSpeed = viewModel::updateBotSpeed,
                    onStartOffline = {
                        viewModel.soundEngine.playLetterReveal()
                        viewModel.startPractice()
                    },
                    onOpenMultiplayer = {
                        viewModel.soundEngine.playLetterTick()
                        viewModel.openMultiplayerLobby()
                    },
                    onOpenDaily = {
                        viewModel.soundEngine.playKeyType()
                        viewModel.setShowDaily(true)
                    },
                    onOpenLeaderboards = {
                        viewModel.soundEngine.playKeyType()
                        viewModel.setShowLeaderboards(true)
                    },
                    onOpenProfile = {
                        viewModel.soundEngine.playKeyType()
                        viewModel.setShowProfile(true)
                    },
                    onOpenAuth = {
                        viewModel.soundEngine.playKeyType()
                        viewModel.setShowAuth(true)
                    },
                    onToggleMute = viewModel::toggleMute
                )
            }

            composable(ROUTE_PRACTICE) {
                GamePlayScreen(
                    profile = state.profile,
                    currentRound = state.practiceRound,
                    totalRounds = state.totalPracticeRounds,
                    currentLetter = state.practiceLetter,
                    categories = state.practiceCategories,
                    answers = state.practiceAnswers,
                    timeRemainingSec = state.practiceTimeRemaining,
                    totalTimeSec = state.practiceTotalTimeSec,
                    isSpinning = state.isSpinningPractice,
                    showStopCelebration = state.showStopCelebration,
                    celebrationFilledCount = state.celebrationFilledCount,
                    celebrationTotalCount = state.celebrationTotalCount,
                    isSummary = state.isPracticeSummary,
                    isPaused = state.isPaused,
                    isMuted = state.isMuted,
                    isDailyRun = false,
                    dailyBonusAwarded = 0,
                    botSpeedSec = state.botSpeedSec,
                    activePlayersStrip = viewModel.getPracticeActivePlayersStrip(),
                    playerScores = state.practiceScores,
                    soundEngine = viewModel.soundEngine,
                    onSpinComplete = viewModel::onPracticeSpinComplete,
                    onAnswerChange = viewModel::updatePracticeAnswer,
                    onCallStop = { viewModel.finishPracticeRound(state.profile.id) },
                    onCelebrationComplete = viewModel::dismissStopCelebration,
                    onToggleMute = viewModel::toggleMute,
                    onPause = viewModel::pausePractice,
                    onResume = viewModel::resumePractice,
                    onBotSpeedChange = viewModel::updateBotSpeed,
                    onCycleBotSpeed = viewModel::cycleBotSpeed,
                    onNextRound = viewModel::nextPracticeRound,
                    onQuitToTitle = viewModel::quitPracticeToTitle
                )
            }

            composable(ROUTE_DAILY) {
                GamePlayScreen(
                    profile = state.profile,
                    currentRound = state.practiceRound,
                    totalRounds = state.totalPracticeRounds,
                    currentLetter = state.practiceLetter,
                    categories = state.practiceCategories,
                    answers = state.practiceAnswers,
                    timeRemainingSec = state.practiceTimeRemaining,
                    totalTimeSec = state.practiceTotalTimeSec,
                    isSpinning = state.isSpinningPractice,
                    showStopCelebration = state.showStopCelebration,
                    celebrationFilledCount = state.celebrationFilledCount,
                    celebrationTotalCount = state.celebrationTotalCount,
                    isSummary = state.isPracticeSummary,
                    isPaused = state.isPaused,
                    isMuted = state.isMuted,
                    isDailyRun = true,
                    dailyBonusAwarded = state.lastDailyBonusAwarded,
                    botSpeedSec = state.botSpeedSec,
                    activePlayersStrip = viewModel.getPracticeActivePlayersStrip(),
                    playerScores = state.practiceScores,
                    soundEngine = viewModel.soundEngine,
                    onSpinComplete = viewModel::onPracticeSpinComplete,
                    onAnswerChange = viewModel::updatePracticeAnswer,
                    onCallStop = { viewModel.finishPracticeRound(state.profile.id) },
                    onCelebrationComplete = viewModel::dismissStopCelebration,
                    onToggleMute = viewModel::toggleMute,
                    onPause = viewModel::pausePractice,
                    onResume = viewModel::resumePractice,
                    onBotSpeedChange = viewModel::updateBotSpeed,
                    onCycleBotSpeed = viewModel::cycleBotSpeed,
                    onNextRound = viewModel::nextPracticeRound,
                    onQuitToTitle = viewModel::quitPracticeToTitle
                )
            }

            composable(ROUTE_ONLINE_LOBBY) {
                MultiplayerLobbyScreen(
                    profile = state.profile,
                    publicRooms = state.publicRooms,
                    onRefreshRooms = {
                        viewModel.soundEngine.playKeyType()
                        viewModel.refreshPublicRooms()
                    },
                    onCreateRoom = { name, settings, addBot ->
                        viewModel.soundEngine.playLetterReveal()
                        viewModel.createRoom(name, settings, addBot)
                    },
                    onJoinRoom = { code ->
                        viewModel.soundEngine.playKeyType()
                        viewModel.joinRoom(code)
                    },
                    onBack = {
                        viewModel.soundEngine.playKeyType()
                        viewModel.backFromLobbyToTitle()
                    }
                )
            }

            composable(ROUTE_ONLINE_ROOM) {
                val room = state.activeRoom
                if (room != null) {
                    RoomViewScreen(
                        room = room,
                        myProfile = state.profile,
                        localAnswers = state.roomLocalAnswers,
                        timeRemainingSec = state.roomTimeRemaining,
                        isSpinning = state.isSpinningRoom,
                        showStopCelebration = state.showStopCelebration,
                        celebrationFilledCount = state.celebrationFilledCount,
                        celebrationTotalCount = state.celebrationTotalCount,
                        calculatedScores = state.roomCalculatedScores,
                        isMuted = state.isMuted,
                        soundEngine = viewModel.soundEngine,
                        onLeaveRoom = viewModel::leaveRoom,
                        onStartRound = {
                            viewModel.soundEngine.playLetterReveal()
                            viewModel.startOnlineRound()
                        },
                        onSpinComplete = viewModel::onRoomSpinComplete,
                        onAnswerChange = viewModel::updateRoomAnswer,
                        onCallStop = { viewModel.callOnlineStop(state.profile.id) },
                        onCelebrationComplete = viewModel::dismissStopCelebration,
                        onSendMessage = { msg ->
                            viewModel.soundEngine.playKeyType()
                            viewModel.sendRoomChat(msg)
                        },
                        onToggleMute = viewModel::toggleMute
                    )
                }
            }
        }

        // Toast Notification Banner
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(StopColors.Rose600)
                    .border(1.dp, StopColors.Rose400, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⚠️", fontSize = 14.sp)
                Text(
                    text = state.toastMessage.orEmpty(),
                    color = StopColors.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Modals
        if (state.showProfileModal) {
            ProfileDialog(
                profile = state.profile,
                onSaveProfile = viewModel::updateProfileDetails,
                onClose = { viewModel.setShowProfile(false) }
            )
        }

        if (state.showAuthModal) {
            AuthDialog(
                currentUser = state.currentUser,
                currentProfile = state.profile,
                onLogout = {
                    viewModel.setShowAuth(false)
                    signOutFromFirebase(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = { viewModel.clearListenersBeforeSignOut() }
                    )
                },
                onClose = { viewModel.setShowAuth(false) }
            )
        }

        if (state.showLeaderboardsModal) {
            LeaderboardDialog(
                profile = state.profile,
                period = state.leaderboardPeriod,
                entries = state.leaderboardEntries,
                isLoading = state.isLeaderboardLoading,
                onSelectPeriod = viewModel::selectLeaderboardPeriod,
                onClose = { viewModel.setShowLeaderboards(false) }
            )
        }

        if (state.showDailyModal) {
            DailyChallengeDialog(
                profile = state.profile,
                challenge = state.dailyChallenge,
                dailyCompletions = state.dailyCompletions,
                onStartDaily = { ch ->
                    viewModel.soundEngine.playLetterReveal()
                    viewModel.startDailyChallenge(ch)
                },
                onClose = { viewModel.setShowDaily(false) }
            )
        }
    }
}
