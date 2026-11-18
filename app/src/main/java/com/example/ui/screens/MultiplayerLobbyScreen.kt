package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.game.GameRules
import com.example.model.Difficulty
import com.example.model.PublicRoomSummary
import com.example.model.RoomSettings
import com.example.model.UserProfile
import com.example.ui.components.FooterCredit
import com.example.ui.components.StopAtmosphereBackground
import com.example.ui.theme.StopColors

@Composable
fun MultiplayerLobbyScreen(
    profile: UserProfile,
    publicRooms: List<PublicRoomSummary>,
    onRefreshRooms: () -> Unit,
    onCreateRoom: (name: String, settings: RoomSettings, addPracticeBot: Boolean) -> Unit,
    onJoinRoom: (code: String) -> Unit,
    onBack: () -> Unit
) {
    var joinCodeInput by remember { mutableStateOf("") }
    var showCreateModal by remember { mutableStateOf(false) }

    BackHandler {
        if (showCreateModal) showCreateModal = false else onBack()
    }

    StopAtmosphereBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StopColors.Slate900)
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("lobby_back_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to title",
                            tint = StopColors.Slate200,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Title Screen",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StopColors.Slate200
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Online Multiplayer Rooms",
                            style = MaterialTheme.typography.titleLarge,
                            color = StopColors.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Join a room or host custom categories",
                            fontSize = 11.sp,
                            color = StopColors.Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onRefreshRooms,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StopColors.Slate900)
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                            .testTag("lobby_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh room list",
                            tint = StopColors.Slate300
                        )
                    }
                }

                HorizontalDivider(color = StopColors.Slate900)

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 720.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            LeftLobbyColumn(
                                joinCodeInput = joinCodeInput,
                                onJoinCodeChange = { joinCodeInput = it.uppercase() },
                                onJoinSubmit = { if (joinCodeInput.isNotBlank()) onJoinRoom(joinCodeInput.trim()) },
                                onOpenCreate = { showCreateModal = true },
                                modifier = Modifier.weight(1f)
                            )
                            PublicRoomsPanel(
                                rooms = publicRooms,
                                onJoinRoom = onJoinRoom,
                                onOpenCreate = { showCreateModal = true },
                                modifier = Modifier.weight(1.7f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            LeftLobbyColumn(
                                joinCodeInput = joinCodeInput,
                                onJoinCodeChange = { joinCodeInput = it.uppercase() },
                                onJoinSubmit = { if (joinCodeInput.isNotBlank()) onJoinRoom(joinCodeInput.trim()) },
                                onOpenCreate = { showCreateModal = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            PublicRoomsPanel(
                                rooms = publicRooms,
                                onJoinRoom = onJoinRoom,
                                onOpenCreate = { showCreateModal = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                FooterCredit()
            }
        }
    }

    if (showCreateModal) {
        CreateCustomRoomDialog(
            profile = profile,
            onDismiss = { showCreateModal = false },
            onCreate = { name, settings, addBot ->
                showCreateModal = false
                onCreateRoom(name, settings, addBot)
            }
        )
    }
}

@Composable
private fun LeftLobbyColumn(
    joinCodeInput: String,
    onJoinCodeChange: (String) -> Unit,
    onJoinSubmit: () -> Unit,
    onOpenCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Join with Code Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(StopColors.Slate900.copy(alpha = 0.92f))
                .border(1.dp, StopColors.Slate800, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Join with code",
                    tint = StopColors.Rose400,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Join with Room Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = StopColors.White
                )
            }
            Text(
                text = "Enter the room code shared by your friend (e.g. STOP-ARENA)",
                fontSize = 12.sp,
                color = StopColors.Slate400
            )
            OutlinedTextField(
                value = joinCodeInput,
                onValueChange = onJoinCodeChange,
                placeholder = {
                    Text(
                        text = "STOP-XXXX",
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = StopColors.Slate600
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = StopColors.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StopColors.Slate950,
                    unfocusedContainerColor = StopColors.Slate950,
                    focusedBorderColor = StopColors.Rose500,
                    unfocusedBorderColor = StopColors.Slate800
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("lobby_join_code_input")
            )
            Button(
                onClick = onJoinSubmit,
                enabled = joinCodeInput.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = StopColors.Rose600),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("lobby_join_code_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Join room",
                    tint = StopColors.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = "Join Room", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StopColors.White)
            }
        }

        // Host Custom Match CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            StopColors.Amber500.copy(alpha = 0.2f),
                            StopColors.Rose500.copy(alpha = 0.2f)
                        )
                    )
                )
                .border(1.dp, StopColors.Amber500.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenCreate)
                .padding(16.dp)
                .testTag("lobby_host_custom_button"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Host Custom Match",
                    style = MaterialTheme.typography.titleMedium,
                    color = StopColors.White
                )
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Host custom match",
                    tint = StopColors.Amber400
                )
            }
            Text(
                text = "Customize categories, round timer (30s–90s), and toggle private codes.",
                fontSize = 12.sp,
                color = StopColors.Slate300
            )
        }
    }
}

