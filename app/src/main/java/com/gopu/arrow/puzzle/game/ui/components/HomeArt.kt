package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.graphics.graphicsLayer
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.theme.CubeFace
import com.gopu.arrow.puzzle.game.ui.theme.CubeSide
import com.gopu.arrow.puzzle.game.ui.theme.CubeTop
import com.gopu.arrow.puzzle.game.ui.theme.LeafDeep
import com.gopu.arrow.puzzle.game.ui.theme.LeafLight
import com.gopu.arrow.puzzle.game.ui.theme.LeafMid
import com.gopu.arrow.puzzle.game.ui.theme.MarkGold
import com.gopu.arrow.puzzle.game.ui.theme.MenuSkyDeep
import com.gopu.arrow.puzzle.game.ui.theme.MenuSkyFloor
import com.gopu.arrow.puzzle.game.ui.theme.MenuSkyMid
import com.gopu.arrow.puzzle.game.ui.theme.MenuSkyTop
import com.gopu.arrow.puzzle.game.ui.theme.TileAzure
import com.gopu.arrow.puzzle.game.ui.theme.TileAzureDeep
import com.gopu.arrow.puzzle.game.ui.theme.TileAzureLight
import com.gopu.arrow.puzzle.game.ui.theme.TileCyan
import com.gopu.arrow.puzzle.game.ui.theme.TileCyanDeep
import com.gopu.arrow.puzzle.game.ui.theme.TileCyanLight
import com.gopu.arrow.puzzle.game.ui.theme.TileGreen
import com.gopu.arrow.puzzle.game.ui.theme.TileGreenDeep
import com.gopu.arrow.puzzle.game.ui.theme.TileGreenLight
import com.gopu.arrow.puzzle.game.ui.theme.TileRed
import com.gopu.arrow.puzzle.game.ui.theme.TileRedDeep
import com.gopu.arrow.puzzle.game.ui.theme.TileRedLight
import com.gopu.arrow.puzzle.game.ui.theme.TileViolet
import com.gopu.arrow.puzzle.game.ui.theme.TileVioletDeep
import com.gopu.arrow.puzzle.game.ui.theme.TileVioletLight
import com.gopu.arrow.puzzle.game.ui.theme.TileYellow
import com.gopu.arrow.puzzle.game.ui.theme.TileYellowDeep
import com.gopu.arrow.puzzle.game.ui.theme.TileYellowLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The three tones of one glossy cube: the lit top facet, the face, and the
 * shaded side that shows under the face. [glossyCube] is the single painter for
 * every cube on the menu - the floating arrow tiles around the wordmark and the
 * hero cube on the splash - so the whole screen shares one material.
 */
internal data class TileSkin(val light: Color, val face: Color, val deep: Color)

internal val SkinCyan = TileSkin(TileCyanLight, TileCyan, TileCyanDeep)
internal val SkinGreen = TileSkin(TileGreenLight, TileGreen, TileGreenDeep)
internal val SkinRed = TileSkin(TileRedLight, TileRed, TileRedDeep)
internal val SkinYellow = TileSkin(TileYellowLight, TileYellow, TileYellowDeep)
internal val SkinViolet = TileSkin(TileVioletLight, TileViolet, TileVioletDeep)

/** How tall the shaded side of a cube is, as a fraction of its edge. */
private const val CubeDepth = 0.17f

/**
 * A single glossy cube, drawn as one silhouette plus one face so the strip left
 * under the face reads as its side. The face is then lit from the top left, rimmed
 * in white, and - when [direction] is set - stamped with a white arrow that
 * carries its own drop shadow and top bevel.
 */
