package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.theme.LeafDeep
import com.gopu.arrow.puzzle.game.ui.theme.LeafLight
import com.gopu.arrow.puzzle.game.ui.theme.LeafMid
import com.gopu.arrow.puzzle.game.ui.theme.MarkExtrude
import com.gopu.arrow.puzzle.game.ui.theme.MarkGold
import com.gopu.arrow.puzzle.game.ui.theme.MarkGoldDeep
import com.gopu.arrow.puzzle.game.ui.theme.MarkGoldTop
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.PlankDeep
import com.gopu.arrow.puzzle.game.ui.theme.PlankEdge
import com.gopu.arrow.puzzle.game.ui.theme.PlankFace
import com.gopu.arrow.puzzle.game.ui.theme.PlankTop
import com.gopu.arrow.puzzle.game.ui.theme.PlayEdge
import com.gopu.arrow.puzzle.game.ui.theme.TileAzure
import com.gopu.arrow.puzzle.game.ui.theme.TileAzureDeep
import com.gopu.arrow.puzzle.game.ui.theme.TileAzureLight
import com.gopu.arrow.puzzle.game.ui.theme.TileCyanLight
import com.gopu.arrow.puzzle.game.ui.theme.TileYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The launch field.
 *
 * The sky underneath is the menu's own [HomeBackdrop], drawn here for the length
 * of the splash and nowhere else. Painting the same field twice is what lets the
 * handoff dissolve one layer into the other with no colour to move, and it is
 * why the launch can never flash a background the menu does not use.
 *
 * Everything on top of it is daylight that only exists here: a sun off the top
 * left corner, a bank of soft cloud and a warm haze along the horizon. It lights
 * the sky the way the reference splash is lit, and it is the one part of the
 * field the handoff genuinely has to give back.
 */
@Composable
fun SplashSky(modifier: Modifier = Modifier) {
    Box(modifier) {
        HomeBackdrop(Modifier.matchParentSize())

        val twinkle = rememberInfiniteTransition(label = "splashSky").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "sky"
        )
        val reduceMotion = rememberSystemReduceMotion()

        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val breath = if (reduceMotion) 0.5f else twinkle.value

            // Sun off the top left corner: the source every other light agrees with.
            val sun = Offset(width * 0.16f, -height * 0.04f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.34f),
                        0.45f to Color.White.copy(alpha = 0.14f),
                        1f to Color.Transparent
                    ),
                    center = sun,
                    radius = width * 1.20f
                ),
                radius = width * 1.20f,
                center = sun
            )

            // Cloud bank: overlapping soft lobes across the upper third, each on
            // its own offset, so the sky has weather in it without ever becoming
            // a hard shape.
            val lobes = listOf(
                Triple(0.10f, 0.09f, 0.26f),
                Triple(0.34f, 0.05f, 0.20f),
                Triple(0.62f, 0.11f, 0.24f),
                Triple(0.88f, 0.04f, 0.18f)
            )
            for ((index, lobe) in lobes.withIndex()) {
                val reach = lobe.third * width
                val center = Offset((lobe.first + 0.012f * breath) * width, lobe.second * height)

                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = 0.20f + 0.04f * breath),
                            0.62f to Color.White.copy(alpha = 0.09f),
                            1f to Color.Transparent
                        ),
                        center = center,
                        radius = reach
                    ),
                    radius = reach,
                    center = center
                )
                // A warm underside, which is what stops a white blob reading as fog.
                val warm = center.copy(y = center.y + reach * 0.30f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFFFFE0B4).copy(alpha = 0.14f),
                            1f to Color.Transparent
                        ),
                        center = warm,
                        radius = reach * 0.72f
                    ),
                    radius = reach * 0.72f,
                    center = warm
                )
                if (index % 2 == 0) {
                    drawSparkle(
                        center = Offset(center.x, center.y - reach * 0.55f),
                        radius = width * 0.012f,
                        color = Color.White.copy(alpha = 0.34f)
                    )
                }
            }

            // Warm haze along the horizon, where light has travelled furthest
            // through the sky and lost most of its blue.
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.38f to Color.Transparent,
                        0.66f to Color(0xFFFFD9A6).copy(alpha = 0.16f),
                        0.84f to Color(0xFFFFC98C).copy(alpha = 0.07f),
                        1f to Color.Transparent
                    )
                )
            )
        }
    }
}

