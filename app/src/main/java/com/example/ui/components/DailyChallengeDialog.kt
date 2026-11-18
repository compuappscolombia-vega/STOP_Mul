package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DailyChallenge
import com.example.model.DailyChallengeCompletion
import com.example.model.UserProfile
import com.example.ui.theme.StopColors

@Composable
fun DailyChallengeDialog(
    profile: UserProfile,
    challenge: DailyChallenge,
    dailyCompletions: List<DailyChallengeCompletion> = emptyList(),
    onStartDaily: (DailyChallenge) -> Unit,
    onClose: () -> Unit
) {
    val todayCompletion = dailyCompletions.firstOrNull { it.date == challenge.date }
    val alreadyCompleted = todayCompletion != null || profile.stats.dailyChallengesCompleted.contains(challenge.date)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(StopColors.Slate900)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StopColors.Amber500.copy(alpha = 0.2f))
                                .border(1.dp, StopColors.Amber500.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Daily Challenge",
                                tint = StopColors.Amber400
                            )
                        }
                        Column {
                            Text(
                                text = "CLOUD DAILY GAUNTLET · ${challenge.date}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = StopColors.Amber400
                            )
                            Text(
                                text = challenge.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = StopColors.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("daily_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close daily challenge",
                            tint = StopColors.Slate400
                        )
                    }
                }

                if (challenge.taskDescription.isNotBlank()) {
                    Text(
                        text = challenge.taskDescription,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = StopColors.Slate200,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StopColors.Amber500.copy(alpha = 0.10f))
                            .border(1.dp, StopColors.Amber500.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                // Details Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StopColors.Slate950.copy(alpha = 0.85f))
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Assigned Letter:", fontSize = 12.sp, color = StopColors.Slate400)
                        Text(
                            text = "'${challenge.letter.uppercase()}'",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = StopColors.Rose400
                        )
                    }
                    HorizontalDivider(color = StopColors.Slate900)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Target Par Score:", fontSize = 12.sp, color = StopColors.Slate400)
                        Text(
                            text = "${challenge.targetParScore} pts",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StopColors.Amber400
                        )
                    }
                    HorizontalDivider(color = StopColors.Slate900)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Time Limit:", fontSize = 12.sp, color = StopColors.Slate400)
                        Text(
                            text = "${challenge.timeLimitSec} seconds",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StopColors.Slate200
                        )
                    }
                }

                // 5 Challenge Categories
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${challenge.categories.size} Challenge Categories:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StopColors.Slate400
                    )
                    challenge.categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StopColors.Slate950)
                                .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = cat.icon, fontSize = 16.sp)
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = StopColors.Slate200
                            )
                        }
                    }
                }

                // Streak & Cloud Completion Counter Bonus
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StopColors.Rose500.copy(alpha = 0.12f))
                        .border(1.dp, StopColors.Rose500.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
                            contentDescription = "Daily streak",
                            tint = StopColors.Rose400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Streak: ${profile.stats.currentStreak}d · Cleared: ${dailyCompletions.size.coerceAtLeast(profile.stats.dailyChallengesCompleted.size)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StopColors.Rose300
                        )
                    }
                    Text(
                        text = "+${challenge.bonusPoints} Bonus XP",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StopColors.Rose400
                    )
                }

                if (alreadyCompleted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StopColors.Emerald500.copy(alpha = 0.18f))
                            .border(1.dp, StopColors.Emerald500.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Completed",
                                tint = StopColors.Emerald300,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Today's Challenge Completed!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StopColors.Emerald300
                            )
                        }
                        if (todayCompletion != null) {
                            Text(
                                text = "Score: ${todayCompletion.roundScore} pts · Bonus Earned: +${todayCompletion.bonusPointsAwarded} XP" +
                                    if (todayCompletion.parAchieved) " · PAR ACHIEVED 🏆" else "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = StopColors.Emerald300
                            )
                        } else {
                            Text(
                                text = "Come back tomorrow for a new daily challenge!",
                                fontSize = 12.sp,
                                color = StopColors.Emerald300
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onStartDaily(challenge) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(StopColors.Amber500, StopColors.Rose600)))
                            .testTag("daily_accept_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start daily challenge",
                            tint = StopColors.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Accept Today's Challenge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = StopColors.White
                        )
                    }
                }
            }
        }
    }
}