internal fun DrawScope.glossyCube(
    topLeft: Offset,
    edge: Float,
    skin: TileSkin,
    direction: Direction? = null,
    depth: Float = edge * CubeDepth,
    corner: Float = 0.30f,
    glow: Color? = null,
    alpha: Float = 1f
) {
    val faceHeight = edge - depth
    val radius = CornerRadius(edge * corner, edge * corner)
    val center = Offset(topLeft.x + edge / 2f, topLeft.y + faceHeight / 2f)

    if (glow != null) {
        val glowRadius = edge * 0.95f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to glow.copy(alpha = 0.55f * alpha),
                    0.55f to glow.copy(alpha = 0.20f * alpha),
                    1f to Color.Transparent
                ),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )
    }

    // The whole silhouette in the shaded tone: what shows below the face is the
    // side, and what shows around it is the rounded edge of the bottom.
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(skin.deep, skin.deep.darken(0.55f)),
            startY = topLeft.y,
            endY = topLeft.y + edge
        ),
        topLeft = topLeft,
        size = Size(edge, edge),
        cornerRadius = radius,
        alpha = alpha
    )

    // The face, lit from the top left corner.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(skin.light, skin.face),
            start = Offset(topLeft.x, topLeft.y),
            end = Offset(topLeft.x + edge, topLeft.y + faceHeight)
        ),
        topLeft = topLeft,
        size = Size(edge, faceHeight),
        cornerRadius = radius,
        alpha = alpha
    )

    /*
     * Everything that makes the face read as moulded plastic rather than as a
     * flat swatch, and all of it is clipped to the face so nothing bleeds past
     * the rounded corners: a bright lens across the top third where the light
     * lands, a harder sheen along the very top, and a shaded band at the bottom
     * that turns the flat face into a shallow dome.
     */
    val face = Path().apply {
        addRoundRect(
            RoundRect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + edge,
                bottom = topLeft.y + faceHeight,
                radiusX = radius.x,
                radiusY = radius.y
            )
        )
    }
    clipPath(face) {
        val lens = edge * 0.66f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.72f * alpha),
                    0.55f to Color.White.copy(alpha = 0.30f * alpha),
                    1f to Color.White.copy(alpha = 0f)
                ),
                center = Offset(topLeft.x + edge * 0.42f, topLeft.y + faceHeight * 0.16f),
                radius = lens
            ),
            radius = lens,
            center = Offset(topLeft.x + edge * 0.42f, topLeft.y + faceHeight * 0.16f)
        )

        val sheen = faceHeight * 0.34f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0f)),
                startY = topLeft.y,
                endY = topLeft.y + sheen
            ),
            topLeft = topLeft,
            size = Size(edge, sheen)
        )

        val shade = faceHeight * 0.46f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    skin.deep.copy(alpha = 0.34f * alpha)
                ),
                startY = topLeft.y + faceHeight - shade,
                endY = topLeft.y + faceHeight
            ),
            topLeft = Offset(topLeft.x, topLeft.y + faceHeight - shade),
            size = Size(edge, shade)
        )

        // The hard specular line just inside the top edge.
        drawLine(
            color = Color.White.copy(alpha = 0.72f * alpha),
            start = Offset(topLeft.x + edge * 0.13f, topLeft.y + edge * corner * 0.42f),
            end = Offset(topLeft.x + edge * 0.87f, topLeft.y + edge * corner * 0.42f),
            strokeWidth = edge * 0.030f,
            cap = StrokeCap.Round
        )
    }

    // Rim light: bright along the top, fading to nothing down the sides.
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.26f)
            ),
            startY = topLeft.y,
            endY = topLeft.y + faceHeight
        ),
        topLeft = topLeft,
        size = Size(edge, faceHeight),
        cornerRadius = radius,
        style = Stroke(width = edge * 0.026f),
        alpha = alpha
    )

    if (direction != null) drawCubeGlyph(direction, edge, faceHeight, topLeft, skin, alpha)
}

/**
 * The white arrow stamped on a cube face. The shadow sits a few pixels under the
 * fill so the glyph lifts off the face, and the bevel is the same path clipped
 * and washed from the top, which is what gives the reference tiles their lit edge.
 */
private fun DrawScope.drawCubeGlyph(
    direction: Direction,
    edge: Float,
    faceHeight: Float,
    topLeft: Offset,
    skin: TileSkin,
    alpha: Float
) {
    val box = min(edge, faceHeight) * 0.60f
    val center = Offset(topLeft.x + edge / 2f, topLeft.y + faceHeight / 2f)

    drawPath(
        path = chunkyArrow(direction, box, center.copy(y = center.y + edge * 0.045f)),
        color = skin.deep.copy(alpha = 0.45f * alpha)
    )

    val glyph = chunkyArrow(direction, box, center)
    clipPath(glyph) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
                startY = center.y - box / 2f,
                endY = center.y + box * 0.2f
            ),
            topLeft = Offset(center.x - box, center.y - box),
            size = Size(box * 2f, box * 2f)
        )
    }
    drawPath(path = glyph, color = Color.White, alpha = alpha)
}

