package com.gopu.arrowpuzzle.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF113C47)
val InkSoft = Color(0xFF2C5A66)
val CanvasWhite = Color(0xFFF3F5EC)
val Cloud = Color(0xFFE4E9DE)
val Coral = Color(0xFFF05D3A)
val CoralDark = Color(0xFFC8431F)
val Mint = Color(0xFF72C9A5)
val Gold = Color(0xFFF4BD4F)
val TileBlue = Color(0xFF3E99C1)
val TileBlueDark = Color(0xFF2A708F)
val Locked = Color(0xFFB9C2B7)

val NeonCyan = Color(0xFF2BE7FF)
val NeonMagenta = Color(0xFFFF3DCE)
val NeonLime = Color(0xFFA6FF3D)
val NeonAmber = Color(0xFFFFD11A)
val NeonViolet = Color(0xFF9B5CFF)
val FlameOrange = Color(0xFFFF6B00)
val FlameYellow = Color(0xFFFFC107)
val BoardDark = Color(0xFF0B222B)
val BoardCell = Color(0xFF123A46)

/** Muted dot drawn on cells an arrow has left, so the board grid shows through. */
val GridDot = Color(0x332C5A66)

private val ArrowColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Coral.copy(alpha = 0.15f),
    onPrimaryContainer = CoralDark,
    secondary = TileBlue,
    onSecondary = Color.White,
    secondaryContainer = TileBlue.copy(alpha = 0.15f),
    onSecondaryContainer = TileBlueDark,
    tertiary = Mint,
    onTertiary = Ink,
    background = CanvasWhite,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Cloud,
    onSurfaceVariant = InkSoft,
    outline = Ink.copy(alpha = 0.25f)
)

private val ArrowTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = 2.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

private val ArrowShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ArrowPuzzleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArrowColorScheme,
        typography = ArrowTypography,
        shapes = ArrowShapes,
        content = content
    )
}
