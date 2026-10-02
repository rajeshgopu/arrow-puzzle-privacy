package com.gopu.arrow.puzzle.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.components.BrandPlaque
import com.gopu.arrow.puzzle.game.ui.components.FloatingArrowTile
import com.gopu.arrow.puzzle.game.ui.components.GemBlue
import com.gopu.arrow.puzzle.game.ui.components.GemGold
import com.gopu.arrow.puzzle.game.ui.components.SkinGreen
import com.gopu.arrow.puzzle.game.ui.components.SkinRed
import com.gopu.arrow.puzzle.game.ui.components.SkinViolet
import com.gopu.arrow.puzzle.game.ui.components.SkinYellow
import com.gopu.arrow.puzzle.game.ui.components.SplashArrow
import com.gopu.arrow.puzzle.game.ui.components.SplashGem
import com.gopu.arrow.puzzle.game.ui.components.SplashLoader
import com.gopu.arrow.puzzle.game.ui.components.SplashSky

/**
 * The splash, drawn as an overlay on top of the menu that is already composed
 * behind it rather than as a screen that gets swapped for one.
 *
 * The field is the menu's own sky, painted at full strength from the very first
 * frame, so the launch never flashes a different colour on its way in and the
 * cross-fade on its way out has nothing to change. Everything else is the launch
 * scene: the hero arrow standing in its own light, four glossy cubes thrown
 * around it, a scatter of stones, the brand on its wooden plaque under a crown,
 * and a loading bar under all of it.
 *
 * It owns no timing of its own. [intro] hands each piece of the scene its own
 * slice of the entrance and [reveal] takes the whole scene away at the handoff,
 * and every clock is only ever read inside a `graphicsLayer` block, so a frame
 * of the launch repaints its layers and recomposes nothing.
 */
@Composable
fun SplashOverlay(
    intro: Animatable<Float, AnimationVector1D>,
    reveal: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = splashFieldAlphaAt(reveal.value) }
        ) {
            SplashSky(Modifier.matchParentSize())
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .swallowsTouches()
                .graphicsLayer {
                    alpha = splashMarkAlphaAt(intro.value, reveal.value)
                    val scale = splashMarkScaleAt(intro.value, reveal.value)
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SplashGutter, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))
                Spacer(Modifier.weight(SplashSkyShare))

                SplashStage(intro = intro, modifier = Modifier.fillMaxWidth().weight(1f))

                Spacer(Modifier.height(10.dp))

                SplashPiece(
                    intro = intro,
                    start = SplashPlaqueStart,
                    from = 0.82f,
                    pop = 0.05f,
                    travelY = 46.dp
                ) {
                    BrandPlaque(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(SplashPlaqueRatio)
                    )
                }

                Spacer(Modifier.weight(SplashLoaderShare))

                SplashPiece(intro = intro, start = SplashLoaderStart, from = 0.70f) {
                    SplashLoader(
                        modifier = Modifier
                            .fillMaxWidth(0.54f)
                            .height(11.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/**
 * The part of the launch that floats: the hero arrow, the cubes around it and
 * the stones wedged between them.
 *
 * Every size is a fraction of the stage it is laid out on, and every piece is
 * placed by offset from the top left rather than by alignment, so the scatter
 * holds its shape from a short phone to a tall one and the arrow stays the
 * subject of the screen on both.
 */
@Composable
private fun SplashStage(
    intro: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val width = maxWidth
        val height = maxHeight
        val arrowBox = minOf(width * SplashArrowShare, height * SplashArrowHeightShare)
        val cube = width * SplashCubeShare
        val gem = width * SplashGemShare

        // Stones first, so the cubes and the arrow pass in front of them.
        SplashPiece(intro = intro, start = SplashGemStart, from = 0.30f, travelY = -40.dp) {
            Box(Modifier.matchParentSize()) {
                SplashGem(
                    skin = GemGold,
                    spin = 0.10f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = width * 0.05f, y = height * 0.24f)
                        .size(gem)
                )
                SplashGem(
                    skin = GemBlue,
                    spin = 0.55f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = width * 0.30f, y = height * 0.05f)
                        .size(gem * 0.86f)
                )
                SplashGem(
                    skin = GemBlue,
                    spin = 0.80f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = width * 0.85f, y = height * 0.29f)
                        .size(gem)
                )
                SplashGem(
                    skin = GemGold,
                    spin = 0.35f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = width * 0.66f, y = height * 0.86f)
                        .size(gem * 0.86f)
                )
            }
        }

        SplashPiece(
            intro = intro,
            start = SplashCubeStarts[2],
            modifier = Modifier.offset(x = width * 0.01f, y = height * 0.66f),
            from = 0.55f,
            pop = 0.10f,
            travelX = -width * 0.24f,
            travelY = height * 0.20f,
            turn = 16f
        ) {
            FloatingArrowTile(
                direction = Direction.UP,
                skin = SkinViolet,
                tilt = -14f,
                phase = 0.15f,
                modifier = Modifier.size(cube)
            )
        }

        SplashPiece(
            intro = intro,
            start = SplashCubeStarts[0],
            modifier = Modifier.offset(x = width * 0.01f, y = height * 0.01f),
            from = 0.55f,
            pop = 0.10f,
            travelX = -width * 0.26f,
            travelY = -height * 0.24f,
            turn = -18f
        ) {
            FloatingArrowTile(
                direction = Direction.UP,
                skin = SkinGreen,
                tilt = -11f,
                phase = 0.00f,
                modifier = Modifier.size(cube)
            )
        }

        SplashPiece(
            intro = intro,
            start = SplashCubeStarts[1],
            modifier = Modifier.offset(x = width * 0.73f, y = height * 0.03f),
            from = 0.55f,
            pop = 0.10f,
            travelX = width * 0.26f,
            travelY = -height * 0.22f,
            turn = 18f
        ) {
            FloatingArrowTile(
                direction = Direction.DOWN,
                skin = SkinRed,
                tilt = 13f,
                phase = 0.42f,
                modifier = Modifier.size(cube * 0.94f)
            )
        }

        // The arrow takes the full width of the stage so its beams have somewhere
        // to run to; the glyph itself is sized inside its own canvas.
        SplashPiece(intro = intro, start = SplashArrowStart, from = 0.55f, pop = 0.14f, travelY = 42.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SplashArrow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(arrowBox)
                )
            }
        }

        // The last cube lands in front of the arrow, the way the reference badge
        // has its yellow tile sitting across the blue one's tail.
        SplashPiece(
            intro = intro,
            start = SplashCubeStarts[3],
            modifier = Modifier.offset(x = width * 0.15f, y = height * 0.44f),
            from = 0.55f,
            pop = 0.10f,
            travelX = -width * 0.28f,
            travelY = height * 0.10f,
            turn = -12f
        ) {
            FloatingArrowTile(
                direction = Direction.LEFT,
                skin = SkinYellow,
                tilt = 9f,
                phase = 0.68f,
                modifier = Modifier.size(cube * 0.80f)
            )
        }
    }
}

