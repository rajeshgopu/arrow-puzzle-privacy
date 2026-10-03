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

/* Menu palette: a violet-indigo sky that falls away to a deep blue floor.
   The field is the only dark mass on the screen, so the gold wordmark, the
   glossy arrow cubes and the orange action pill are the only things competing
   for attention, and every one of them is light on dark. */
val MenuSkyTop = Color(0xFFA6B0F8)
val MenuSkyMid = Color(0xFF6C77E2)
val MenuSkyFloor = Color(0xFF3A45B6)
val MenuSkyDeep = Color(0xFF1B1F66)

/** Wordmark. ARROW is lit gold, PUZZLE lit ice, both on a cocoa outline. */
val MarkGoldTop = Color(0xFFFFF1C0)
val MarkGold = Color(0xFFFFC53A)
val MarkGoldDeep = Color(0xFFEE9008)
val MarkIceTop = Color(0xFFFFFFFF)
val MarkIce = Color(0xFFDDECFF)
val MarkIceDeep = Color(0xFF9BC1F7)
val MarkOutline = Color(0xFF7C3C10)
val MarkExtrude = Color(0xFF52270A)

/** Glossy arrow cubes: top facet, face, and the shaded side underneath it. */
val TileCyanLight = Color(0xFF8CEBFF)
val TileCyan = Color(0xFF15BFEC)
val TileCyanDeep = Color(0xFF0789C4)
val TileGreenLight = Color(0xFF8BF78A)
val TileGreen = Color(0xFF2FC44E)
val TileGreenDeep = Color(0xFF11842B)
val TileRedLight = Color(0xFFFFA797)
val TileRed = Color(0xFFF0402F)
val TileRedDeep = Color(0xFFA31910)
val TileYellowLight = Color(0xFFFFE48F)
val TileYellow = Color(0xFFFFB627)
val TileYellowDeep = Color(0xFFC5760A)
val TileAzureLight = Color(0xFF9FD4FF)
val TileAzure = Color(0xFF2E86F0)
val TileAzureDeep = Color(0xFF1455B0)
val TileVioletLight = Color(0xFFCBAAFF)
val TileViolet = Color(0xFF8B4CF0)
val TileVioletDeep = Color(0xFF5A21A8)

/**
 * The carved slab the brand sits on at launch: the sunlit top of the wood, the
 * face of it, and the two darker steps the bevel is cut back to. The plaque is
 * the only brown mass in the game, so it reads as its own object against the
 * sky rather than as part of the wordmark.
 */
val PlankTop = Color(0xFFC79257)
val PlankFace = Color(0xFF9A6634)
val PlankDeep = Color(0xFF6B4020)
val PlankEdge = Color(0xFF3F2310)

/** The plain cubes of the menu board: lit top, face, shaded side. */
val CubeTop = Color(0xFFF4F7FF)
val CubeFace = Color(0xFFC8D2F3)
val CubeSide = Color(0xFF97A3D5)

/** Planting along the bottom of the menu field and behind the wordmark. */
val LeafDeep = Color(0xFF12401F)
val LeafMid = Color(0xFF2E7A3A)
val LeafLight = Color(0xFF63B85C)

/** Menu text: pale on the dark field, with a darker pair for the light screens. */
val MenuText = Color(0xFFFFFFFF)
val MenuTextDim = Color(0xFFC3D0FF)
val TealInk = Color(0xFF0F3B45)
val TealMuted = Color(0xFF5F828A)

/** Glass plates, their rim, and the glow the settings disc sits in. */
val GlassTop = Color(0x8FDCE9FF)
val GlassMid = Color(0x4A7FA8F0)
val GlassDeep = Color(0x662B3E96)
val GlassRim = Color(0xB8E8F2FF)
val DiscLight = Color(0xFF63C6FF)
val DiscFace = Color(0xFF1E6BE0)
val DiscDeep = Color(0xFF0B3C9B)

/** Primary action gradient and its 3D underside. */
val PlayTop = Color(0xFFFFC93B)
val PlayMid = Color(0xFFFF8A12)
val PlayBottom = Color(0xFFFF5E00)
val PlayEdge = Color(0xFFB83600)
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
    background = MenuSkyTop,
    onBackground = MenuText,
    surface = CardSurface,
    onSurface = TealInk,
    surfaceVariant = MenuSkyDeep,
    onSurfaceVariant = TealMuted,
    outline = TealInk.copy(alpha = 0.18f)
)

/**
 * The type scale.
 *
 * A composable rather than a val because the families are locale-aware: [UiSans]
 * and [DisplaySerif] resolve to the system family on a Japanese or Korean
 * device, which the theme has to see before it hands a Typography to
 * MaterialTheme. This is also why `ArrowPuzzleTheme` is composed inside
 * `LocalizedApp` rather than around it.
 */
@Composable
private fun arrowTypography(): Typography {
    val sans = UiSans
    val serif = DisplaySerif
    return Typography(
        displaySmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = 2.sp),
        headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = 1.sp),
        titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 16.sp),
        labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    )
}

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
        typography = arrowTypography(),
        shapes = ArrowShapes,
        content = content
    )
}
