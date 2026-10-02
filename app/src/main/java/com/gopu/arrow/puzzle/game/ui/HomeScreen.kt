package com.gopu.arrow.puzzle.game.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ads.AdHost
import com.gopu.arrow.puzzle.game.ads.BannerAdSlot
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.ui.components.ArrowBoardScene
import com.gopu.arrow.puzzle.game.ui.components.FloatingArrowTile
import com.gopu.arrow.puzzle.game.ui.components.GlassStatCard
import com.gopu.arrow.puzzle.game.ui.components.GlossyIconButton
import com.gopu.arrow.puzzle.game.ui.components.HomeBackdrop
import com.gopu.arrow.puzzle.game.ui.components.MarkGlow
import com.gopu.arrow.puzzle.game.ui.components.PlayButton
import com.gopu.arrow.puzzle.game.ui.components.SkinCyan
import com.gopu.arrow.puzzle.game.ui.components.SkinGreen
import com.gopu.arrow.puzzle.game.ui.components.SkinRed
import com.gopu.arrow.puzzle.game.ui.components.StatBadge
import com.gopu.arrow.puzzle.game.ui.components.Wordmark
import com.gopu.arrow.puzzle.game.ui.theme.ArrowPuzzleTheme
import kotlin.math.roundToInt

/**
 * The proportions of the brand block, all as fractions so the composition is the
 * same on every screen it is laid out on.
 *
 * [MarkHeightEm] is how tall the two-line wordmark is in em, which is what the
 * block is measured against; the rest are shares of the block's own width. The
 * title above the mark is the one thing that has to stay clear of it, so its
 * clearance is a share of the mark rather than a fixed gap.
 */
private const val MarkHeightEm = 1.70f
private const val TopTileShare = 0.22f
private const val SideTileShare = 0.20f
private const val TileClearance = 0.05f

/**
 * Main menu.
 *
 * Reading order is deliberate: the wordmark and the three arrow cubes that orbit
 * it, the board they are a sample of, the two numbers, and then the action, with
 * settings last in the corner. The block sizes come off the content width and
 * everything flexible is weighted, so the composition holds from a short 4:5
 * phone to a tall 21:9 one and the action button always sits inside the safe
 * area above the gesture bar.
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
        val gutter = 20.dp
        val content = maxWidth - gutter * 2

        /*
         * PUZZLE is the widest line of the wordmark and runs at roughly four and
         * a half em, so the display size follows the available width: the mark
         * comes out about 0.6 of the content width, which is what leaves room
         * for the two cubes either side of it without either one touching a
         * letter.
         */
        val titleSize = minOf(56, (content / 7.4f).value.roundToInt()).coerceAtLeast(28)

        HomeBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = gutter, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.weight(0.30f))

            WordmarkBlock(fontSize = titleSize)

            Spacer(Modifier.weight(0.30f))

            ArrowBoardScene(modifier = Modifier.fillMaxWidth().weight(1.15f))

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassStatCard(
                    label = "LEVEL",
                    value = highestUnlocked.toString(),
                    badge = StatBadge.CROWN,
                    modifier = Modifier.weight(1f)
                )
                GlassStatCard(
                    label = "STARS",
                    value = totalStars.toString(),
                    badge = StatBadge.STAR,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            BannerAdSlot(adHost = ads)

            PlayButton(onClick = onPlay)

            Spacer(Modifier.height(10.dp))
        }

        GlossyIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp)
        )
    }
}

/**
 * The brand block: the logotype with a halo and planting behind it, and three
 * arrow cubes floating around it.
 *
 * Every position is computed from the mark's own measured height and the block's
 * own width rather than from a ratio against a fixed box, which is what keeps
 * the cube above the title from ever landing on top of the letters. Nothing in
 * the block is laid out by its own size either, so a cube can never grow the
 * block and shove the mark off centre.
 */
@Composable
private fun WordmarkBlock(fontSize: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth
        val density = LocalDensity.current

        // The two lines of the mark, in em, and the em as laid out on this device.
        val markHeight = with(density) { (fontSize * MarkHeightEm).sp.toDp() }
        val topTile = width * TopTileShare
        val sideTile = width * SideTileShare

        // The cube above the title clears it by a slice of the mark's own height,
        // so the overlap holds at any font scale.
        val markTop = topTile + markHeight * TileClearance
        val markMiddle = markTop + markHeight / 2f
        val redDrop = width * 0.07f

        // Tall enough to hold the lowest cube as well as the mark, so nothing
        // in the block can spill out of it.
        val blockHeight = maxOf(markTop + markHeight, markMiddle + sideTile / 2f + redDrop)

        Box(
            modifier = Modifier.size(width, blockHeight)
        ) {
            MarkGlow(Modifier.matchParentSize())

            Wordmark(
                fontSize = fontSize,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (width - width * 0.88f) / 2f, y = markTop)
                    .rotate(-3f)
                    .size(width * 0.88f, markHeight)
            )

            FloatingArrowTile(
                direction = Direction.UP,
                skin = SkinCyan,
                tilt = -7f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (width - topTile) / 2f)
                    .size(topTile)
            )

            FloatingArrowTile(
                direction = Direction.RIGHT,
                skin = SkinGreen,
                tilt = 10f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = width * 0.005f, y = markMiddle - sideTile / 2f)
                    .size(sideTile)
            )

            FloatingArrowTile(
                direction = Direction.DOWN,
                skin = SkinRed,
                tilt = -13f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = width - sideTile - width * 0.005f, y = markMiddle + redDrop)
                    .size(sideTile)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    ArrowPuzzleTheme {
        HomeScreen(progressRepository = ProgressRepository(LocalContext.current), onPlay = {}, onSettings = {})
    }
}
