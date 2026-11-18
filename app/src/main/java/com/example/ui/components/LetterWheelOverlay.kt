package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEngine
import com.example.game.GameRules
import com.example.ui.theme.StopColors
import kotlinx.coroutines.delay

private enum class WheelPhase {
    SPINNING,
    REVEALED,
    COUNT_3,
    COUNT_2,
    COUNT_1,
    COUNT_STOP
}

@Composable
fun LetterWheelOverlay(
    targetLetter: String,
    soundEngine: SoundEngine,
    onSpinComplete: () -> Unit
) {
    var displayLetter by remember(targetLetter) { mutableStateOf("A") }
    var phase by remember(targetLetter) { mutableStateOf(WheelPhase.SPINNING) }
    var announcerText by remember(targetLetter) {
        mutableStateOf("¡Bienvenidos al show! ¡La ruleta está en marcha!")
    }

    LaunchedEffect(targetLetter) {
        soundEngine.playGameShowIntro()
        soundEngine.speakVoice(
            text = "¡Bienvenidos! ¡Mucha actitud y energía, gira la ruleta!",
            rate = 1.24f,
            pitch = 1.18f,
            flush = true
        )

        var tickDelay = 42L
        val maxTicks = 16
        for (tick in 1..maxTicks) {
            delay(tickDelay)
            displayLetter = if (tick >= maxTicks) {
                targetLetter
            } else {
                GameRules.VALID_LETTERS.random()
            }
            soundEngine.playLetterTick()
            tickDelay += 12L
        }

        displayLetter = targetLetter
        phase = WheelPhase.REVEALED
        announcerText = "¡LETRA '$targetLetter'! ¡Con todo el ánimo, prepárate!"
        soundEngine.playLetterReveal()
        soundEngine.speakVoice(
            text = "¡Letra ${targetLetter.uppercase()}! ¡Vamos con todo! ¡Arrancamos en!",
            rate = 1.26f,
            pitch = 1.20f,
            flush = true
        )
        delay(1450L)

        phase = WheelPhase.COUNT_3
        announcerText = "¡Tres! Respira profundo..."
        soundEngine.playCountdownBeep(3)
        soundEngine.speakVoice("¡Tres!", rate = 1.32f, pitch = 1.18f, flush = true)
        delay(820L)

        phase = WheelPhase.COUNT_2
        announcerText = "¡Dos! ¡Manos listas!"
        soundEngine.playCountdownBeep(2)
        soundEngine.speakVoice("¡Dos!", rate = 1.32f, pitch = 1.22f, flush = true)
        delay(820L)

        phase = WheelPhase.COUNT_1
        announcerText = "¡Uno! ¡Máxima velocidad!"
        soundEngine.playCountdownBeep(1)
        soundEngine.speakVoice("¡Uno!", rate = 1.34f, pitch = 1.28f, flush = true)
        delay(820L)

        phase = WheelPhase.COUNT_STOP
        announcerText = "¡A JUGAR! ¡ESCRIBE RÁPIDO!"
        soundEngine.playCountdownStop()
        soundEngine.speakVoice("¡A JUGAR!", rate = 1.28f, pitch = 1.32f, flush = true)
        delay(880L)

        onSpinComplete()
    }

    val isSpinning = phase == WheelPhase.SPINNING
    val boxScale by animateFloatAsState(
        targetValue = if (isSpinning) 0.96f else 1.05f,
        animationSpec = tween(350),
        label = "tumblerScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StopColors.Slate950.copy(alpha = 0.94f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game-Show Host Attitude Pill
            Text(
                text = "🎙️ $announcerText",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StopColors.Cyan300,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Status Pill
            val (pillText, pillColor, pillBg) = when (phase) {
                WheelPhase.SPINNING -> Triple(
                    "GIRANDO LA RULETA DE LETRAS...",
                    StopColors.Amber300,
                    StopColors.Orange500.copy(alpha = 0.16f)
                )
                WheelPhase.REVEALED -> Triple(
                    "¡LETRA OFICIAL SELECCIONADA!",
                    StopColors.Cyan300,
                    StopColors.Cyan950
                )
                WheelPhase.COUNT_3, WheelPhase.COUNT_2, WheelPhase.COUNT_1 -> Triple(
                    "¡PREPÁRATE! ARRANCANDO EN:",
                    StopColors.Purple200,
                    StopColors.Purple950
                )
                WheelPhase.COUNT_STOP -> Triple(
                    "¡A JUGAR AHORA!",
                    StopColors.Rose300,
                    StopColors.Rose950
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(1.dp, pillColor.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(pillColor)
                )
                Text(
                    text = pillText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = pillColor
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Glowing Neon Tumbler Box
            val outerBrush = if (isSpinning) {
                Brush.linearGradient(
                    listOf(StopColors.Red600, StopColors.Amber400, StopColors.Orange500, StopColors.Fuchsia500)
                )
            } else {
                Brush.linearGradient(
                    listOf(StopColors.Cyan400, StopColors.Blue600, StopColors.Purple600, StopColors.Violet500)
                )
            }

            Box(
                modifier = Modifier
                    .scale(boxScale)
                    .size(196.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = if (isSpinning) StopColors.Orange500 else StopColors.Cyan400,
                        spotColor = if (isSpinning) StopColors.Fuchsia500 else StopColors.Purple500
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(outerBrush)
                    .padding(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(25.dp))
                        .background(StopColors.Slate950)
                        .border(
                            width = 1.dp,
                            color = (if (isSpinning) StopColors.Orange500 else StopColors.Cyan300).copy(alpha = 0.45f),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayLetter.uppercase(),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Black,
                        color = StopColors.White
                    )
                }
            }
        }

        // Countdown Overlay: 3... 2... 1... STOP!
        AnimatedVisibility(
            visible = phase == WheelPhase.COUNT_3 || phase == WheelPhase.COUNT_2 || phase == WheelPhase.COUNT_1 || phase == WheelPhase.COUNT_STOP,
            enter = fadeIn(tween(150)) + scaleIn(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StopColors.Slate950.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                when (phase) {
                    WheelPhase.COUNT_3, WheelPhase.COUNT_2, WheelPhase.COUNT_1 -> {
                        val num = when (phase) {
                            WheelPhase.COUNT_3 -> "3"
                            WheelPhase.COUNT_2 -> "2"
                            else -> "1"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = num,
                                style = TextStyle(
                                    fontSize = 132.sp,
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.verticalGradient(
                                        listOf(StopColors.White, StopColors.Cyan300, StopColors.Purple500)
                                    )
                                )
                            )
                            Text(
                                text = "$num...",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 4.sp,
                                color = StopColors.Slate300
                            )
                        }
                    }
                    WheelPhase.COUNT_STOP -> {
                        StopTitleLockup(
                            subtitleText = "¡A JUGAR AHORA!",
                            titleFontSize = 72.sp,
                            modifier = Modifier.fillMaxSize().wrapToCenter()
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

private fun Modifier.wrapToCenter(): Modifier = this.padding(16.dp)
