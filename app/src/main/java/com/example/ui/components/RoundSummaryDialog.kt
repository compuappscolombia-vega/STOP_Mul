package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.SoundEngine
import com.example.model.Category
import com.example.model.PlayerRoundScore
import com.example.ui.theme.StopColors

data class SummaryPlayerInfo(
    val id: String,
    val name: String,
    val avatar: String
)

@Composable
fun RoundSummaryDialog(
    currentRound: Int,
    totalRounds: Int,
    letter: String,
    categories: List<Category>,
    playerScores: List<PlayerRoundScore>,
    players: List<SummaryPlayerInfo>,
    myPlayerId: String,
    isMatchFinished: Boolean,
    dailyBonusAwarded: Int = 0,
    soundEngine: SoundEngine,
    onNextRound: () -> Unit,
    onReturnToLobby: () -> Unit
) {
    val myScore = playerScores.firstOrNull { it.playerId == myPlayerId }
    val winner = playerScores.maxByOrNull { it.totalRoundScore }
    val isMeWinner = winner?.playerId == myPlayerId
    val showConfetti = isMatchFinished || dailyBonusAwarded > 0 || (myScore != null && myScore.totalRoundScore >= 40)

    LaunchedEffect(currentRound, isMatchFinished, dailyBonusAwarded) {
        soundEngine.playScoreChime(myScore?.totalRoundScore ?: 20)
        if (showConfetti) {
            soundEngine.playVictoryFanfare()
            if (isMatchFinished && isMeWinner) {
                soundEngine.playCrowdApplauseAndCheer(2400)
                soundEngine.speakVoice(
                    text = "¡Victoria total! ¡Felicidades campeón!",
                    rate = 1.24f,
                    pitch = 1.18f,
                    flush = false
                )
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StopColors.Slate950.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showConfetti) {
                ConfettiBurstOverlay()
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(StopColors.Slate900)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = if (isMatchFinished) "MATCH CONCLUDED" else "ROUND $currentRound OF $totalRounds RESULTS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = StopColors.Rose400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isMatchFinished && isMeWinner -> "🏆 VICTORY! YOU DOMINATED!"
                        isMatchFinished -> "MATCH SUMMARY"
                        else -> "ROUND SCORECARD"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = StopColors.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Letter '${letter.uppercase()}' · 10 pts Unique · 5 pts Duplicate · +5 First STOP Bonus",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = StopColors.Slate400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Daily Challenge Cloud Bonus Banner
                if (dailyBonusAwarded > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StopColors.Emerald500.copy(alpha = 0.16f))
                            .border(1.dp, StopColors.Emerald400.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Daily Challenge Bonus",
                                tint = StopColors.Emerald300,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Daily Challenge Cleared!",
                                color = StopColors.Emerald300,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "+$dailyBonusAwarded Bonus XP",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StopColors.Emerald300
                        )
                    }
                }

                // First STOP Shoutout
                if (myScore?.stoppedFirst == true) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StopColors.Amber500.copy(alpha = 0.12f))
                            .border(1.dp, StopColors.Amber500.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "First STOP bonus",
                                tint = StopColors.Amber400,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "You called STOP first!",
                                color = StopColors.Amber300,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "+5 Time Bonus",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StopColors.Amber400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Breakdown
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val catScore = myScore?.categoryScores?.get(cat.id)
                        val answer = catScore?.answer.orEmpty()
                        val isValid = catScore?.isValid == true
                        val isDup = catScore?.isDuplicate == true
                        val pts = catScore?.score ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StopColors.Slate950.copy(alpha = 0.7f))
                                .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = cat.icon, fontSize = 18.sp)
                                Column {
                                    Text(
                                        text = cat.name,
                                        fontSize = 11.sp,
                                        color = StopColors.Slate400
                                    )
                                    Text(
                                        text = answer.ifEmpty { "No entry" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (answer.isNotEmpty()) StopColors.Slate100 else StopColors.Slate600,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val (badgeText, badgeFg, badgeBg) = when {
                                    isValid && isDup -> Triple("Shared (+5)", StopColors.Amber300, StopColors.Amber950)
                                    isValid -> Triple("Unique (+10)", StopColors.Emerald300, StopColors.Emerald950)
                                    else -> Triple("0 pts", StopColors.Rose300, StopColors.Rose950)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .border(1.dp, badgeFg.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        color = badgeFg
                                    )
                                }
                                Text(
                                    text = "+$pts",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = StopColors.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Match Standings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StopColors.Slate950.copy(alpha = 0.85f))
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MATCH STANDINGS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = StopColors.Slate400
                    )
                    players.forEach { p ->
                        val sc = playerScores.firstOrNull { it.playerId == p.id }
                        val isLead = p.id == winner?.playerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isLead) StopColors.Rose500.copy(alpha = 0.14f) else StopColors.Slate900)
                                .border(
                                    1.dp,
                                    if (isLead) StopColors.Rose500.copy(alpha = 0.5f) else StopColors.Slate800,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = p.avatar, fontSize = 16.sp)
                                Text(
                                    text = p.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StopColors.Slate200
                                )
                            }
                            Text(
                                text = "${sc?.totalRoundScore ?: 0} pts",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = StopColors.Amber400
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modal Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onReturnToLobby,
                        colors = ButtonDefaults.buttonColors(containerColor = StopColors.Slate800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("summary_title_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Title menu",
                            tint = StopColors.Slate200,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "Title Menu", fontSize = 12.sp, color = StopColors.Slate200)
                    }

                    if (!isMatchFinished) {
                        Button(
                            onClick = onNextRound,
                            colors = ButtonDefaults.buttonColors(containerColor = StopColors.Rose600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("summary_next_round_button")
                        ) {
                            Text(text = "Next Round", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StopColors.White)
                            Spacer(modifier = Modifier.size(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next round",
                                tint = StopColors.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onReturnToLobby,
                            colors = ButtonDefaults.buttonColors(containerColor = StopColors.Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("summary_back_lobby_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Back to lobby",
                                tint = StopColors.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(text = "Back to Lobby", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StopColors.White)
                        }
                    }
                }
            }
        }
    }
}
