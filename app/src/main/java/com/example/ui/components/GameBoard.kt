package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameRules
import com.example.model.ActivePlayerStripItem
import com.example.model.Category
import com.example.ui.theme.StopColors

@Composable
fun GameBoard(
    categories: List<Category>,
    targetLetter: String,
    answers: Map<String, String>,
    onAnswerChange: (String, String) -> Unit,
    onCallStop: () -> Unit,
    isStopDisabled: Boolean = false,
    activePlayers: List<ActivePlayerStripItem> = emptyList(),
    botSpeedSec: Int? = null,
    onBotSpeedChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val filledCount = categories.count { (answers[it.id] ?: "").trim().isNotEmpty() }
    val canStop = filledCount >= 1 && !isStopDisabled
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember(categories) {
        List(categories.size) { FocusRequester() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Competitors Live Strip
        if (activePlayers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StopColors.Slate900.copy(alpha = 0.85f))
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(18.dp))
                    .padding(12.dp),
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
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StopColors.Emerald400)
                        )
                        Text(
                            text = "Competidores en Tiempo Real",
                            style = MaterialTheme.typography.labelMedium,
                            color = StopColors.Slate300
                        )
                    }

                    if (botSpeedSec != null && onBotSpeedChange != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StopColors.Slate950)
                                .border(1.dp, StopColors.Slate800, RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..6).forEach { sec ->
                                val selected = botSpeedSec == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) StopColors.Amber500 else StopColors.Slate900)
                                        .clickable { onBotSpeedChange(sec) }
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                        .testTag("board_bot_speed_${sec}s"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (selected) StopColors.Slate950 else StopColors.Slate400
                                    )
                                }
                            }
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useRow = maxWidth >= 560.dp && activePlayers.size <= 3
                    if (useRow) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activePlayers.forEach { p ->
                                CompetitorCard(
                                    player = p,
                                    humanFilledCount = filledCount,
                                    fallbackTotal = categories.size,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activePlayers.forEach { p ->
                                CompetitorCard(
                                    player = p,
                                    humanFilledCount = filledCount,
                                    fallbackTotal = categories.size,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Responsive Category Grid (1 col on phone, 2 cols on tablet/wide)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 600.dp) 2 else 1
            val rows = categories.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEachIndexed { rowIndex, rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCats.forEachIndexed { colIndex, cat ->
                            val globalIdx = rowIndex * columns + colIndex
                            CategoryInputCard(
                                category = cat,
                                targetLetter = targetLetter,
                                value = answers[cat.id].orEmpty(),
                                onValueChange = { onAnswerChange(cat.id, it) },
                                focusRequester = focusRequesters[globalIdx],
                                isLast = globalIdx == categories.lastIndex,
                                onNext = {
                                    if (globalIdx < categories.lastIndex) {
                                        focusRequesters[globalIdx + 1].requestFocus()
                                    } else {
                                        focusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowCats.size < columns) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Iconic STOP! Slap Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onCallStop,
                enabled = canStop,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = StopColors.Slate800
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (canStop) {
                            Brush.horizontalGradient(
                                listOf(StopColors.Rose600, StopColors.Rose500, StopColors.Red600)
                            )
                        } else {
                            Brush.horizontalGradient(listOf(StopColors.Slate800, StopColors.Slate800))
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (canStop) StopColors.White.copy(alpha = 0.25f) else StopColors.Slate700,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("stop_slap_button")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🛑 STOP!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = if (canStop) StopColors.White else StopColors.Slate500
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StopColors.Slate950.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$filledCount/${categories.size} Ready",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (canStop) StopColors.White else StopColors.Slate400
                            )
                        }
                    }
                    if (canStop) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SLAP TO FINISH ROUND & LOCK ALL ANSWERS!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = StopColors.Rose300
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitorCard(
    player: ActivePlayerStripItem,
    humanFilledCount: Int,
    fallbackTotal: Int,
    modifier: Modifier = Modifier
) {
    val total = player.totalCategories.coerceAtLeast(1).takeIf { it > 0 } ?: fallbackTotal.coerceAtLeast(1)
    val count = if (player.isHuman) humanFilledCount else player.filledCount
    val pct = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    val borderColor = when {
        player.hasStopped -> StopColors.Rose500.copy(alpha = 0.7f)
        player.isHuman -> StopColors.Slate700
        else -> StopColors.Slate800
    }
    val bgColor = when {
        player.hasStopped -> StopColors.Rose950.copy(alpha = 0.45f)
        player.isHuman -> StopColors.Slate950.copy(alpha = 0.85f)
        else -> StopColors.Slate950.copy(alpha = 0.55f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = player.avatar, fontSize = 15.sp)
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = StopColors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$count/$total",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = StopColors.Slate300
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(StopColors.Slate800)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct)
                    .clip(CircleShape)
                    .background(
                        when {
                            player.hasStopped -> StopColors.Rose500
                            player.isHuman -> StopColors.Amber400
                            else -> StopColors.Indigo400
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (player.hasStopped) "¡Gritó STOP!" else player.statusText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = StopColors.Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (player.hasStopped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StopColors.Rose500)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "STOP",
                        color = StopColors.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryInputCard(
    category: Category,
    targetLetter: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isLast: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasInput = value.trim().isNotEmpty()
    val letterMatch = if (hasInput) GameRules.matchesStartingLetter(value, targetLetter) else false

    val borderColor = when {
        letterMatch -> StopColors.Emerald500.copy(alpha = 0.5f)
        hasInput -> StopColors.Amber500.copy(alpha = 0.5f)
        else -> StopColors.Slate800
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StopColors.Slate900.copy(alpha = 0.85f))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
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
                Text(text = category.icon, fontSize = 18.sp)
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = StopColors.Slate200
                )
            }

            if (hasInput) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (letterMatch) Icons.Filled.Check else Icons.Filled.ErrorOutline,
                        contentDescription = if (letterMatch) "Valid starting letter" else "Letter mismatch",
                        tint = if (letterMatch) StopColors.Emerald400 else StopColors.Amber400,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (letterMatch) "Starts with ${targetLetter.uppercase()}" else "Needs '${targetLetter.uppercase()}'",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = if (letterMatch) StopColors.Emerald400 else StopColors.Amber400
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = category.placeholder,
                    color = StopColors.Slate600,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Text(
                    text = targetLetter.uppercase(),
                    color = StopColors.Rose400,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = if (isLast) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() },
                onDone = { onNext() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StopColors.Slate950,
                unfocusedContainerColor = StopColors.Slate950,
                focusedBorderColor = StopColors.Rose500,
                unfocusedBorderColor = StopColors.Slate800,
                focusedTextColor = StopColors.White,
                unfocusedTextColor = StopColors.White,
                cursorColor = StopColors.Rose400
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag("category_input_${category.id}")
        )
    }
}
