package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameRules
import com.example.model.AuthUser
import com.example.model.Difficulty
import com.example.model.UserProfile
import com.example.ui.components.FooterCredit
import com.example.ui.components.NeonLetterBadge
import com.example.ui.components.StopAtmosphereBackground
import com.example.ui.components.StopTitleLockup
import com.example.ui.theme.StopColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleScreen(
    profile: UserProfile,
    currentUser: AuthUser?,
    selectedDifficulty: Difficulty,
    botSpeedSec: Int,
    isMuted: Boolean,
    onSelectDifficulty: (Difficulty) -> Unit,
    onSelectBotSpeed: (Int) -> Unit,
    onStartOffline: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenDaily: () -> Unit,
    onOpenLeaderboards: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAuth: () -> Unit,
    onToggleMute: () -> Unit
) {
    val tier = GameRules.getPlayerTier(profile.stats.totalPoints)
    val carouselLetters = remember { listOf("S", "T", "O", "P", "!", "A", "M", "R", "C", "L", "B") }
    var letterIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1800L)
            letterIndex = (letterIndex + 1) % carouselLetters.size
        }
    }

    StopAtmosphereBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Perimeter Cluster
            Column(
                modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Quick Pill
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StopColors.Slate900.copy(alpha = 0.85f))
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenProfile)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("title_profile_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StopColors.Rose500.copy(alpha = 0.2f))
                                .border(1.dp, StopColors.Rose500.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = profile.avatar, fontSize = 18.sp)
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StopColors.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(StopColors.Rose950.copy(alpha = 0.7f))
                                        .border(1.dp, StopColors.Rose500.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = profile.syncCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = StopColors.Rose300
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = tier.rankTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tier.color
                                )
                                Text(text = "·", fontSize = 11.sp, color = StopColors.Slate500)
                                Text(
                                    text = "${profile.stats.totalPoints} pts",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = StopColors.Slate300
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    // Mute Button
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StopColors.Slate900.copy(alpha = 0.85f))
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(14.dp))
                            .testTag("title_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute sound" else "Mute sound",
                            tint = if (isMuted) StopColors.Rose400 else StopColors.Emerald400
                        )
                    }
                }

                // Secondary Action Bar (Auth, Daily Challenge, Rankings)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopActionChip(
                        icon = if (currentUser != null) Icons.Filled.Shield else Icons.AutoMirrored.Filled.Login,
                        label = if (currentUser != null) "Session Account" else "Sign In / Register",
                        tint = if (currentUser != null) StopColors.Emerald400 else StopColors.Rose400,
                        borderColor = if (currentUser != null) StopColors.Emerald500.copy(alpha = 0.35f) else StopColors.Rose500.copy(alpha = 0.4f),
                        bgColor = if (currentUser != null) StopColors.Slate900 else StopColors.Rose600.copy(alpha = 0.18f),
                        onClick = onOpenAuth,
                        testTag = "title_auth_button"
                    )
                    TopActionChip(
                        icon = Icons.Filled.CalendarMonth,
                        label = "Daily Challenge",
                        tint = StopColors.Amber400,
                        borderColor = StopColors.Amber500.copy(alpha = 0.35f),
                        bgColor = StopColors.Slate900,
                        onClick = onOpenDaily,
                        testTag = "title_daily_button"
                    )
                    TopActionChip(
                        icon = Icons.Filled.EmojiEvents,
                        label = "Rankings",
                        tint = StopColors.Amber400,
                        borderColor = StopColors.Slate800,
                        bgColor = StopColors.Slate900,
                        onClick = onOpenLeaderboards,
                        testTag = "title_rankings_button"
                    )
                }
            }

            // Center Stage
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeonLetterBadge(
                    letter = carouselLetters[letterIndex],
                    size = 96.dp,
                    fontSize = 48.sp,
                    showOnlineBadge = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                StopTitleLockup()

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Spin the alphabet, brainstorm fast categories under pressure, and slap ")
                        withStyle(SpanStyle(color = StopColors.Rose400, fontWeight = FontWeight.Bold)) {
                            append("STOP!")
                        }
                        append(" before your rivals.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = StopColors.Slate400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Primary CTA
                Button(
                    onClick = onStartOffline,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(StopColors.Rose600, StopColors.Rose500, StopColors.Amber500)
                            )
                        )
                        .testTag("start_practice_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start practice sprint",
                        tint = StopColors.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "START PRACTICE SPRINT",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StopColors.White,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bot Speed Selector (1s - 6s per field)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(StopColors.Slate900.copy(alpha = 0.92f))
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = "Velocidad de Respuesta del Bot:",
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
                                .padding(horizontal = 8.dp, vertical = 3.dp)
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
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) StopColors.Amber500 else StopColors.Slate950)
                                    .border(
                                        1.dp,
                                        if (selected) StopColors.Amber400 else StopColors.Slate800,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectBotSpeed(sec) }
                                    .padding(vertical = 8.dp)
                                    .testTag("title_bot_speed_${sec}s"),
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

                    val speedCaption = when (botSpeedSec) {
                        1 -> "⚡ 1s · Máquina Flash (Extremo)"
                        2 -> "🏎️ 2s · Rápido (Alta Presión)"
                        3 -> "🏃 3s · Ágil (Competitivo)"
                        4 -> "👤 4s · Humano Normal (Equilibrado - Recomendado)"
                        5 -> "☕ 5s · Pensativo (Relajado para pensar)"
                        else -> "🧘 6s · Sin Prisa (Ideal para principiantes)"
                    }
                    Text(
                        text = speedCaption,
                        fontSize = 11.sp,
                        color = StopColors.Slate400,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Difficulty Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StopColors.Slate900.copy(alpha = 0.92f))
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nivel de Bot:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = StopColors.Slate400
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Difficulty.entries.forEach { diff ->
                            val selected = selectedDifficulty == diff
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) StopColors.Rose500.copy(alpha = 0.22f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selected) StopColors.Rose500.copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { onSelectDifficulty(diff) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("title_diff_${diff.label}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff.label.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) StopColors.Rose300 else StopColors.Slate400
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Online Multiplayer Lobby CTA
                Button(
                    onClick = onOpenMultiplayer,
                    colors = ButtonDefaults.buttonColors(containerColor = StopColors.Slate900),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp))
                        .testTag("open_multiplayer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = "Online multiplayer lobby",
                        tint = StopColors.Rose400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Online Multiplayer Lobby & Rooms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StopColors.White
                    )
                }
            }

            // Bottom Footer
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = StopColors.Slate900)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next advances Category · Slap STOP! to Lock · Pause Anytime",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = StopColors.Slate500,
                    textAlign = TextAlign.Center
                )
                FooterCredit()
            }
        }
    }
}

@Composable
private fun TopActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    borderColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = StopColors.Slate200
        )
    }
}