/**
 * The chunky arrow glyph in a unit box, turned to face [direction] and scaled to
 * [box] around [center]. Sharp corners on purpose: it has to stay readable as a
 * direction at tile size, and the splash hero draws the same shape far larger
 * without having to be told about it.
 */
internal fun chunkyArrow(direction: Direction, box: Float, center: Offset): Path {
    val shaft = 0.17f
    val head = 0.40f
    val tail = -0.42f
    val neck = 0.04f

    val unit = listOf(
        Offset(tail, -shaft),
        Offset(neck, -shaft),
        Offset(neck, -head),
        Offset(head, 0f),
        Offset(neck, head),
        Offset(neck, shaft),
        Offset(tail, shaft)
    )

    val angle = when (direction) {
        Direction.RIGHT -> 0f
        Direction.DOWN -> (PI / 2.0).toFloat()
        Direction.LEFT -> PI.toFloat()
        Direction.UP -> (-PI / 2.0).toFloat()
    }
    val cosAngle = cos(angle)
    val sinAngle = sin(angle)

    val path = Path()
    for (index in unit.indices) {
        val point = unit[index] * box
        val x = center.x + point.x * cosAngle - point.y * sinAngle
        val y = center.y + point.x * sinAngle + point.y * cosAngle
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** A four-point sparkle, the small glint the backdrop and the beam are built from. */
internal fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val waist = radius * 0.26f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + waist, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - waist, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * A single leaf, lens shaped and veined, drawn about its own centre and turned by
 * [degrees]. The foliage clumps are built from these so the bottom edge of the
 * screen reads as planting rather than as green blobs.
 */
internal fun DrawScope.drawLeaf(
    center: Offset,
    length: Float,
    width: Float,
    degrees: Float,
    color: Color,
    vein: Color
) {
    val body = Path().apply {
        moveTo(-length / 2f, 0f)
        quadraticTo(0f, -width, length / 2f, 0f)
        quadraticTo(0f, width, -length / 2f, 0f)
        close()
    }
    rotate(degrees, center) {
        translate(center.x, center.y) {
            drawPath(path = body, color = color)
            drawLine(
                color = vein,
                start = Offset(-length * 0.42f, 0f),
                end = Offset(length * 0.40f, 0f),
                strokeWidth = width * 0.10f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Deterministic 0..1 from a seed, so the backdrop is identical on every frame. */
internal fun noise(seed: Int): Float {
    var value = seed * 1103515245 + 12345
    value = value xor (value shr 16)
    value *= -1640531527
    value = value xor (value shr 13)
    return (value and 0x7FFFFFFF) / 0x7FFFFFFF.toFloat()
}

/** One soft out-of-focus light in the sky. */
private class Bokeh(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float
)

private val SkyBokeh = listOf(
    Bokeh(0.08f, 0.10f, 0.30f, Color.White, 0.22f),
    Bokeh(0.86f, 0.04f, 0.22f, Color(0xFFFFE0C2), 0.26f),
    Bokeh(0.62f, 0.16f, 0.13f, Color.White, 0.16f),
    Bokeh(0.30f, 0.30f, 0.20f, Color(0xFFC9C6FF), 0.24f),
    Bokeh(0.95f, 0.36f, 0.26f, Color(0xFFFFCFE6), 0.16f),
    Bokeh(0.14f, 0.52f, 0.24f, Color(0xFF9FE0FF), 0.14f),
    Bokeh(0.74f, 0.58f, 0.30f, Color(0xFF7FE8FF), 0.12f),
    Bokeh(0.42f, 0.72f, 0.26f, Color(0xFF6FD2FF), 0.10f)
)

/** A translucent cube hanging in the far distance, out of focus behind the board. */
private class DistantCube(
    val x: Float,
    val y: Float,
    val size: Float,
    val degrees: Float,
    val color: Color,
    val alpha: Float
)

private val SkyCubes = listOf(
    DistantCube(0.10f, 0.05f, 0.20f, -14f, Color(0xFFFFB27A), 0.30f),
    DistantCube(0.72f, 0.02f, 0.15f, 10f, Color(0xFF9FD2FF), 0.26f),
    DistantCube(0.92f, 0.22f, 0.24f, 18f, Color(0xFFE7B6FF), 0.20f),
    DistantCube(0.24f, 0.24f, 0.13f, -22f, Color.White, 0.22f),
    DistantCube(0.48f, 0.06f, 0.09f, 6f, Color(0xFFFFE9B0), 0.24f)
)

/**
 * The menu field: a violet-indigo sky falling to a deep blue floor, with soft
 * focus lights, distant cubes, glints and a fringe of planting along the bottom
 * edge. It is painted in one canvas, back to front, and never recomposes.
 */
@Composable
fun HomeBackdrop(modifier: Modifier = Modifier) {
    val reduceMotion = rememberSystemReduceMotion()
    val twinkle by rememberInfiniteTransition(label = "backdrop").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "twinkle"
    )
    val energy = if (reduceMotion) 0.5f else twinkle

    Canvas(modifier) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to MenuSkyTop,
                    0.30f to MenuSkyMid,
                    0.62f to MenuSkyFloor,
                    1f to MenuSkyDeep
                )
            )
        )

        // Light spilling in from the top left, so the sky is not a flat ramp.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.26f),
                    1f to Color.Transparent
                ),
                center = Offset(width * 0.12f, -height * 0.06f),
                radius = width * 1.15f
            ),
            radius = width * 1.15f,
            center = Offset(width * 0.12f, -height * 0.06f)
        )

        for (light in SkyBokeh) {
            val radius = light.radius * width
            val center = Offset(light.x * width, light.y * height)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to light.color.copy(alpha = light.alpha),
                        0.72f to light.color.copy(alpha = light.alpha * 0.35f),
                        1f to Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        for (cube in SkyCubes) {
            val edge = cube.size * width
            val topLeft = Offset(cube.x * width - edge / 2f, cube.y * height - edge / 2f)
            val radius = CornerRadius(edge * 0.24f, edge * 0.24f)
            rotate(cube.degrees, Offset(cube.x * width, cube.y * height)) {
                translate(topLeft.x, topLeft.y) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(cube.color.copy(alpha = cube.alpha), Color.Transparent)
                        ),
                        size = Size(edge, edge),
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = cube.alpha * 0.55f),
                        size = Size(edge, edge * 0.16f),
                        cornerRadius = radius,
                        style = Stroke(width = edge * 0.022f, join = StrokeJoin.Round)
                    )
                }
            }
        }

        // Glints, each on its own phase so they never pulse as a group.
        val glints = 9
        for (index in 0 until glints) {
            val phase = noise(index * 7 + 3)
            val fade = 0.35f + 0.65f * (0.5f + 0.5f * sin((energy + phase) * PI.toFloat() * 2f))
            val x = (0.06f + 0.88f * phase) * width
            val y = (0.04f + 0.80f * noise(index * 13 + 11)) * height
            drawSparkle(
                center = Offset(x, y),
                radius = width * (0.010f + 0.016f * noise(index * 29 + 5)),
                color = Color.White.copy(alpha = 0.55f * fade)
            )
        }

        // Planting along the bottom edge, closing the field off.
        drawFoliage(Offset(-width * 0.04f, height * 1.02f), width * 0.62f, seed = 11)
        drawFoliage(Offset(width * 1.05f, height * 0.99f), width * 0.55f, seed = 97)
        drawFoliage(Offset(width * 0.50f, height * 1.10f), width * 0.40f, seed = 53)

        // Vignette, so the corners of the field fall away behind the furniture.
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f)
                )
            )
        )
    }
}

