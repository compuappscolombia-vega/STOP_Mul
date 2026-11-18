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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LeaderboardEntry
import com.example.model.LeaderboardPeriod
import com.example.model.UserProfile
import com.example.ui.theme.StopColors

@Composable
fun LeaderboardDialog(
    profile: UserProfile,
    period: LeaderboardPeriod,
    entries: List<LeaderboardEntry>,
    isLoading: Boolean,
    onSelectPeriod: (LeaderboardPeriod) -> Unit,
    onClose: () -> Unit
) {
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
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(StopColors.Slate900)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(24.dp))
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
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Leaderboards",
                                tint = StopColors.Amber400
                            )
                        }
                        Column {
                            Text(
                                text = "STOP! Rankings & Leaderboards",
                                style = MaterialTheme.typography.titleLarge,
                                color = StopColors.White
                            )
                            Text(
                                text = "Global Hall of Fame and Top Weekly Sprinters",
                                fontSize = 11.sp,
                                color = StopColors.Slate400
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("leaderboard_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close leaderboards",
                            tint = StopColors.Slate400
                        )
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StopColors.Slate950)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PeriodTab(
                        label = "All-Time Legends",
                        selected = period == LeaderboardPeriod.ALL,
                        onClick = { onSelectPeriod(LeaderboardPeriod.ALL) },
                        modifier = Modifier.weight(1f).testTag("leaderboard_tab_all")
                    )
                    PeriodTab(
                        label = "Weekly Champions",
                        selected = period == LeaderboardPeriod.WEEKLY,
                        onClick = { onSelectPeriod(LeaderboardPeriod.WEEKLY) },
                        modifier = Modifier.weight(1f).testTag("leaderboard_tab_weekly")
                    )
                }

                // Entries
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StopColors.Rose500)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(entries, key = { _, item -> item.id }) { idx, item ->
                            val isMe = item.id == profile.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isMe) StopColors.Rose500.copy(alpha = 0.12f) else StopColors.Slate950.copy(alpha = 0.75f))
                                    .border(
                                        1.dp,
                                        if (isMe) StopColors.Rose500.copy(alpha = 0.55f) else StopColors.Slate800,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val (badgeBg, badgeFg) = when (idx) {
                                        0 -> StopColors.Amber400 to StopColors.Slate950
                                        1 -> StopColors.Slate300 to StopColors.Slate950
                                        2 -> StopColors.Amber700 to StopColors.White
                                        else -> StopColors.Slate800 to StopColors.Slate400
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${item.rank}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = badgeFg
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StopColors.Slate900)
                                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = item.avatar, fontSize = 17.sp)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StopColors.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isMe) {
                                                Text(
                                                    text = "(You)",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = StopColors.Rose400
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${item.title} · ${item.wins} Wins · ${item.stopsCalled} Stops",
                                            fontSize = 10.sp,
                                            color = StopColors.Slate400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${item.totalPoints} pts",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = StopColors.Amber400
                                    )
                                    Text(
                                        text = item.tier.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = StopColors.Slate500
                                    )
                                }
                            }
                        }
                    }
                }

                // Current Total Summary Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StopColors.Slate950)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Your Current Total:", fontSize = 11.sp, color = StopColors.Slate300)
                        Text(
                            text = "${profile.stats.totalPoints} pts",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = StopColors.Amber400
                        )
                    }
                    Text(
                        text = "${profile.stats.wins} Wins · ${profile.stats.stopsCalled} Stops",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = StopColors.Slate400
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) StopColors.Rose600 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) StopColors.White else StopColors.Slate400
        )
    }
}
