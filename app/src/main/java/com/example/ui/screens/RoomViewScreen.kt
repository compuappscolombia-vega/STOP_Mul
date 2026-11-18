package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEngine
import com.example.model.ActivePlayerStripItem
import com.example.model.GameRoom
import com.example.model.PlayerRoundScore
import com.example.model.RoomPhase
import com.example.model.UserProfile
import com.example.ui.components.FooterCredit
import com.example.ui.components.GameBoard
import com.example.ui.components.GameHUD
import com.example.ui.components.LetterWheelOverlay
import com.example.ui.components.RoundSummaryDialog
import com.example.ui.components.StopAtmosphereBackground
import com.example.ui.components.StopCelebrationOverlay
import com.example.ui.components.SummaryPlayerInfo
import com.example.ui.theme.StopColors

@Composable
fun RoomViewScreen(
    room: GameRoom,
    myProfile: UserProfile,
    localAnswers: Map<String, String>,
    timeRemainingSec: Int,
    isSpinning: Boolean,
    showStopCelebration: Boolean = false,
    celebrationFilledCount: Int = 0,
    celebrationTotalCount: Int = 6,
    calculatedScores: List<PlayerRoundScore>,
    isMuted: Boolean,
    soundEngine: SoundEngine,
    onLeaveRoom: () -> Unit,
    onStartRound: () -> Unit,
    onSpinComplete: () -> Unit,
    onAnswerChange: (String, String) -> Unit,
    onCallStop: () -> Unit,
    onCelebrationComplete: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onToggleMute: () -> Unit
) {
    BackHandler(enabled = !showStopCelebration) {
        onLeaveRoom()
    }

    val isHost = room.hostId == myProfile.id
    val myPlayer = room.players.firstOrNull { it.id == myProfile.id }

    StopAtmosphereBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (room.phase == RoomPhase.ROUND_ACTIVE) {
                    GameHUD(
                        currentLetter = if (isSpinning) "?" else room.currentLetter,
                        currentRound = room.currentRound,
                        totalRounds = room.settings.roundCount,
                        timeRemainingSec = timeRemainingSec,
                        totalTimeSec = room.settings.roundDurationSec,
                        currentScore = myPlayer?.totalScore ?: 0,
                        isMuted = isMuted,
                        onToggleMute = onToggleMute,
                        onPause = onLeaveRoom
                    )
                }

                when (room.phase) {
                    RoomPhase.LOBBY -> {
                        RoomLobbyContent(
                            room = room,
                            myProfile = myProfile,
                            isHost = isHost,
                            onLeaveRoom = onLeaveRoom,
                            onStartRound = onStartRound,
                            onSendMessage = onSendMessage,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    RoomPhase.ROUND_ACTIVE -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 880.dp)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val totalCats = room.settings.categories.size
                            val humanFilled = localAnswers.values.count { it.trim().isNotEmpty() }
                            val strip = room.players.map { p ->
                                val isHuman = p.id == myProfile.id
                                ActivePlayerStripItem(
                                    id = p.id,
                                    name = p.name,
                                    avatar = p.avatar,
                                    hasStopped = p.hasStopped,
                                    filledCount = if (isHuman) humanFilled else p.filledCount,
                                    totalCategories = totalCats,
                                    statusText = if (isHuman) "$humanFilled de $totalCats completados" else p.statusText.ifEmpty { "Pensando..." },
                                    isHuman = isHuman
                                )
                            }
                            GameBoard(
                                categories = room.settings.categories,
                                targetLetter = if (isSpinning) "?" else room.currentLetter,
                                answers = localAnswers,
                                onAnswerChange = onAnswerChange,
                                onCallStop = onCallStop,
                                isStopDisabled = isSpinning || showStopCelebration,
                                activePlayers = strip
                            )
                            FooterCredit()
                        }
                    }

                    else -> {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (isSpinning) {
                LetterWheelOverlay(
                    targetLetter = room.currentLetter,
                    soundEngine = soundEngine,
                    onSpinComplete = onSpinComplete
                )
            }

            if (showStopCelebration) {
                StopCelebrationOverlay(
                    playerName = myProfile.name,
                    filledCount = celebrationFilledCount,
                    totalCategories = celebrationTotalCount,
                    isDailyChallenge = false,
                    soundEngine = soundEngine,
                    onCelebrationComplete = onCelebrationComplete
                )
            }

            if (!showStopCelebration && (room.phase == RoomPhase.STOP_CALLED || room.phase == RoomPhase.ROUND_SCORES)) {
                RoundSummaryDialog(
                    currentRound = room.currentRound,
                    totalRounds = room.settings.roundCount,
                    letter = room.currentLetter,
                    categories = room.settings.categories,
                    playerScores = calculatedScores,
                    players = room.players.map { SummaryPlayerInfo(it.id, it.name, it.avatar) },
                    myPlayerId = myProfile.id,
                    isMatchFinished = room.currentRound >= room.settings.roundCount,
                    soundEngine = soundEngine,
                    onNextRound = if (isHost) onStartRound else onLeaveRoom,
                    onReturnToLobby = onLeaveRoom
                )
            }
        }
    }
}