/**
 * A clump of leaves about [center], [spread] wide, thrown from [seed] so the same
 * clump is drawn the same way on every frame. Two passes - a dark mass, then lit
 * leaves on top of it - is what stops it reading as flat green circles.
 */
private fun DrawScope.drawFoliage(center: Offset, spread: Float, seed: Int) {
    val blades = 22
    for (index in 0 until blades) {
        val angle = noise(seed + index * 5) * 200f - 100f
        val reach = spread * (0.30f + 0.70f * noise(seed + index * 11))
        val length = spread * (0.26f + 0.34f * noise(seed + index * 17))
        val at = Offset(
            x = center.x + cos(angle * (PI / 180.0).toFloat()) * reach,
            y = center.y + sin(angle * (PI / 180.0).toFloat()) * reach * 0.72f
        )
        drawLeaf(
            center = at,
            length = length,
            width = length * 0.44f,
            degrees = angle * 0.5f - 90f,
            color = if (index % 3 == 0) LeafDeep else LeafMid,
            vein = LeafDeep
        )
    }
    for (index in 0 until 10) {
        val angle = noise(seed + index * 23) * 180f - 90f
        val reach = spread * (0.20f + 0.55f * noise(seed + index * 31))
        val length = spread * (0.22f + 0.26f * noise(seed + index * 37))
        val at = Offset(
            x = center.x + cos(angle * (PI / 180.0).toFloat()) * reach,
            y = center.y + sin(angle * (PI / 180.0).toFloat()) * reach * 0.66f
        )
        drawLeaf(
            center = at,
            length = length,
            width = length * 0.46f,
            degrees = angle * 0.4f - 90f,
            color = LeafLight,
            vein = LeafMid
        )
    }
}

