package com.gopu.arrow.puzzle.game.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.ads.AdHost
import com.gopu.arrow.puzzle.game.ads.BannerAdSlot
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.levels.LEVELS_PER_PACK
import com.gopu.arrow.puzzle.game.ui.theme.AccentAzure
import com.gopu.arrow.puzzle.game.ui.theme.AccentViolet
import com.gopu.arrow.puzzle.game.ui.theme.BackdropBottom
import com.gopu.arrow.puzzle.game.ui.theme.BackdropMid
import com.gopu.arrow.puzzle.game.ui.theme.BackdropTop
import com.gopu.arrow.puzzle.game.ui.theme.LevelCardPalettes
import com.gopu.arrow.puzzle.game.ui.theme.LockedGlassBottom
import com.gopu.arrow.puzzle.game.ui.theme.LockedGlassTop
import com.gopu.arrow.puzzle.game.ui.theme.NavyInk
import com.gopu.arrow.puzzle.game.ui.theme.NavyMuted
import com.gopu.arrow.puzzle.game.ui.theme.ProgressGreen
import com.gopu.arrow.puzzle.game.ui.theme.ProgressGreenSoft
import com.gopu.arrow.puzzle.game.ui.theme.StarGold
import com.gopu.arrow.puzzle.game.ui.theme.UiSans
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

data class PackInfo(
    val packNumber: Int,
    val levelCount: Int,
    val levels: List<PuzzleLevel>
)

private val CardShape = RoundedCornerShape(22.dp)
private val PlateShape = RoundedCornerShape(26.dp)
private val SelectorShape = RoundedCornerShape(28.dp)
private val ButtonShape = RoundedCornerShape(17.dp)

/** Soft frosted plate: white with a touch of transparency so the page shows through. */
private val GlassFill = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.94f),
    1f to Color.White.copy(alpha = 0.72f)
)

/** Hairline that is brightest along the top edge, the way glass catches light. */
private fun glassBorder(accent: Color = Color.White): Brush = Brush.verticalGradient(
    0f to accent.copy(alpha = 0.95f),
    0.5f to accent.copy(alpha = 0.20f),
    1f to accent.copy(alpha = 0.55f)
)

/** Very low, very soft drop shadow tinted with the navy ink rather than black. */
private val PlateAmbient = NavyInk.copy(alpha = 0.10f)
private val PlateSpot = NavyInk.copy(alpha = 0.16f)

/**
 * A quick scale-down that springs back. Paired with a ripple-free clickable so
 * the motion is the only press feedback and the card never looks busy.
 */
@Composable
private fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.955f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Top sheen plus a hairline inner border. Every frosted plate on this screen
 * ends with this, which is what makes the cards read as one material. The
 * sheen is kept light: anything heavier bleaches the navy type underneath it.
 */
@Composable
private fun BoxScope.glassChrome(
    shape: Shape,
    borderBrush: Brush = glassBorder(),
    borderWidth: Dp = 1.dp
) {
    Box(
        modifier = Modifier.matchParentSize().background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.28f),
                0.42f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.10f)
            )
        )
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .border(width = borderWidth, brush = borderBrush, shape = shape)
    )
}

@Composable
fun LevelSelectScreen(
    packs: List<PackInfo>,
    progressRepository: ProgressRepository,
    onLevelClick: (PuzzleLevel) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    ads: AdHost? = null
) {
    val highestUnlocked by progressRepository.highestUnlockedLevel.collectAsState(initial = 1)
    val bestStars by progressRepository.bestStars.collectAsState(initial = emptyMap())
    val totalStars = remember(packs, bestStars) {
        packs.sumOf { pack -> pack.levels.sumOf { bestStars[it.id] ?: 0 } }
    }

    val pagerState = rememberPagerState(pageCount = { packs.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LevelSelectBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LevelSelectHeader(
                totalStars = totalStars,
                onBack = onBack,
                onSettings = onSettings
            )

            Spacer(Modifier.height(14.dp))

            if (packs.isEmpty()) return@Column

            PackSelector(
                current = pagerState.currentPage,
                total = packs.size,
                onPrevious = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } },
                onNext = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(packs.size - 1)) } }
            )

            Spacer(Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pack = packs[page]
                // Distance from the settled page, used to fade and settle the
                // incoming pack instead of letting it slide in flat.
                val distance = (
                    abs(pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, 1f)
                    )
                val settleAlpha by animateFloatAsState(
                    targetValue = 1f - distance * 0.6f,
                    animationSpec = tween(220),
                    label = "packAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = settleAlpha
                            val scale = 1f - distance * 0.04f
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    if (pack.levels.isEmpty()) {
                        ComingSoonPack(pack.packNumber)
                    } else {
                        PackPage(
                            pack = pack,
                            highestUnlocked = highestUnlocked,
                            bestStars = bestStars,
                            onLevelClick = onLevelClick
                        )
                    }
                }
            }

            BannerAdSlot(adHost = ads)
        }
    }
}

