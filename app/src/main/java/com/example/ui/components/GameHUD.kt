package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StopColors

@Composable
fun GameHUD(
    currentLetter: String,
    currentRound: Int,
    totalRounds: Int,
    timeRemainingSec: Int,
    totalTimeSec: Int,
    currentScore: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onPause: () -> Unit,
    isPractice: Boolean = false,
    botSpeedSec: Int? = null,
    onCycleBotSpeed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUrgent = timeRemainingSec <= 10
    val progressPct = (timeRemainingSec.toFloat() / totalTimeSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val timerColor by animateColorAsState(
        targetValue = if (isUrgent) StopColors.Rose500 else StopColors.Slate100,
        label = "timerColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StopColors.Slate950.copy(alpha = 0.94f))
            .border(width = 1.dp, color = StopColors.Slate900)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Neon Letter Badge + Round + optional Bot speed chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeonLetterBadge(
                    letter = currentLetter,
                    size = 44.dp,
                    fontSize = 22.sp
                )
                Column {
                    Text(
                        text = "Round $currentRound of $totalRounds",
                        style = MaterialTheme.typography.labelSmall,
                        color = StopColors.Slate400
                    )
                    Text(
                        text = "Letra '${currentLetter.uppercase()}'",
                        style = MaterialTheme.typography.labelMedium,
                        color = StopColors.Cyan300,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Center: Timer + Progress bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "${timeRemainingSec}s",
                    color = timerColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(StopColors.Slate800)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPct)
                            .clip(CircleShape)
                            .background(
                                if (isUrgent) {
                                    Brush.horizontalGradient(listOf(StopColors.Rose600, StopColors.Rose400))
                                } else {
                                    Brush.horizontalGradient(listOf(StopColors.Amber400, StopColors.Rose500))
                                }
                            )
                    )
                }
            }

            // Right: Score + Mute + Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SCORE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StopColors.Slate400
                    )
                    Text(
                        text = "$currentScore",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StopColors.Amber400
                    )
                }

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isMuted) StopColors.Rose950.copy(alpha = 0.7f) else StopColors.Emerald950.copy(alpha = 0.65f))
                        .border(
                            1.dp,
                            if (isMuted) StopColors.Rose500.copy(alpha = 0.5f) else StopColors.Emerald500.copy(alpha = 0.45f),
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("hud_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute sound" else "Mute sound",
                        tint = if (isMuted) StopColors.Rose400 else StopColors.Emerald400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StopColors.Slate900)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                        .testTag("hud_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Pause game",
                        tint = StopColors.Slate200,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Optional Practice Bot Speed Cycler Strip
        if (isPractice && botSpeedSec != null && onCycleBotSpeed != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StopColors.Slate900)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(10.dp))
                    .clickable(onClick = onCycleBotSpeed)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("hud_bot_speed_chip"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Bot:",
                    style = MaterialTheme.typography.labelSmall,
                    color = StopColors.Slate400
                )
                Text(
                    text = "${botSpeedSec}s/campo",
                    style = MaterialTheme.typography.labelMedium,
                    color = StopColors.Amber400
                )
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Cycle bot speed",
                    tint = StopColors.Slate400,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