/**
 * The hero of the launch: one oversized arrow standing on its own light, with a
 * fan of beams firing out from behind it.
 *
 * It is the same [chunkyArrow] the board tiles are stamped with, drawn large
 * enough to be the subject of the screen. Four passes give it the moulded look:
 * a cyan halo, a block of extruded blue dropped down and right, the lit face,
 * and a white wash clipped to the top of the shape so the highlight follows the
 * arrow rather than sitting on the screen behind it.
 */
@Composable
fun SplashArrow(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "splashArrow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val reduceMotion = rememberSystemReduceMotion()

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        // The glyph spans 0.82 of its box along the shaft and 0.80 across it, so
        // this is the box that fills the tighter of the two constraints.
        val box = min(size.width / 0.82f, size.height / 0.80f) * 0.94f
        val beat = 0.5f + 0.5f * sin((if (reduceMotion) 0.6f else pulse.value) * PI.toFloat())

        drawArrowBeams(center = center, box = box, reach = size.width, energy = beat)
        drawArrowHalo(center = center, box = box, energy = beat)

        val arrow = chunkyArrow(Direction.RIGHT, box, center)

        // The extruded edge first, so the lit face sits on top of its own shadow.
        translate(box * 0.045f, box * 0.055f) {
            drawPath(path = arrow, color = TileAzureDeep)
        }
        drawPath(
            path = arrow,
            brush = Brush.linearGradient(
                colors = listOf(TileAzureLight, TileAzure),
                start = Offset(center.x - box * 0.42f, center.y - box * 0.40f),
                end = Offset(center.x + box * 0.42f, center.y + box * 0.40f)
            )
        )

        clipPath(arrow) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.62f),
                        0.34f to Color.White.copy(alpha = 0.16f),
                        1f to Color.Transparent
                    ),
                    startY = center.y - box * 0.42f,
                    endY = center.y + box * 0.10f
                ),
                topLeft = Offset(center.x - box, center.y - box),
                size = Size(box * 2f, box * 2f)
            )
            // One wet highlight along the top of the shaft: the moulded plastic
            // the board tiles carry, at a size you can actually see it on.
            drawOval(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.55f),
                        1f to Color.Transparent
                    ),
                    center = Offset(center.x - box * 0.10f, center.y - box * 0.22f),
                    radius = box * 0.34f
                ),
                topLeft = Offset(center.x - box * 0.44f, center.y - box * 0.52f),
                size = Size(box * 0.68f, box * 0.40f)
            )
        }

        drawPath(
            path = arrow,
            color = Color.White.copy(alpha = 0.72f + 0.22f * beat),
            style = Stroke(width = box * 0.020f, join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
    }
}

/**
 * One streak of the fan firing out from behind the arrow: how far off the
 * arrow's axis it sits, how thick it is, where it sits in the breathing cycle,
 * and what colour it burns at its hot end.
 */
private class Beam(
    val offset: Float,
    val thickness: Float,
    val phase: Float,
    val color: Color
)

/** The five streaks, widest and brightest on the arrow's own axis. */
private val ArrowBeams = listOf(
    Beam(-0.26f, 0.030f, 0.00f, TileCyanLight),
    Beam(-0.13f, 0.055f, 0.34f, MarkGold),
    Beam(0.00f, 0.075f, 0.00f, Color.White),
    Beam(0.14f, 0.050f, 0.62f, TileYellow),
    Beam(0.28f, 0.028f, 0.28f, TileCyanLight)
)

/**
 * The light firing out from behind the arrow.
 *
 * Drawn before the arrow, so it reads as light leaving rather than light lying on
 * top: a bright core with a pair of gold and a pair of cyan streaks fanning out
 * of it, each on its own phase, and sparkles riding them.
 */
