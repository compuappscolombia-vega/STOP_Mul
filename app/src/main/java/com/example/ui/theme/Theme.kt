package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object StopColors {
    val Slate950 = Color(0xFF020617)
    val Slate900 = Color(0xFF0F172A)
    val Slate800 = Color(0xFF1E293B)
    val Slate700 = Color(0xFF334155)
    val Slate600 = Color(0xFF475569)
    val Slate500 = Color(0xFF64748B)
    val Slate400 = Color(0xFF94A3B8)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate100 = Color(0xFFF1F5F9)

    val Rose600 = Color(0xFFE11D48)
    val Rose500 = Color(0xFFF43F5E)
    val Rose400 = Color(0xFFFB7185)
    val Rose300 = Color(0xFFFDA4AF)
    val Rose950 = Color(0xFF4C0519)

    val Red600 = Color(0xFFDC2626)

    val Amber500 = Color(0xFFF59E0B)
    val Amber400 = Color(0xFFFBBF24)
    val Amber300 = Color(0xFFFCD34D)
    val Amber700 = Color(0xFFB45309)
    val Amber950 = Color(0xFF451A03)

    val Orange500 = Color(0xFFF97316)
    val Fuchsia500 = Color(0xFFD946EF)

    val Cyan500 = Color(0xFF06B6D4)
    val Cyan400 = Color(0xFF22D3EE)
    val Cyan300 = Color(0xFF67E8F9)
    val Cyan950 = Color(0xFF083344)

    val Blue600 = Color(0xFF2563EB)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo400 = Color(0xFF818CF8)
    val Purple600 = Color(0xFF9333EA)
    val Purple500 = Color(0xFFA855F7)
    val Purple200 = Color(0xFFE9D5FF)
    val Purple950 = Color(0xFF3B0764)
    val Violet500 = Color(0xFF8B5CF6)
    val Violet400 = Color(0xFFA78BFA)

    val Emerald600 = Color(0xFF059669)
    val Emerald500 = Color(0xFF10B981)
    val Emerald400 = Color(0xFF34D399)
    val Emerald300 = Color(0xFF6EE7B7)
    val Emerald950 = Color(0xFF022C22)

    val White = Color(0xFFFFFFFF)
}

private val StopDarkColorScheme = darkColorScheme(
    primary = StopColors.Rose500,
    onPrimary = StopColors.White,
    primaryContainer = StopColors.Rose950,
    onPrimaryContainer = StopColors.Rose300,
    secondary = StopColors.Amber500,
    onSecondary = StopColors.Slate950,
    secondaryContainer = StopColors.Amber950,
    onSecondaryContainer = StopColors.Amber300,
    tertiary = StopColors.Cyan400,
    onTertiary = StopColors.Slate950,
    tertiaryContainer = StopColors.Cyan950,
    onTertiaryContainer = StopColors.Cyan300,
    background = StopColors.Slate950,
    onBackground = StopColors.Slate100,
    surface = StopColors.Slate900,
    onSurface = StopColors.Slate100,
    surfaceVariant = StopColors.Slate800,
    onSurfaceVariant = StopColors.Slate400,
    outline = StopColors.Slate800,
    outlineVariant = StopColors.Slate700,
    error = StopColors.Rose400,
    onError = StopColors.Slate950
)

val StopTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = 2.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

val StopShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StopRushTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StopDarkColorScheme,
        typography = StopTypography,
        shapes = StopShapes,
        content = content
    )
}
