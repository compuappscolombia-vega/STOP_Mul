package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.GameRules
import com.example.model.UserProfile
import com.example.ui.theme.StopColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDialog(
    profile: UserProfile,
    onSaveProfile: (name: String, avatar: String, onDone: () -> Unit) -> Unit,
    onClose: () -> Unit
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var avatar by remember(profile.id) { mutableStateOf(profile.avatar) }
    var copied by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

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
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(StopColors.Slate900)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                                .background(StopColors.Rose500.copy(alpha = 0.2f))
                                .border(1.dp, StopColors.Rose500.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile icon",
                                tint = StopColors.Rose400
                            )
                        }
                        Column {
                            Text(
                                text = "Player Profile & Sync",
                                style = MaterialTheme.typography.titleLarge,
                                color = StopColors.White
                            )
                            Text(
                                text = "Synced automatically across devices via Google Sign-In",
                                fontSize = 11.sp,
                                color = StopColors.Slate400
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close profile",
                            tint = StopColors.Slate400
                        )
                    }
                }

                // Nickname + Avatar Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StopColors.Slate950)
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = avatar, fontSize = 28.sp)
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 20) name = it },
                        label = { Text("Player Nickname", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = StopColors.Slate950,
                            unfocusedContainerColor = StopColors.Slate950,
                            focusedBorderColor = StopColors.Rose500,
                            unfocusedBorderColor = StopColors.Slate800,
                            focusedTextColor = StopColors.White,
                            unfocusedTextColor = StopColors.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_nickname_input")
                    )
                }

                // Avatar Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Choose Avatar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StopColors.Slate400
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StopColors.Slate950)
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GameRules.AVATAR_OPTIONS.forEach { av ->
                            val selected = avatar == av
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) StopColors.Rose500.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) StopColors.Rose500 else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { avatar = av },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = av, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            onSaveProfile(name, avatar) {
                                saveSuccess = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StopColors.Rose600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).testTag("profile_save_button")
                    ) {
                        Text(
                            text = if (saveSuccess) "Saved to Cloud!" else "Save Details",
                            fontWeight = FontWeight.Bold,
                            color = StopColors.White
                        )
                    }
                }

                // Stats Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StopColors.Slate950)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCell("Total Pts", "${profile.stats.totalPoints}", StopColors.Amber400)
                    StatCell("Victories", "${profile.stats.wins}", StopColors.White)
                    StatCell("STOPs", "${profile.stats.stopsCalled}", StopColors.Rose400)
                    StatCell("Streak", "${profile.stats.currentStreak}d", StopColors.Emerald400)
                }

                // Cloud Sync Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StopColors.Slate950.copy(alpha = 0.85f))
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Profile Sync ID",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StopColors.White
                            )
                            Text(
                                text = "Sign in with Google on any device to keep your stats & rank",
                                fontSize = 11.sp,
                                color = StopColors.Slate400
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StopColors.Slate900)
                                .border(1.dp, StopColors.Slate800, RoundedCornerShape(10.dp))
                                .clickable {
                                    clipboard.setText(AnnotatedString(profile.syncCode))
                                    copied = true
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.syncCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = StopColors.Slate200
                            )
                            Icon(
                                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = "Copy sync code",
                                tint = if (copied) StopColors.Emerald400 else StopColors.Slate400,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = StopColors.Slate400)
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = valueColor,
            textAlign = TextAlign.Center
        )
    }
}
