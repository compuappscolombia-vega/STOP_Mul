package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEngine
import com.example.ui.theme.StopColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun StopCelebrationOverlay(
    playerName: String,
    filledCount: Int,
    totalCategories: Int,
    isDailyChallenge: Boolean,
    soundEngine: SoundEngine,
    onCelebrationComplete: () -> Unit
) {
    var entered by remember { mutableStateOf(false) }

    val hostQuote = remember(filledCount, totalCategories, isDailyChallenge) {
        when {
            isDailyChallenge && filledCount >= totalCategories ->
                "¡DESAFÍO COMPLETO! ¡Increíble velocidad y precisión, el público ruge!"
            filledCount >= totalCategories ->
                "¡BRAVO! ¡Cartón lleno! ¡El estudio entero está de pie aplaudiendo!"
            filledCount >= (totalCategories / 2).coerceAtLeast(1) ->
                "¡STOP ESPECTACULAR! ¡Gran actitud y reflejos de campeón!"
            else ->
                "¡TIEMPO DETENIDO! ¡Excelente valentía al presionar STOP!"
        }
    }

    LaunchedEffect(Unit) {
        entered = true
        soundEngine.playStopBuzzerAndFanfare()
        soundEngine.playCrowdApplauseAndCheer(2750)
        soundEngine.speakVoice(
            text = "¡STOP! ¡Bravo $playerName! ¡Escucha esos aplausos del público!",
            rate = 1.24f,
            pitch = 1.18f,
            flush = true
        )
        delay(2750L)
        onCelebrationComplete()
    }

    val badgeScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.45f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 260f),
        label = "celebrationBadgeScale"
    )

    val infinite = rememberInfiniteTransition(label = "stop_celebration")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crowd_wave"
    )
    val shockwaveProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shockwave"
    )
    val bubbleProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floating_cheers"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        StopColors.Slate950.copy(alpha = 0.94f),
                        Color(0xFF1F0D24).copy(alpha = 0.95f),
                        StopColors.Slate950.copy(alpha = 0.96f)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCelebrationComplete
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Confetti Shower
        ConfettiBurstOverlay()

        // Expanding Game-Show Stage Shockwave Rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val maxR = size.minDimension * 0.48f
            val r1 = (shockwaveProgress * maxR).coerceAtLeast(1f)
            val r2 = (((shockwaveProgress + 0.45f) % 1f) * maxR).coerceAtLeast(1f)
            drawCircle(
                color = StopColors.Rose500.copy(alpha = (1f - shockwaveProgress) * 0.42f),
                radius = r1,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
            val p2 = (shockwaveProgress + 0.45f) % 1f
            drawCircle(
                color = StopColors.Amber400.copy(alpha = (1f - p2) * 0.36f),
                radius = r2,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Rising Joyful Crowd Bubbles
        val risingIcons = listOf("👏", "🎉", "🥳", "⭐", "🙌", "✨", "🎊")
        risingIcons.forEachIndexed { idx, icon ->
            val p = (bubbleProgress + idx * 0.14f) % 1f
            val yOffset = (150f - p * 320f).dp
            val xOffset = ((-130 + idx * 44) + sin((p * 6.28f + idx).toDouble()) * 14.0).dp
            Text(
                text = icon,
                fontSize = (22 + (idx % 3) * 4).sp,
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .alpha((1f - p) * 0.9f)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .scale(badgeScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game Show Applause Pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(StopColors.Amber500.copy(alpha = 0.20f))
                    .border(1.dp, StopColors.Amber400.copy(alpha = 0.65f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = "Applause celebration",
                    tint = StopColors.Amber300,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "👏 ¡OVACIÓN DEL PÚBLICO! 👏",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = StopColors.Amber300
                )
            }

            // Giant STOP Celebration Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "¡STOP!",
                    style = TextStyle(
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 5.sp,
                        brush = Brush.linearGradient(
                            listOf(StopColors.White, StopColors.Amber300, StopColors.Rose500)
                        )
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "¡$playerName completó $filledCount de $totalCategories categorías!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StopColors.Emerald300,
                    textAlign = TextAlign.Center
                )
            }

            // Animated Game-Show Host Announcer Quote + Voice Equalizer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StopColors.Slate900.copy(alpha = 0.92f))
                    .border(1.5.dp, StopColors.Cyan400.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
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
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = "Host announcer",
                            tint = StopColors.Cyan300,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ANIMADOR EN VIVO",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = StopColors.Cyan300
                        )
                    }

                    // Live Voice Equalizer Bars
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Audio active",
                            tint = StopColors.Amber400,
                            modifier = Modifier.size(14.dp)
                        )
                        repeat(5) { barIdx ->
                            val barFactor = 0.4f + 0.6f * ((sin(wavePhase.toDouble() * 1.5 + barIdx * 1.1) + 1.0) / 2.0).toFloat()
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height((6 + barFactor * 16).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (barIdx % 2 == 0) StopColors.Cyan400 else StopColors.Rose400
                                    )
                            )
                        }
                    }
                }

                Text(
                    text = "\"$hostQuote\"",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StopColors.White
                )
            }

            // Happy Cheering Audience Row (Stadium Wave!)
            val crowdFaces = listOf("🙌", "🥳", "👏", "🤩", "👏", "😄", "🎉")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StopColors.Slate900.copy(alpha = 0.78f))
                    .border(1.dp, StopColors.Amber500.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                crowdFaces.forEachIndexed { idx, face ->
                    val bounceY = (sin(wavePhase.toDouble() + idx * 0.85) * 7.0).dp
                    val pulseScale = 1f + 0.14f * ((sin(wavePhase.toDouble() + idx * 0.85) + 1.0) / 2.0).toFloat()
                    Text(
                        text = face,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .offset(y = bounceY)
                            .scale(pulseScale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onCelebrationComplete,
                colors = ButtonDefaults.buttonColors(containerColor = StopColors.Rose600),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("stop_celebration_continue_button")
            ) {
                Text(
                    text = "Ver Puntuación",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = StopColors.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Continue to scorecard",
                    tint = StopColors.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