private fun DrawScope.drawArrowBeams(center: Offset, box: Float, reach: Float, energy: Float) {
    val tail = center.x - box * 0.44f
    val start = -reach * 0.06f
    if (tail <= start) return

    for (beam in ArrowBeams) {
        val strength = 0.30f + 0.70f * (0.5f + 0.5f * sin((energy + beam.phase) * PI.toFloat()))
        val y = center.y + box * beam.offset
        drawLine(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.55f to beam.color.copy(alpha = 0.28f * strength),
                    1f to beam.color.copy(alpha = 0.95f * strength)
                ),
                startX = start,
                endX = tail
            ),
            start = Offset(start, y),
            end = Offset(tail, y),
            strokeWidth = box * beam.thickness,
            cap = StrokeCap.Round
        )
    }

    for (index in 0 until 5) {
        val phase = energy + index * 0.19f
        val strength = 0.4f + 0.6f * (0.5f + 0.5f * sin(phase * PI.toFloat() * 1.6f))
        val x = start + (tail - start) * (0.30f + 0.16f * index)
        val y = center.y + sin(phase * PI.toFloat()) * box * 0.22f
        drawSparkle(
            center = Offset(x, y),
            radius = box * (0.020f + 0.012f * strength),
            color = Color.White.copy(alpha = 0.72f * strength)
        )
    }
}

/**
 * The arrow's own light: a wide cyan wash and three tight rims around the glyph,
 * all breathing, so the arrow sits in a pool of light instead of on the sky.
 */
private fun DrawScope.drawArrowHalo(center: Offset, box: Float, energy: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to NeonCyan.copy(alpha = 0.30f + 0.10f * energy),
                0.48f to NeonCyan.copy(alpha = 0.10f),
                1f to Color.Transparent
            ),
            center = center,
            radius = box * 0.92f
        ),
        radius = box * 0.92f,
        center = center
    )

    for (ring in 3 downTo 1) {
        drawPath(
            path = chunkyArrow(Direction.RIGHT, box * (1f + ring * 0.055f), center),
            color = NeonCyan.copy(alpha = (0.10f - ring * 0.02f) * (0.7f + 0.3f * energy))
        )
    }
}

/** The two stones the launch is scattered with: one cut from the sky, one from the sun. */
internal val GemBlue = TileSkin(Color(0xFFC6ECFF), Color(0xFF3FA9F5), Color(0xFF1350B0))
internal val GemGold = TileSkin(Color(0xFFFFE9AE), Color(0xFFFFB43C), Color(0xFFC4760F))

/**
 * A cut stone the size of a thumbnail, breathing and turning in place.
 *
 * Five passes make it a solid rather than a shape: a halo, the body, a lit top
 * facet, a shaded lower one and a white edge. [spin] is the stone's place in the
 * shared cycle, so a whole scatter of them can be thrown onto different phases
 * instead of pulsing as one block.
 */
@Composable
internal fun SplashGem(
    skin: TileSkin,
    modifier: Modifier = Modifier,
    spin: Float = 0f
) {
    val drift = rememberInfiniteTransition(label = "splashGem").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val reduceMotion = rememberSystemReduceMotion()

    Canvas(modifier) {
        val unit = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val phase = if (reduceMotion) 0.5f else drift.value
        val turn = (phase + spin) * PI.toFloat() * 2f
        val breath = 1f + 0.06f * sin(turn)

        val halfWidth = unit * 0.36f * breath
        val halfHeight = unit * 0.48f * breath
        val crown = Offset(center.x, center.y - halfHeight)
        val tableLeft = Offset(center.x - halfWidth, center.y - halfHeight * 0.14f)
        val tableRight = Offset(center.x + halfWidth, center.y - halfHeight * 0.14f)
        val foot = Offset(center.x, center.y + halfHeight)
        val table = Offset(center.x, center.y - halfHeight * 0.02f)

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to skin.light.copy(alpha = 0.34f),
                    1f to Color.Transparent
                ),
                center = center,
                radius = unit * 0.80f
            ),
            radius = unit * 0.80f,
            center = center
        )

        val body = Path().apply {
            moveTo(crown.x, crown.y)
            lineTo(tableRight.x, tableRight.y)
            lineTo(foot.x, foot.y)
            lineTo(tableLeft.x, tableLeft.y)
            close()
        }

        rotate(4f * sin(turn), center) {
            drawPath(
                path = body,
                brush = Brush.verticalGradient(
                    colors = listOf(skin.light, skin.face, skin.deep),
                    startY = center.y - halfHeight,
                    endY = center.y + halfHeight
                )
            )
            // Top facet, lit from above.
            drawPath(
                path = Path().apply {
                    moveTo(crown.x, crown.y)
                    lineTo(tableLeft.x, tableLeft.y)
                    lineTo(table.x, table.y)
                    lineTo(tableRight.x, tableRight.y)
                    close()
                },
                color = skin.light.copy(alpha = 0.92f)
            )
            // The two lower facets, each a different step darker than the body so
            // the stone reads as turning rather than as flat.
            drawPath(
                path = Path().apply {
                    moveTo(table.x, table.y)
                    lineTo(tableRight.x, tableRight.y)
                    lineTo(foot.x, foot.y)
                    close()
                },
                color = skin.deep.copy(alpha = 0.72f)
            )
            drawPath(
                path = Path().apply {
                    moveTo(table.x, table.y)
                    lineTo(tableLeft.x, tableLeft.y)
                    lineTo(foot.x, foot.y)
                    close()
                },
                color = skin.face.copy(alpha = 0.80f)
            )
            drawPath(
                path = body,
                color = Color.White.copy(alpha = 0.62f),
                style = Stroke(width = unit * 0.022f, join = StrokeJoin.Round)
            )
        }

        drawSparkle(
            center = Offset(crown.x - halfWidth * 0.35f, crown.y + halfHeight * 0.22f),
            radius = unit * 0.10f,
            color = Color.White.copy(alpha = 0.80f)
        )
    }
}

