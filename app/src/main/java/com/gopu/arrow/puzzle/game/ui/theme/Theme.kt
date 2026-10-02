package com.gopu.arrow.puzzle.game.ui.theme

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

/* Premium light menu palette.
   The menu keeps a warm ivory field with almost no colour in it, so the
   wordmark gradients, the cyan hero arrow and the coral action button carry
   every accent on the screen. */
val Ivory = Color(0xFFF4F5EE)
val IvoryTop = Color(0xFFFAFBF5)
val IvoryBottom = Color(0xFFE9ECDF)

/** Wordmark: ARROW runs charcoal into rust, PUZZLE coral into gold. */
val CharcoalDeep = Color(0xFF23242E)
val Rust = Color(0xFF8C3A22)
val Ember = Color(0xFFFF6A3D)
val AmberGold = Color(0xFFFFA62B)

/** Menu text. Dark blue-teal for headings, a softer tone for supporting copy. */
val TealInk = Color(0xFF0F3B45)
val TealMuted = Color(0xFF5F828A)

/** Hero arrow: light facet, body, shaded facet and the 3D edge underneath it. */
val HeroCyanLight = Color(0xFF6FE6F8)
val HeroCyan = Color(0xFF23D2EF)
val HeroCyanDeep = Color(0xFF0B93B4)
val HeroCyanEdge = Color(0xFF05697F)

/** Primary action gradient, star accent and card surface. */
val PlayTop = Color(0xFFFF7A45)
val PlayMid = Color(0xFFFF5A33)
val PlayBottom = Color(0xFFEE4526)
val StarGold = Color(0xFFF6B93B)
val CardSurface = Color(0xFFFCFDF8)

/** Muted dot drawn on cells an arrow has left, so the board grid shows through. */
val GridDot = Color(0x332C5A66)

/** The seven neon arrow colours the maze cycles through. */
val NeonGreen = Color(0xFF3DFF9E)
val NeonOrange = Color(0xFFFF8A1F)
val NeonBlue = Color(0xFF4D7CFF)

/** Pure white core used for the hot centre of every neon tube. */
val NeonCore = Color(0xFFFFFFFF)

/** Red reserved for blocked feedback, never used as an arrow colour. */
val NeonRed = Color(0xFFFF2D55)
val NeonRedDeep = Color(0xFFC4002B)

/** Dark stage behind the puzzle: page, board plate, and board frame. */
val StageVoid = Color(0xFF05060F)
val StageDeep = Color(0xFF0A0A1C)
val BoardPlate = Color(0xFF101029)
val BoardPlateEdge = Color(0xFF232352)
val BoardFrame = Color(0xFF2E2E6B)
val GridDotDim = Color(0x26FFFFFF)

/** HUD text and surface colours for the neon gameplay screen. */
val NeonText = Color(0xFFEAF2FF)
val NeonTextDim = Color(0xFF9AA3D4)
val NeonPanel = Color(0xFF15152E)
val NeonPanelSoft = Color(0xFF1E1E3E)

/* Level select: a near-white page with six pastel card gradients.
   Nothing here is saturated, so the gold stars stay the loudest thing on the
   screen and the navy headings keep the hierarchy readable. */

/** Page field behind the frosted plates. */
val BackdropTop = Color(0xFFFBFCFF)
val BackdropMid = Color(0xFFF4F7FD)
val BackdropBottom = Color(0xFFEDF2FB)

/** Deep blue headings and the softer tone used for supporting copy. */
val NavyInk = Color(0xFF18234F)
val NavyMuted = Color(0xFF6A74A3)

/** Title accent: the gradient the LEVEL wordmark runs through. */
val AccentViolet = Color(0xFF7B5CFF)
val AccentAzure = Color(0xFF31A9FF)

/** Pack progress bar fill. */
val ProgressGreenSoft = Color(0xFF86E7BC)
val ProgressGreen = Color(0xFF3ED598)

/** Locked level plate: flat, desaturated, deliberately the dullest card. */
val LockedGlassTop = Color(0xFFE9EDF6)
val LockedGlassBottom = Color(0xFFDCE2EF)

/**
 * Six soft pastel pairs (top, bottom) that rotate across a pack's level grid,
 * so ten cards read as a varied set without any single card shouting.
 */
val LevelCardPalettes: List<List<Color>> = listOf(
    listOf(Color(0xFFC8F3E0), Color(0xFF8FE4C4)), // mint
    listOf(Color(0xFFD2E9FC), Color(0xFFA6D6F5)), // sky
    listOf(Color(0xFFFFE9D2), Color(0xFFFFCF9F)), // peach
    listOf(Color(0xFFE7DBFD), Color(0xFFCBB9F7)), // lilac
    listOf(Color(0xFFFFDFEB), Color(0xFFFFC1DA)), // rose
    listOf(Color(0xFFD0F2F4), Color(0xFFA1E6EC))  // aqua
)

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
    background = Ivory,
    onBackground = TealInk,
    surface = CardSurface,
    onSurface = TealInk,
    surfaceVariant = IvoryBottom,
    onSurfaceVariant = TealMuted,
    outline = TealInk.copy(alpha = 0.18f)
)

private val ArrowTypography = Typography(
    displaySmall = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = 2.sp),
    headlineMedium = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