/**
 * One piece of the launch scene and its entrance.
 *
 * The piece is invisible until its own start, arrives from [travelX] / [travelY]
 * turned by [turn], and comes to rest on one scale after a single soft
 * overshoot. All of it is read inside the `graphicsLayer`, so the piece's own
 * animation never recomposes the scene it sits in.
 */
@Composable
private fun SplashPiece(
    intro: Animatable<Float, AnimationVector1D>,
    start: Float,
    modifier: Modifier = Modifier,
    from: Float = 0.7f,
    pop: Float = 0.08f,
    travelX: Dp = 0.dp,
    travelY: Dp = 0.dp,
    turn: Float = 0f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.graphicsLayer {
            alpha = splashPieceAlphaAt(intro.value, start)
            val scale = splashPieceScaleAt(intro.value, start, from, pop)
            scaleX = scale
            scaleY = scale
            val travel = splashPieceTravelAt(intro.value, start)
            translationX = travelX.toPx() * travel
            translationY = travelY.toPx() * travel
            rotationZ = splashPieceTurnAt(intro.value, start, turn)
        }
    ) {
        content()
    }
}

/**
 * Eats every touch that lands on the overlay while it is up, so a tap during the
 * hold or during the handoff cannot reach the menu waiting behind it. The
 * modifier leaves with the overlay, so the menu is fully interactive again the
 * moment the handoff ends.
 */
private fun Modifier.swallowsTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

/** Gutter either side of the launch, so no piece can touch the edge of the screen. */
private val SplashGutter = 14.dp

/** Height of the badge as a fraction of its own width. */
private const val SplashPlaqueRatio = 1.45f

/** The arrow's box as a fraction of the stage width, and of the stage height. */
private const val SplashArrowShare = 0.62f
private const val SplashArrowHeightShare = 0.60f

/** Cube and stone edges as fractions of the stage width. */
private const val SplashCubeShare = 0.25f
private const val SplashGemShare = 0.05f

/** Flexible space above the stage and between the plaque and the bar. */
private const val SplashSkyShare = 0.10f
private const val SplashLoaderShare = 0.18f
