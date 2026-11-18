package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StopColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StopAtmosphereBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StopColors.Slate950)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        StopColors.Rose500.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.05f),
                    radius = size.maxDimension * 0.65f
                ),
                radius = size.maxDimension * 0.65f,
                center = Offset(size.width * 0.5f, size.height * 0.05f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        StopColors.Amber500.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.15f),
                    radius = size.maxDimension * 0.4f
                ),
                radius = size.maxDimension * 0.4f,
                center = Offset(size.width * 0.1f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        StopColors.Indigo500.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.9f, size.height * 0.88f),
                    radius = size.maxDimension * 0.45f
                ),
                radius = size.maxDimension * 0.45f,
                center = Offset(size.width * 0.9f, size.height * 0.88f)
            )
        }
        content()
    }
}

@Composable
fun NeonLetterBadge(
    letter: String,
    size: Dp = 96.dp,
    fontSize: TextUnit = 46.sp,
    showOnlineBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = StopColors.Cyan400.copy(alpha = glowAlpha),
                    spotColor = StopColors.Purple500.copy(alpha = glowAlpha)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            StopColors.Cyan400,
                            StopColors.Blue600,
                            StopColors.Purple600,
                            StopColors.Violet500
                        )
                    )
                )
                .padding(3.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(StopColors.Slate950)
                    .border(
                        width = 1.dp,
                        color = StopColors.Cyan400.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.uppercase(),
                    color = StopColors.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        if (showOnlineBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StopColors.Cyan500)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ONLINE",
                    color = StopColors.Slate950,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
fun StopTitleLockup(
    subtitleText: String = "MULTIPLAYER CATEGORY RUSH",
    titleFontSize: TextUnit = 60.sp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STOP!",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = titleFontSize,
                letterSpacing = 5.sp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        StopColors.White,
                        StopColors.Rose500,
                        StopColors.Rose600
                    )
                )
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitleText,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 4.sp,
                brush = Brush.horizontalGradient(
                    colors = listOf(StopColors.Amber400, StopColors.Amber500)
                )
            ),
            textAlign = TextAlign.Center
        )
    }
}

private data class ConfettiParticle(
    val xFraction: Float,
    val speed: Float,
    val angle: Float,
    val sizeDp: Float,
    val color: Color,
    val drift: Float
)

@Composable
fun ConfettiBurstOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        val palette = listOf(
            StopColors.Rose500,
            StopColors.Amber400,
            StopColors.Cyan400,
            StopColors.Emerald400,
            StopColors.Purple500
        )
        List(45) {
            ConfettiParticle(
                xFraction = Random.nextFloat(),
                speed = 0.5f + Random.nextFloat() * 0.9f,
                angle = Random.nextFloat() * 360f,
                sizeDp = 6f + Random.nextFloat() * 6f,
                color = palette[it % palette.size],
                drift = (Random.nextFloat() - 0.5f) * 80f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = ((progress * p.speed) % 1f) * size.height
            val x = p.xFraction * size.width + sin(progress * 6.28f) * p.drift
            rotate(degrees = p.angle + progress * 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = (1f - ((progress * p.speed) % 1f)) * 0.9f),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(p.sizeDp.dp.toPx(), (p.sizeDp * 0.55f).dp.toPx())
                )
            }
        }
    }
}

@Composable
fun FooterCredit(modifier: Modifier = Modifier) {
    Text(
        text = "createby CompuApps SecureDev",
        style = MaterialTheme.typography.labelSmall,
        color = StopColors.Slate500,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