/** Share of the badge's height given over to the crown, above the slab. */
private const val PlaqueCrownShare = 0.30f

/** Share of the badge's width the slab leaves free at each side for the leaves. */
private const val PlaqueLeafMargin = 0.085f

/**
 * The brand at launch, built the way the reference badge is: the logotype carved
 * into a wooden slab, a gold crown riding the top edge, and a fringe of leaves
 * round the sides.
 *
 * The slab is the only brown mass in the game, which is what stops the mark
 * reading as a sticker on the sky. The crown and the leaves are the same parts
 * the menu already draws, so the two screens share one material.
 */
@Composable
fun BrandPlaque(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        // PUZZLE runs about four and a half em wide, so the display size follows
        // the slab rather than the font being picked and the slab built round it.
        // It is capped like the menu's is, so the two never show the logotype at
        // two different sizes on the same device.
        val fontSize = ((maxWidth * 0.84f).value / 4.5f).roundToInt().coerceIn(20, 60)

        PlaqueSlab(Modifier.matchParentSize())

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = maxHeight * PlaqueCrownShare),
            contentAlignment = Alignment.Center
        ) {
            Wordmark(
                fontSize = fontSize,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .aspectRatio(2.1f)
            )
        }

        CrownBadge(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(maxWidth * 0.30f)
        )
    }
}

/**
 * The slab, the planting behind it and the glints on it.
 *
 * Leaves go down first and in two passes, a dark mass and then lit blades, so
 * the fringe round the plaque has the same depth as the planting along the
 * bottom of the menu field and does not read as flat green cut-outs.
 */
