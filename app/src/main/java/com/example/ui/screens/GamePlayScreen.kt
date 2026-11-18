package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEngine
import com.example.model.ActivePlayerStripItem
import com.example.model.Category
import com.example.model.PlayerRoundScore
import com.example.model.UserProfile
import com.example.ui.components.FooterCredit
import com.example.ui.components.GameBoard
import com.example.ui.components.GameHUD
import com.example.ui.components.LetterWheelOverlay
import com.example.ui.components.PauseDialog
import com.example.ui.components.RoundSummaryDialog
import com.example.ui.components.StopAtmosphereBackground
import com.example.ui.components.StopCelebrationOverlay
import com.example.ui.components.SummaryPlayerInfo

@Composable
fun GamePlayScreen(
    profile: UserProfile,
    currentRound: Int,
    totalRounds: Int,
    currentLetter: String,
    categories: List<Category>,
    answers: Map<String, String>,
    timeRemainingSec: Int,
    totalTimeSec: Int,
    isSpinning: Boolean,
    showStopCelebration: Boolean = false,
    celebrationFilledCount: Int = 0,
    celebrationTotalCount: Int = 6,
    isSummary: Boolean,
    isPaused: Boolean,
    isMuted: Boolean,
    isDailyRun: Boolean,
    dailyBonusAwarded: Int = 0,
    botSpeedSec: Int,
    activePlayersStrip: List<ActivePlayerStripItem>,
    playerScores: List<PlayerRoundScore>,
    soundEngine: SoundEngine,
    onSpinComplete: () -> Unit,
    onAnswerChange: (String, String) -> Unit,
    onCallStop: () -> Unit,
    onCelebrationComplete: () -> Unit = {},
    onToggleMute: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onBotSpeedChange: (Int) -> Unit,
    onCycleBotSpeed: () -> Unit,
    onNextRound: () -> Unit,
    onQuitToTitle: () -> Unit
) {
    BackHandler(enabled = !isSpinning && !showStopCelebration) {
        when {
            isSummary -> onQuitToTitle()
            isPaused -> onResume()
            else -> onPause()
        }
    }

    StopAtmosphereBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameHUD(
                    currentLetter = if (isSpinning) "?" else currentLetter,
                    currentRound = currentRound,
                    totalRounds = totalRounds,
                    timeRemainingSec = timeRemainingSec,
                    totalTimeSec = totalTimeSec,
                    currentScore = profile.stats.totalPoints,
                    isMuted = isMuted,
                    onToggleMute = onToggleMute,
                    onPause = onPause,
                    isPractice = !isDailyRun,
                    botSpeedSec = if (isDailyRun) null else botSpeedSec,
                    onCycleBotSpeed = if (isDailyRun) null else onCycleBotSpeed
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 880.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GameBoard(
                        categories = categories,
                        targetLetter = if (isSpinning) "?" else currentLetter,
                        answers = answers,
                        onAnswerChange = onAnswerChange,
                        onCallStop = onCallStop,
                        isStopDisabled = isSummary || isSpinning || isPaused || showStopCelebration,
                        activePlayers = activePlayersStrip,
                        botSpeedSec = if (isDailyRun) null else botSpeedSec,
                        onBotSpeedChange = if (isDailyRun) null else onBotSpeedChange
                    )

                    FooterCredit()
                }
            }

            if (isSpinning) {
                LetterWheelOverlay(
                    targetLetter = currentLetter,
                    soundEngine = soundEngine,
                    onSpinComplete = onSpinComplete
                )
            }

            if (showStopCelebration) {
                StopCelebrationOverlay(
                    playerName = profile.name,
                    filledCount = celebrationFilledCount,
                    totalCategories = celebrationTotalCount,
                    isDailyChallenge = isDailyRun,
                    soundEngine = soundEngine,
                    onCelebrationComplete = onCelebrationComplete
                )
            }

            if (isSummary && !showStopCelebration) {
                val summaryPlayers = if (isDailyRun) {
                    listOf(SummaryPlayerInfo(profile.id, profile.name, profile.avatar))
                } else {
                    listOf(
                        SummaryPlayerInfo(profile.id, profile.name, profile.avatar),
                        SummaryPlayerInfo("bot_vicky", "Vocabulary Vicky", "🦉"),
                        SummaryPlayerInfo("bot_sam", "Speedy Sam", "⚡")
                    )
                }
                RoundSummaryDialog(
                    currentRound = currentRound,
                    totalRounds = totalRounds,
                    letter = currentLetter,
                    categories = categories,
                    playerScores = playerScores,
                    players = summaryPlayers,
                    myPlayerId = profile.id,
                    isMatchFinished = currentRound >= totalRounds,
                    dailyBonusAwarded = dailyBonusAwarded,
                    soundEngine = soundEngine,
                    onNextRound = onNextRound,
                    onReturnToLobby = onQuitToTitle
                )
            }

            if (isPaused) {
                PauseDialog(
                    isMuted = isMuted,
                    botSpeedSec = if (isDailyRun) null else botSpeedSec,
                    onBotSpeedChange = if (isDailyRun) null else onBotSpeedChange,
                    onToggleMute = onToggleMute,
                    onResume = onResume,
                    onQuit = onQuitToTitle
                )
            }
        }
    }
}
