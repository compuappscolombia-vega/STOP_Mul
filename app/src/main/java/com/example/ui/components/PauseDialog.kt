package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.StopColors

@Composable
fun PauseDialog(
    isMuted: Boolean,
    botSpeedSec: Int?,
    onBotSpeedChange: ((Int) -> Unit)?,
    onToggleMute: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(StopColors.Slate900)
                .border(1.dp, StopColors.Slate800, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GAME PAUSED",
                style = MaterialTheme.typography.headlineMedium,
                color = StopColors.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Timer is frozen · Take a breather",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = StopColors.Slate400
            )

            if (botSpeedSec != null && onBotSpeedChange != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StopColors.Slate950)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Bot speed",
                                tint = StopColors.Amber400,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Velocidad del Bot:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StopColors.Slate300
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StopColors.Amber950)
                                .border(1.dp, StopColors.Amber500.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$botSpeedSec seg / campo",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = StopColors.Amber400
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..6).forEach { sec ->
                            val selected = botSpeedSec == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) StopColors.Amber500 else StopColors.Slate900)
                                    .clickable { onBotSpeedChange(sec) }
                                    .padding(vertical = 8.dp)
                                    .testTag("pause_bot_speed_${sec}s"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selected) StopColors.Slate950 else StopColors.Slate400
                                )
                            }
                        }
                    }

                    Text(
                        text = if (botSpeedSec >= 4) "👤 Ritmo Humano Relajado" else "⚡ Ritmo Competitivo Rápido",
                        fontSize = 11.sp,
                        color = StopColors.Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = StopColors.Rose600),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_resume_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Resume game",
                    tint = StopColors.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = "Resume Game", fontWeight = FontWeight.Bold, color = StopColors.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onToggleMute,
                colors = ButtonDefaults.buttonColors(containerColor = StopColors.Slate800),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Toggle audio",
                    tint = if (isMuted) StopColors.Rose400 else StopColors.Emerald400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = if (isMuted) "Unmute Sound" else "Mute Sound",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StopColors.Slate200
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onQuit,
                colors = ButtonDefaults.buttonColors(containerColor = StopColors.Slate950),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                    .testTag("pause_quit_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Quit to title screen",
                    tint = StopColors.Slate400,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Quit to Title Screen",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StopColors.Slate300
                )
            }
        }
    }
}