@Composable
private fun PlaqueSlab(modifier: Modifier = Modifier) {
    val twinkle = rememberInfiniteTransition(label = "plaqueGlints").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glints"
    )
    val reduceMotion = rememberSystemReduceMotion()

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val energy = if (reduceMotion) 0.5f else twinkle.value

        val slabLeft = width * PlaqueLeafMargin
        val slabRight = width * (1f - PlaqueLeafMargin)
        val slabTop = height * PlaqueCrownShare
        val slabWidth = slabRight - slabLeft
        val slabHeight = height - slabTop
        val radius = CornerRadius(slabHeight * 0.24f, slabHeight * 0.24f)

        val clumps = listOf(
            Offset(slabLeft, slabTop + slabHeight * 0.16f) to 0.17f,
            Offset(slabRight, slabTop + slabHeight * 0.24f) to 0.16f,
            Offset(width * 0.5f, height * 1.04f) to 0.30f
        )
        for ((index, clump) in clumps.withIndex()) {
            val (origin, spread) = clump
            val reach = width * spread
            for (pass in 0 until 2) {
                val count = if (pass == 0) 16 else 8
                for (blade in 0 until count) {
                    val seed = index * 71 + pass * 31 + blade * 5
                    val angle = noise(seed) * 240f - 120f
                    val radians = angle * (PI / 180.0).toFloat()
                    val length = reach * (0.45f + 0.65f * noise(seed + 6))
                    val at = Offset(
                        x = origin.x + sin(radians) * length * 0.55f,
                        y = origin.y - cos(radians) * length * 0.50f
                    )
                    drawLeaf(
                        center = at,
                        length = length,
                        width = length * (if (pass == 0) 0.46f else 0.48f),
                        degrees = angle * 0.40f + 90f,
                        color = if (pass == 0) {
                            if (blade % 3 == 0) LeafDeep else LeafMid
                        } else {
                            LeafLight
                        },
                        vein = if (pass == 0) LeafDeep else LeafMid
                    )
                }
            }
        }

        // A halo of warm light behind the slab, so the wood is lit rather than cut out.
        val lit = Offset(width * 0.5f, slabTop + slabHeight * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to MarkGold.copy(alpha = 0.22f),
                    0.62f to MarkGold.copy(alpha = 0.07f),
                    1f to Color.Transparent
                ),
                center = lit,
                radius = width * 0.72f
            ),
            radius = width * 0.72f,
            center = lit
        )

        // The slab: a cast shadow, the lit face, its own grain, a bevel across
        // the top and a shadow along the bottom - which is the whole of moulding.
        val topLeft = Offset(slabLeft, slabTop)
        val slabSize = Size(slabWidth, slabHeight)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.26f),
            topLeft = Offset(slabLeft, slabTop + slabHeight * 0.035f),
            size = slabSize,
            cornerRadius = radius
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to PlankTop,
                    0.38f to PlankFace,
                    1f to PlankDeep
                ),
                startY = slabTop,
                endY = slabTop + slabHeight
            ),
            topLeft = topLeft,
            size = slabSize,
            cornerRadius = radius
        )

        clipToRoundRect(topLeft, slabSize, radius) {
            for (grain in 0 until 5) {
                val y = slabTop + slabHeight * (0.16f + 0.17f * grain)
                drawLine(
                    color = PlankEdge.copy(alpha = 0.16f),
                    start = Offset(slabLeft, y),
                    end = Offset(slabRight, y + slabHeight * 0.02f),
                    strokeWidth = slabHeight * 0.012f,
                    cap = StrokeCap.Round
                )
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.30f),
                        0.26f to Color.Transparent,
                        0.74f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.24f)
                    ),
                    startY = slabTop,
                    endY = slabTop + slabHeight
                ),
                topLeft = topLeft,
                size = slabSize
            )
        }

        drawRoundRect(
            color = PlankEdge,
            topLeft = topLeft,
            size = slabSize,
            cornerRadius = radius,
            style = Stroke(width = slabHeight * 0.020f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.30f),
            topLeft = Offset(slabLeft, slabTop + slabHeight * 0.012f),
            size = Size(slabWidth, slabHeight * 0.90f),
            cornerRadius = radius,
            style = Stroke(width = slabHeight * 0.010f)
        )

        for (index in 0 until 7) {
            val phase = noise(index * 17 + 2)
            val fade = 0.35f + 0.65f * (0.5f + 0.5f * sin((energy + phase) * PI.toFloat() * 2f))
            drawSparkle(
                center = Offset(
                    x = (0.04f + 0.92f * phase) * width,
                    y = (0.06f + 0.88f * noise(index * 23 + 9)) * height
                ),
                radius = width * (0.012f + 0.014f * phase),
                color = Color.White.copy(alpha = 0.62f * fade)
            )
        }
    }
}

/** Clips to a rounded rectangle, so the grain and the bevel stay inside the wood. */
private inline fun DrawScope.clipToRoundRect(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    block: DrawScope.() -> Unit
) {
    val path = Path().apply { addRoundRect(RoundRect(Rect(topLeft, size), cornerRadius)) }
    clipPath(path) { block() }
}

/**
 * The crown on the slab: three points with a ball on each, a blue stone in the
 * middle of the band, and the same dark-then-gold two pass the stat badges use,
 * so it is carved from the same gold as the wordmark.
 */