/**
 * The warm halo and the planting that sit behind the wordmark, so the mark reads
 * as lit from its own gold rather than pasted onto the sky.
 */
@Composable
fun MarkGlow(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to MarkGold.copy(alpha = 0.34f),
                    0.55f to MarkGold.copy(alpha = 0.12f),
                    1f to Color.Transparent
                ),
                center = Offset(width * 0.5f, height * 0.5f),
                radius = width * 0.62f
            ),
            radius = width * 0.62f,
            center = Offset(width * 0.5f, height * 0.5f)
        )

        val leaves = listOf(
            Triple(0.16f, 0.40f, -34f),
            Triple(0.10f, 0.56f, 18f),
            Triple(0.22f, 0.24f, -62f),
            Triple(0.86f, 0.36f, 128f),
            Triple(0.92f, 0.54f, 152f),
            Triple(0.79f, 0.22f, 104f)
        )
        for ((index, leaf) in leaves.withIndex()) {
            val length = width * (0.13f + 0.05f * noise(index * 19 + 4))
            drawLeaf(
                center = Offset(leaf.first * width, leaf.second * height),
                length = length,
                width = length * 0.50f,
                degrees = leaf.third,
                color = if (index % 2 == 0) LeafMid else LeafDeep,
                vein = LeafLight
            )
        }

        for (index in 0 until 6) {
            val x = (0.18f + 0.66f * noise(index * 3 + 1)) * width
            val y = (0.12f + 0.76f * noise(index * 9 + 2)) * height
            drawSparkle(
                center = Offset(x, y),
                radius = width * 0.016f,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

/**
 * A floating arrow cube for the corners of the wordmark: the same [glossyCube]
 * material as the board, lit from its own glow and turned by [tilt].
 *
 * [phase] is where this cube sits in the shared breathing cycle. A screen that
 * floats several cubes at once gives each its own phase, so they rise and fall
 * against each other instead of pumping as one block.
 */
@Composable
internal fun FloatingArrowTile(
    direction: Direction,
    skin: TileSkin,
    modifier: Modifier = Modifier,
    tilt: Float = 0f,
    phase: Float = 0f
) {
    val reduceMotion = rememberSystemReduceMotion()
    val bob by rememberInfiniteTransition(label = "floatingTile").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val energy = if (reduceMotion) 0.5f else bob

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = tilt
            val breath = 1f + 0.018f * (0.5f + 0.5f * sin((energy + phase) * PI.toFloat() * 2f))
            scaleX = breath
            scaleY = breath
        }
    ) {
        glossyCube(
            topLeft = Offset.Zero,
            edge = size.width,
            skin = skin,
            direction = direction,
            glow = skin.light
        )
    }
}

/** Darkens a colour toward black by [amount], for shaded cube sides. */
private fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha
)