/**
 * The page field. A vertical wash plus a handful of very soft pastel radial
 * blobs that stand in for frosted scenery without any bitmap.
 */
@Composable
private fun LevelSelectBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val unit = minOf(width, height)

        drawRect(
            brush = Brush.verticalGradient(
                0f to BackdropTop,
                0.55f to BackdropMid,
                1f to BackdropBottom
            )
        )

        val blobs = listOf(
            Triple(Offset(width * 0.10f, height * 0.08f), unit * 0.46f, LevelCardPalettes[0][1]),
            Triple(Offset(width * 0.95f, height * 0.22f), unit * 0.40f, LevelCardPalettes[3][1]),
            Triple(Offset(width * 0.14f, height * 0.55f), unit * 0.38f, LevelCardPalettes[4][1]),
            Triple(Offset(width * 0.92f, height * 0.80f), unit * 0.44f, LevelCardPalettes[1][1]),
            Triple(Offset(width * 0.40f, height * 1.02f), unit * 0.34f, LevelCardPalettes[5][1])
        )
        blobs.forEach { (center, radius, color) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to color.copy(alpha = 0.50f),
                        0.55f to color.copy(alpha = 0.18f),
                        1f to Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

/**
 * Back arrow, two-tone wordmark and the star/settings cluster. The title keeps
 * its own weight rather than being laid out by `SpaceBetween`, which keeps it
 * optically centred no matter how wide the star counter grows.
 */
@Composable
private fun LevelSelectHeader(
    totalStars: Int,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack
        )
        Spacer(Modifier.weight(1f))
        ScreenTitle()
        Spacer(Modifier.weight(1f))
        StarCounter(stars = totalStars)
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onSettings
        )
    }
}

@Composable
private fun ScreenTitle(modifier: Modifier = Modifier) {
    val title = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(color = NavyInk)) { append("SELECT ") }
            withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(AccentViolet, AccentAzure)))) {
                append("LEVEL")
            }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = NavyInk,
            fontFamily = UiSans,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(74.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.28f to AccentViolet,
                        0.72f to AccentAzure,
                        1f to Color.Transparent
                    )
                )
        )
    }
}

@Composable
private fun StarCounter(stars: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(46.dp)
            .shadow(8.dp, ButtonShape, ambientColor = PlateAmbient, spotColor = PlateSpot)
            .clip(ButtonShape)
            .background(GlassFill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = StarGold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$stars",
            color = NavyInk,
            fontFamily = UiSans,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/** Frosted round-cornered icon button: white plate, hairline, soft shadow. */
@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NavyInk,
    size: Dp = 46.dp,
    enabled: Boolean = true,
    circular: Boolean = false
) {
    val shape = if (circular) CircleShape else ButtonShape
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .pressScale(interactionSource, pressedScale = 0.93f)
            .shadow(8.dp, shape, ambientColor = PlateAmbient, spotColor = PlateSpot)
            .clip(shape)
            .background(GlassFill)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else NavyMuted.copy(alpha = 0.42f),
            modifier = Modifier.size(size * 0.46f)
        )
        glassChrome(shape)
    }
}

/**
 * Pack navigation: a frosted pill with two arrows and a row of dots. The dots
 * widen for the selected pack, which is the clearest cheap selected-state cue.
 */