@Composable
private fun CrownBadge(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height * 0.54f)

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to MarkGold.copy(alpha = 0.30f),
                    1f to Color.Transparent
                ),
                center = center,
                radius = unit * 0.68f
            ),
            radius = unit * 0.68f,
            center = center
        )

        val crown = crownPath()
        scale(unit, unit, center) {
            translate(0f, 0.055f) {
                drawPath(path = crown, color = MarkExtrude)
            }
            drawPath(
                path = crown,
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to MarkGoldTop,
                        0.46f to MarkGold,
                        1f to MarkGoldDeep
                    ),
                    startY = -0.42f,
                    endY = 0.34f
                )
            )
            drawPath(
                path = crown,
                color = Color.White.copy(alpha = 0.45f),
                style = Stroke(width = 0.030f, join = StrokeJoin.Round)
            )
        }

        for (ball in listOf(-0.31f to -0.26f, 0f to -0.38f, 0.31f to -0.26f)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = unit * 0.055f,
                center = center + Offset(ball.first * unit, ball.second * unit)
            )
        }

        // The stone in the band, cut the same way as the gems above it.
        val stone = Offset(center.x, center.y + unit * 0.20f)
        val reach = unit * 0.075f
        val gem = Path().apply {
            moveTo(stone.x - reach * 0.8f, stone.y)
            lineTo(stone.x, stone.y - reach * 0.9f)
            lineTo(stone.x + reach * 0.8f, stone.y)
            lineTo(stone.x, stone.y + reach * 0.9f)
            close()
        }
        drawPath(path = gem, color = PlayEdge)
        drawPath(
            path = gem,
            brush = Brush.verticalGradient(
                colors = listOf(TileCyanLight, TileAzure, TileAzureDeep),
                startY = stone.y - reach,
                endY = stone.y + reach
            )
        )
        drawPath(
            path = gem,
            color = Color.White.copy(alpha = 0.80f),
            style = Stroke(width = unit * 0.014f, join = StrokeJoin.Round)
        )
    }
}

/** The crown in a unit box: three points, a straight band, and a flat base. */
private fun crownPath(): Path = Path().apply {
    moveTo(-0.44f, 0.14f)
    lineTo(-0.31f, -0.20f)
    lineTo(-0.16f, 0.02f)
    lineTo(0f, -0.32f)
    lineTo(0.16f, 0.02f)
    lineTo(0.31f, -0.20f)
    lineTo(0.44f, 0.14f)
    lineTo(0.40f, 0.32f)
    lineTo(-0.40f, 0.32f)
    close()
}

/**
 * The loading bar under the brand: a dark trough with a lit fill running its
 * length and a spark riding the head of the fill.
 *
 * The fill is on its own keyframed cycle rather than on the launch clock, so it
 * is still sweeping at the moment the handoff begins and the screen reads as
 * work in progress for the whole of it.
 */
@Composable
fun SplashLoader(modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = "splashLoader").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1700
                0f at 0
                0.88f at 820
                0.88f at 1010
                0f at 1290
                0f at 1700
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val reduceMotion = rememberSystemReduceMotion()

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val radius = CornerRadius(height / 2f, height / 2f)
        val progress = if (reduceMotion) 0.62f else sweep.value
        val fill = (width * progress).coerceAtLeast(height)

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to NeonCyan.copy(alpha = 0.10f),
                    0.5f to NeonCyan.copy(alpha = 0.34f),
                    1f to Color.Transparent
                )
            ),
            topLeft = Offset(-height * 0.5f, -height * 0.9f),
            size = Size(width + height, height * 2.8f),
            cornerRadius = CornerRadius(height * 1.4f, height * 1.4f)
        )

        drawRoundRect(color = Color(0xFF16205A).copy(alpha = 0.62f), cornerRadius = radius)

        if (fill > 1f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to TileCyanLight,
                        0.55f to NeonCyan,
                        1f to Color.White
                    ),
                    startX = 0f,
                    endX = fill
                ),
                size = Size(fill, height),
                cornerRadius = radius
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.55f),
                        0.5f to Color.Transparent
                    )
                ),
                size = Size(fill, height * 0.5f),
                cornerRadius = radius
            )
            val head = Offset(fill - height * 0.34f, height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.75f),
                        1f to Color.Transparent
                    ),
                    center = head,
                    radius = height * 0.90f
                ),
                radius = height * 0.90f,
                center = head
            )
        }

        drawRoundRect(
            color = Color.White.copy(alpha = 0.42f),
            cornerRadius = radius,
            style = Stroke(width = height * 0.075f)
        )
    }
}