@Composable
private fun RoomLobbyContent(
    room: GameRoom,
    myProfile: UserProfile,
    isHost: Boolean,
    onLeaveRoom: () -> Unit,
    onStartRound: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 980.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 740.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.6f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RoomHeaderCard(room, isHost, onLeaveRoom, onStartRound)
                        ConnectedPlayersCard(room, myProfile)
                    }
                    MatchChatCard(
                        room = room,
                        myProfileId = myProfile.id,
                        onSendMessage = onSendMessage,
                        modifier = Modifier.weight(1f).height(480.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoomHeaderCard(room, isHost, onLeaveRoom, onStartRound)
                    ConnectedPlayersCard(room, myProfile)
                    MatchChatCard(
                        room = room,
                        myProfileId = myProfile.id,
                        onSendMessage = onSendMessage,
                        modifier = Modifier.fillMaxWidth().height(340.dp)
                    )
                }
            }
        }

        FooterCredit()
    }
}

@Composable
private fun RoomHeaderCard(
    room: GameRoom,
    isHost: Boolean,
    onLeaveRoom: () -> Unit,
    onStartRound: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StopColors.Slate900.copy(alpha = 0.92f))
            .border(1.dp, StopColors.Slate800, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StopColors.Slate800)
                    .clickable(onClick = onLeaveRoom)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("room_leave_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Leave room",
                    tint = StopColors.Slate200,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = "Leave", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StopColors.Slate200)
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StopColors.Slate950)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(10.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(room.code))
                        copied = true
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("room_copy_code_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = room.code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = StopColors.Slate200
                )
                Icon(
                    imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copy room code",
                    tint = if (copied) StopColors.Emerald400 else StopColors.Slate400,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column {
            Text(
                text = room.name,
                style = MaterialTheme.typography.headlineMedium,
                color = StopColors.White
            )
            Text(
                text = "${room.settings.roundCount} Rounds · ${room.settings.roundDurationSec}s per round · ${room.settings.categories.size} Categories",
                fontSize = 12.sp,
                color = StopColors.Slate400
            )
        }

        HorizontalDivider(color = StopColors.Slate800)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isHost) "You are the host" else "Waiting for host to launch...",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHost) StopColors.Amber400 else StopColors.Slate300
            )

            if (isHost) {
                Button(
                    onClick = onStartRound,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(StopColors.Rose600, StopColors.Amber500)))
                        .testTag("room_start_round_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start round",
                            tint = StopColors.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Start Round ${room.currentRound + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StopColors.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedPlayersCard(room: GameRoom, myProfile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StopColors.Slate900.copy(alpha = 0.92f))
            .border(1.dp, StopColors.Slate800, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Groups,
                contentDescription = "Connected players",
                tint = StopColors.Rose400,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Connected Players (${room.players.size})",
                style = MaterialTheme.typography.titleMedium,
                color = StopColors.White
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            room.players.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StopColors.Slate950.copy(alpha = 0.85f))
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(14.dp))
                        .padding(12.dp),
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StopColors.Slate900)
                                .border(1.dp, StopColors.Slate800, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = p.avatar, fontSize = 18.sp)
                        }
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = p.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StopColors.White
                                )
                                if (p.id == myProfile.id) {
                                    Text(text = "(You)", fontSize = 11.sp, color = StopColors.Slate400)
                                }
                            }
                            Text(
                                text = if (p.id == room.hostId) "Host" else "Competitor",
                                fontSize = 11.sp,
                                color = StopColors.Slate400
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StopColors.Emerald500)
                        )
                        Text(
                            text = "Ready",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StopColors.Emerald400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchChatCard(
    room: GameRoom,
    myProfileId: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(room.chat.size) {
        if (room.chat.isNotEmpty()) {
            listState.animateScrollToItem(room.chat.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(StopColors.Slate900.copy(alpha = 0.92f))
            .border(1.dp, StopColors.Slate800, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChatBubbleOutline,
                    contentDescription = "Match chat",
                    tint = StopColors.Amber400,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Match Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = StopColors.White
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("🔥", "🛑", "😱", "👏").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSendMessage(emoji) }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 14.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = StopColors.Slate800)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(room.chat, key = { it.id }) { msg ->
                val isMine = msg.senderId == myProfileId
                val bg = when {
                    msg.isSystem -> StopColors.Slate950.copy(alpha = 0.5f)
                    isMine -> StopColors.Rose950.copy(alpha = 0.45f)
                    else -> StopColors.Slate950.copy(alpha = 0.85f)
                }
                val border = when {
                    msg.isSystem -> StopColors.Amber500.copy(alpha = 0.25f)
                    isMine -> StopColors.Rose500.copy(alpha = 0.35f)
                    else -> StopColors.Slate800
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (isMine) 18.dp else 0.dp, end = if (!isMine && !msg.isSystem) 18.dp else 0.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    if (!msg.isSystem) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = msg.senderAvatar, fontSize = 12.sp)
                            Text(
                                text = msg.senderName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StopColors.Slate400,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = msg.text,
                        fontSize = 12.sp,
                        fontStyle = if (msg.isSystem) FontStyle.Italic else FontStyle.Normal,
                        color = if (msg.isSystem) StopColors.Amber300 else StopColors.Slate100
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = StopColors.Slate800)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { if (it.length <= 160) chatInput = it },
                placeholder = { Text("Type message...", fontSize = 12.sp, color = StopColors.Slate600) },
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
                modifier = Modifier.weight(1f).testTag("room_chat_input")
            )
            IconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        onSendMessage(chatInput.trim())
                        chatInput = ""
                    }
                },
                enabled = chatInput.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (chatInput.isNotBlank()) StopColors.Rose600 else StopColors.Slate800)
                    .testTag("room_chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send chat message",
                    tint = StopColors.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