@Composable
private fun PublicRoomsPanel(
    rooms: List<PublicRoomSummary>,
    onJoinRoom: (String) -> Unit,
    onOpenCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(StopColors.Slate900.copy(alpha = 0.92f))
            .border(1.dp, StopColors.Slate800, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = "Public matches",
                    tint = StopColors.Emerald400,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Public Matches",
                    style = MaterialTheme.typography.titleMedium,
                    color = StopColors.White
                )
            }
            Text(
                text = "${rooms.size} ${if (rooms.size == 1) "room" else "rooms"} open",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = StopColors.Slate400
            )
        }

        if (rooms.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = "No rooms",
                    tint = StopColors.Slate600,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "No public rooms currently waiting",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = StopColors.Slate300
                )
                Text(
                    text = "Be the host and create the first room!",
                    fontSize = 12.sp,
                    color = StopColors.Slate500
                )
                Button(
                    onClick = onOpenCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = StopColors.Slate800),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(text = "Create Room Now", fontSize = 12.sp, color = StopColors.White)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rooms.forEach { room ->
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
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = room.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StopColors.Slate100,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "(${room.code})",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = StopColors.Slate400
                                )
                            }
                            Text(
                                text = "Host: ${room.hostName} · ${room.maxRounds} Rounds · ${room.duration}s",
                                fontSize = 11.sp,
                                color = StopColors.Slate400,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = "Player count",
                                    tint = StopColors.Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${room.playerCount}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = StopColors.Slate300
                                )
                            }
                            Button(
                                onClick = { onJoinRoom(room.code) },
                                colors = ButtonDefaults.buttonColors(containerColor = StopColors.Rose600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(44.dp).testTag("join_room_${room.code}")
                            ) {
                                Text(text = "Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StopColors.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCustomRoomDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onCreate: (name: String, settings: RoomSettings, addPracticeBot: Boolean) -> Unit
) {
    var roomName by remember { mutableStateOf("${profile.name}'s Match") }
    var roundCount by remember { mutableIntStateOf(3) }
    var roundDurationSec by remember { mutableIntStateOf(60) }
    var isPrivate by remember { mutableStateOf(false) }
    var addPracticeBot by remember { mutableStateOf(false) }
    var selectedCategoryIds by remember {
        mutableStateOf(GameRules.DEFAULT_CATEGORIES.take(6).map { it.id })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = "Host Custom Match",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StopColors.White
                    )
                    Text(
                        text = "Configure custom categories, round time, and privacy",
                        fontSize = 12.sp,
                        color = StopColors.Slate400
                    )
                }

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { if (it.length <= 30) roomName = it },
                    label = { Text("Room Name", fontSize = 12.sp) },
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
                    modifier = Modifier.fillMaxWidth().testTag("create_room_name_input")
                )

                // Rounds selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Rounds", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StopColors.Slate300)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(1 to "1", 3 to "3", 5 to "5", 7 to "7").forEach { (valRounds, label) ->
                            val selected = roundCount == valRounds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) StopColors.Rose600 else StopColors.Slate950)
                                    .border(1.dp, if (selected) StopColors.Rose500 else StopColors.Slate800, RoundedCornerShape(10.dp))
                                    .clickable { roundCount = valRounds }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$label Rds",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) StopColors.White else StopColors.Slate400
                                )
                            }
                        }
                    }
                }

                // Timer selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Round Timer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StopColors.Slate300)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(30, 45, 60, 90).forEach { sec ->
                            val selected = roundDurationSec == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) StopColors.Rose600 else StopColors.Slate950)
                                    .border(1.dp, if (selected) StopColors.Rose500 else StopColors.Slate800, RoundedCornerShape(10.dp))
                                    .clickable { roundDurationSec = sec }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) StopColors.White else StopColors.Slate400
                                )
                            }
                        }
                    }
                }

                // Categories multi-select (3-8)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Active Categories (${selectedCategoryIds.size} selected, 3–8 allowed)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StopColors.Slate300
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StopColors.Slate950)
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GameRules.DEFAULT_CATEGORIES.forEach { cat ->
                            val isSelected = selectedCategoryIds.contains(cat.id)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) StopColors.Rose500.copy(alpha = 0.2f) else StopColors.Slate900)
                                    .border(
                                        1.dp,
                                        if (isSelected) StopColors.Rose500.copy(alpha = 0.5f) else StopColors.Slate800,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            if (selectedCategoryIds.size > 3) {
                                                selectedCategoryIds = selectedCategoryIds - cat.id
                                            }
                                        } else if (selectedCategoryIds.size < 8) {
                                            selectedCategoryIds = selectedCategoryIds + cat.id
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = cat.icon, fontSize = 13.sp)
                                Text(
                                    text = cat.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) StopColors.White else StopColors.Slate400
                                )
                            }
                        }
                    }
                }

                // Private toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StopColors.Slate950)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivate) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "Private match toggle",
                            tint = if (isPrivate) StopColors.Amber400 else StopColors.Emerald400,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(text = "Private Match", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StopColors.White)
                            Text(text = "Only players with code can join", fontSize = 10.sp, color = StopColors.Slate400)
                        }
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = StopColors.Rose500)
                    )
                }

                // Add AI Bot toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StopColors.Slate950)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Include AI Challenger Bot", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StopColors.White)
                        Text(text = "Add Vocabulary Vicky alongside real players", fontSize = 10.sp, color = StopColors.Slate400)
                    }
                    Switch(
                        checked = addPracticeBot,
                        onCheckedChange = { addPracticeBot = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = StopColors.Amber500)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = StopColors.Slate800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 12.sp, color = StopColors.Slate300)
                    }
                    Button(
                        onClick = {
                            val chosenCats = GameRules.DEFAULT_CATEGORIES.filter { selectedCategoryIds.contains(it.id) }
                            onCreate(
                                roomName.trim().ifEmpty { "${profile.name}'s Party" },
                                RoomSettings(
                                    roundCount = roundCount,
                                    roundDurationSec = roundDurationSec,
                                    categories = chosenCats,
                                    isPrivate = isPrivate,
                                    difficulty = Difficulty.MEDIUM
                                ),
                                addPracticeBot
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StopColors.Rose600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("create_room_submit_button")
                    ) {
                        Text(text = "Create & Enter Room", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StopColors.White)
                    }
                }
            }
        }
    }
}
