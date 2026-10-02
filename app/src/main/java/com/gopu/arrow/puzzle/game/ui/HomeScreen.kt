package com.gopu.arrow.puzzle.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.ads.AdHost
import com.gopu.arrow.puzzle.game.ads.BannerAdSlot
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.ui.components.GlassIconButton
import com.gopu.arrow.puzzle.game.ui.components.HeroArrow
import com.gopu.arrow.puzzle.game.ui.components.PlayButton
import com.gopu.arrow.puzzle.game.ui.components.ProgressCard
import com.gopu.arrow.puzzle.game.ui.components.Wordmark
import com.gopu.arrow.puzzle.game.ui.theme.ArrowPuzzleTheme
import com.gopu.arrow.puzzle.game.ui.theme.Ivory
import com.gopu.arrow.puzzle.game.ui.theme.IvoryBottom
import com.gopu.arrow.puzzle.game.ui.theme.IvoryTop
import com.gopu.arrow.puzzle.game.ui.theme.TealInk
import com.gopu.arrow.puzzle.game.ui.theme.UiSans
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Main menu.
 *
 * Reading order is deliberate: wordmark, hero arrow, primary action, then
 * progress, with settings last. The column is laid out with weighted gaps so
 * the composition stays balanced from a short 4:5 phone to a tall 21:9 one, and
 * the action button always sits inside the safe area above the gesture bar.
 */
@Composable
fun HomeScreen(
    progressRepository: ProgressRepository,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    ads: AdHost? = null
) {
    val highestUnlocked by progressRepository.highestUnlockedLevel.collectAsState(initial = 1)
    val bestStars by progressRepository.bestStars.collectAsState(initial = emptyMap())
    val totalStars = remember(bestStars) { bestStars.values.sum() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val gutter = 28.dp
        val content = maxWidth - gutter * 2

        /*
         * PUZZLE is the widest line of the wordmark and runs at roughly five
         * em, so the display size follows the available width instead of
         * wrapping or clipping on a narrow phone.
         */
        val titleSize = minOf(54, (content / 5.4f).value.roundToInt()).coerceAtLeast(34)
        val heroSize = minOf(212.dp, content * 0.70f)

        MenuBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = gutter, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Spacer(Modifier.weight(0.75f))

            Wordmark(fontSize = titleSize)

            Spacer(Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .size(heroSize)
                    .semantics { contentDescription = "Play. Launch the next arrow." }
                    .clickable(onClickLabel = "Play", onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                HeroArrow(modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "TAP TO PLAY",
                color = TealInk,
                fontFamily = UiSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 3.4.sp
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProgressCard(
                    label = "LEVEL",
                    value = highestUnlocked.toString(),
                    modifier = Modifier.weight(1f)
                )
                ProgressCard(
                    label = "STARS",
                    value = totalStars.toString(),
                    modifier = Modifier.weight(1f),
                    showStar = true
                )
            }

            Spacer(Modifier.height(20.dp))

            BannerAdSlot(adHost = ads)

            PlayButton(onClick = onPlay)

            Spacer(Modifier.height(8.dp))
        }

        GlassIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        )
    }
}

/**
 * Warm ivory field with a soft light behind the hero and an almost invisible
 * diamond lattice, so the flat background still has depth without pulling
 * attention from the arrow.
 */
@Composable
private fun MenuBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.verticalGradient(listOf(IvoryTop, Ivory, IvoryBottom))
        )

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.60f),
                    0.6f to Color.White.copy(alpha = 0.18f),
                    1f to Color.Transparent
                ),
                center = Offset(width * 0.5f, height * 0.44f),
                radius = width * 0.92f
            ),
            radius = width * 0.92f,
            center = Offset(width * 0.5f, height * 0.44f)
        )

        val step = 46.dp.toPx()
        val lattice = TealInk.copy(alpha = 0.028f)
        val columns = (width / step).toInt() + 2
        for (index in -1..columns) {
            val x = index * step
            drawLine(lattice, Offset(x, 0f), Offset(x + height, height), 1f)
            drawLine(lattice, Offset(x, 0f), Offset(x - height, height), 1f)
        }

        val outline = TealInk.copy(alpha = 0.05f)
        drawDiamond(Offset(width * 0.16f, height * 0.30f), width * 0.30f, outline)
        drawDiamond(Offset(width * 0.88f, height * 0.74f), width * 0.26f, outline)
    }
}

private fun DrawScope.drawDiamond(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.58f, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.58f, center.y)
        close()
    }
    drawPath(path = path, color = color, style = Stroke(width = 1.2.dp.toPx()))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    ArrowPuzzleTheme {
        HomeScreen(progressRepository = ProgressRepository(LocalContext.current), onPlay = {}, onSettings = {})
    }
}