@Composable
private fun PackSelector(
    current: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, SelectorShape, ambientColor = PlateAmbient, spotColor = PlateSpot)
            .clip(SelectorShape)
            .background(GlassFill)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous pack",
                onClick = onPrevious,
                size = 40.dp,
                enabled = current > 0
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PACK ${current + 1} / $total",
                    color = NavyInk,
                    fontFamily = UiSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(total) { index ->
                        PackDot(selected = index == current)
                    }
                }
            }
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next pack",
                onClick = onNext,
                size = 40.dp,
                enabled = current < total - 1
            )
        }
        glassChrome(SelectorShape)
    }
}

@Composable
private fun PackDot(selected: Boolean) {
    val width by animateDpAsState(
        targetValue = if (selected) 20.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "packDot"
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(7.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(AccentViolet, AccentAzure))
                } else {
                    Brush.horizontalGradient(
                        listOf(NavyMuted.copy(alpha = 0.26f), NavyMuted.copy(alpha = 0.26f))
                    )
                }
            )
    )
}

/**
 * One pack: a compact progress card plus the level grid. Both are measured
 * against the space that is actually left, so ten levels always fit without
 * scrolling and never leave a band of dead space.
 */
@Composable
private fun PackPage(
    pack: PackInfo,
    highestUnlocked: Int,
    bestStars: Map<String, Int>,
    onLevelClick: (PuzzleLevel) -> Unit
) {
    val cleared = remember(pack, bestStars) {
        pack.levels.count { (bestStars[it.id] ?: 0) > 0 }
    }
    val collectedStars = remember(pack, bestStars) {
        pack.levels.sumOf { bestStars[it.id] ?: 0 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PackProgressCard(
            packNumber = pack.packNumber,
            cleared = cleared,
            total = pack.levels.size,
            stars = collectedStars
        )
        Spacer(Modifier.height(14.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val columns = 3
            val rows = ceil(pack.levels.size / columns.toFloat()).toInt().coerceAtLeast(1)
            val gap = 12.dp
            val maxCellWidth = (maxWidth - gap * (columns - 1)) / columns
            val maxCellHeight = (maxHeight - gap * (rows - 1)) / rows

            // Fit the whole pack when there is room; otherwise fall back to a
            // wider-than-tall card and let the grid scroll.
            val cellHeight = if (maxCellHeight >= 56.dp) {
                minOf(maxCellHeight, maxCellWidth * 1.22f)
            } else {
                maxCellWidth / 1.25f
            }
            val cellWidth = minOf(maxCellWidth, cellHeight * 1.3f)

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                items(pack.levels) { level ->
                    val globalOrder = (pack.packNumber - 1) * LEVELS_PER_PACK + level.order
                    val isUnlocked = globalOrder <= highestUnlocked
                    val stars = bestStars[level.id] ?: 0
                    LevelCard(
                        order = level.order,
                        isUnlocked = isUnlocked,
                        isCurrent = globalOrder == highestUnlocked,
                        stars = stars,
                        width = cellWidth,
                        height = cellHeight,
                        onClick = { if (isUnlocked) onLevelClick(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackProgressCard(
    packNumber: Int,
    cleared: Int,
    total: Int,
    stars: Int
) {
    val ratio by animateFloatAsState(
        targetValue = if (total == 0) 0f else cleared.toFloat() / total,
        animationSpec = tween(450),
        label = "packProgress"
    )
    val percent = if (total == 0) 0 else (cleared * 100f / total).roundToInt()
    val complete = total > 0 && cleared >= total

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, PlateShape, ambientColor = PlateAmbient, spotColor = PlateSpot)
            .clip(PlateShape)
            .background(GlassFill)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(listOf(AccentViolet, AccentAzure))
                        )
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PACK $packNumber",
                        color = NavyInk,
                        fontFamily = UiSans,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "$cleared of $total cleared",
                        color = NavyMuted,
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StarGold,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$stars",
                    color = NavyInk,
                    fontFamily = UiSans,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassProgressBar(
                    progress = ratio,
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$percent%",
                    color = if (complete) ProgressGreen else NavyMuted,
                    fontFamily = UiSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        glassChrome(PlateShape)
    }
}

/** Rounded track with a mint-to-green fill and a thin sheen along the top. */
@Composable
private fun GlassProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val height = size.height
        val radius = CornerRadius(height / 2f, height / 2f)
        drawRoundRect(color = NavyMuted.copy(alpha = 0.12f), cornerRadius = radius)

        val filled = size.width * progress.coerceIn(0f, 1f)
        if (filled > 0.5f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(ProgressGreenSoft, ProgressGreen)),
                size = Size(filled, height),
                cornerRadius = radius
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.42f),
                topLeft = Offset(0f, height * 0.14f),
                size = Size(filled, height * 0.34f),
                cornerRadius = CornerRadius(height * 0.17f, height * 0.17f)
            )
        }
    }
}

/**
 * One level. The card is a soft pastel gradient with a top sheen, a small gloss
 * in the corner and a hairline border; only the level the player is up to gets
 * an extra border and a warmer shadow, so the grid never looks noisy.
 */
@Composable
private fun LevelCard(
    order: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    val palette = LevelCardPalettes[(order - 1).mod(LevelCardPalettes.size)]
    val top = if (isUnlocked) palette[0] else LockedGlassTop
    val bottom = if (isUnlocked) palette[1] else LockedGlassBottom
    val interactionSource = remember { MutableInteractionSource() }

    val numberSize = (height.value * 0.34f).coerceIn(24f, 42f).sp
    val starSize = (height.value * 0.16f).coerceIn(13f, 21f).dp
    val glossSize = (height.value * 0.26f).coerceIn(14f, 30f).dp

    Box(
        modifier = Modifier
            .size(width, height)
            .pressScale(interactionSource)
            .shadow(
                elevation = when {
                    !isUnlocked -> 0.dp
                    isCurrent -> 14.dp
                    else -> 7.dp
                },
                shape = CardShape,
                ambientColor = PlateAmbient,
                spotColor = if (isCurrent) palette[1].copy(alpha = 0.55f) else PlateSpot
            )
            .clip(CardShape)
            .background(Brush.verticalGradient(0f to top, 1f to bottom))
            .clickable(
                enabled = isUnlocked,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = if (isUnlocked) {
                    "Level $order, $stars of 3 stars"
                } else {
                    "Level $order, locked"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isUnlocked) {
            // Small glossy highlight. The gradient fades out exactly on the
            // shape edge so it reads as a light catch, never as a scratch.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = height * 0.10f, top = height * 0.08f)
                    .size(width = glossSize, height = glossSize * 0.7f)
                    .background(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.80f),
                                0.45f to Color.White.copy(alpha = 0.34f),
                                1f to Color.Transparent
                            ),
                            center = Offset(glossSize.value / 2f, glossSize.value * 0.35f),
                            radius = glossSize.value / 2f
                        ),
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        if (isUnlocked) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = order.toString(),
                    color = NavyInk,
                    fontFamily = UiSans,
                    fontSize = numberSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Spacer(Modifier.height(height * 0.05f))
                Row(horizontalArrangement = Arrangement.spacedBy(starSize * 0.16f)) {
                    repeat(3) { index ->
                        Icon(
                            imageVector = if (index < stars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (index < stars) StarGold else NavyMuted.copy(alpha = 0.30f),
                            modifier = Modifier.size(starSize)
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = NavyMuted.copy(alpha = 0.55f),
                    modifier = Modifier.size(height * 0.28f)
                )
                Spacer(Modifier.height(height * 0.05f))
                Text(
                    text = order.toString(),
                    color = NavyMuted.copy(alpha = 0.60f),
                    fontFamily = UiSans,
                    fontSize = numberSize * 0.5f,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        glassChrome(
            shape = CardShape,
            borderBrush = if (isCurrent && isUnlocked) {
                Brush.linearGradient(listOf(AccentAzure, AccentViolet, AccentAzure))
            } else {
                glassBorder()
            },
            borderWidth = if (isCurrent && isUnlocked) 2.dp else 1.dp
        )
    }
}

@Composable
private fun ComingSoonPack(packNumber: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, PlateShape, ambientColor = PlateAmbient, spotColor = PlateSpot)
            .clip(PlateShape)
            .background(GlassFill)
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NavyMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "PACK $packNumber",
                color = NavyInk,
                fontFamily = UiSans,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "More puzzles are on the way",
                color = NavyMuted,
                fontFamily = UiSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        glassChrome(PlateShape)
    }
}