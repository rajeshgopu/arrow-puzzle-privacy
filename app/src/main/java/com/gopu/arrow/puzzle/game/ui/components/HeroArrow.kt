package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import com.gopu.arrow.puzzle.game.ui.theme.HeroCyan
import com.gopu.arrow.puzzle.game.ui.theme.HeroCyanDeep
import com.gopu.arrow.puzzle.game.ui.theme.HeroCyanEdge
import com.gopu.arrow.puzzle.game.ui.theme.HeroCyanLight
import com.gopu.arrow.puzzle.game.ui.theme.TealInk
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Right-pointing arrow silhouette in a normalised 0..1 box, listed clockwise
 * from the top-left of the shaft. The two 0.30/0.70 vertices are the concave
 * joins where the shaft meets the back edge of the head.
 */
private val ArrowPoints = listOf(
    Offset(0.075f, 0.285f),
    Offset(0.470f, 0.285f),
    Offset(0.470f, 0.115f),
    Offset(0.955f, 0.500f),
    Offset(0.470f, 0.885f),
    Offset(0.470f, 0.715f),
    Offset(0.075f, 0.715f)
)

/** Per-corner fillet: crisp enough to read as one shape, softened at the tip. */
private val ArrowRadii = listOf(0.055f, 0.030f, 0.040f, 0.032f, 0.040f, 0.030f, 0.055f)

/**
 * The menu hero: a thick cyan arrow built as layered 2D vector art rather than
 * a raster asset, so it stays crisp at any size.
 *
 * Depth comes from four passes, back to front: a silhouette-shaped ambient
 * shadow, a two-step extrusion under the body, a diagonal-lit body gradient,
 * and a clipped top bevel with a diagonal sheen. A wide radial halo and three
 * faint speed streaks behind the tail suggest an arrow about to launch.
 *
 * The idle motion is deliberately small (a 2% breath and a slow halo pulse),
 * so it reads as polish rather than an animation, and it is suppressed entirely
 * when the system animation scale is zero.
 */
@Composable
fun HeroArrow(
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val reduceMotion = rememberSystemReduceMotion()

    val transition = rememberInfiniteTransition(label = "heroArrow")

    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val motion = animated && !reduceMotion
    val energy = if (motion) breathe else 0.5f
    val phase = if (motion) sweep else 0f

    Canvas(
        modifier = modifier.graphicsLayer {
            val breath = 1f + 0.022f * energy
            scaleX = breath
            scaleY = breath
        }
    ) {
        val width = size.width
        val height = size.height
        val unit = min(width, height)
        val depth = unit * 0.050f
        val center = Offset(width * 0.5f, height * 0.5f)

        val body = arrowPath(width, height)

        /*
         * Ambient halo. Wide and very faint so the arrow reads as lit rather
         * than as a neon sign: the far stop is fully transparent.
         */
        val haloRadius = unit * 0.88f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to HeroCyan.copy(alpha = 0.15f + 0.09f * energy),
                    0.55f to HeroCyan.copy(alpha = 0.06f + 0.04f * energy),
                    1f to Color.Transparent
                ),
                center = center,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = center
        )

        /*
         * Silhouette shadow: the same shape dropped down and right, repeated
         * at decreasing offset so the edge fades instead of ending hard.
         */
        repeat(3) { layer ->
            val fall = depth * (1.9f - layer * 0.5f)
            drawPath(
                path = arrowPath(
                    width = width,
                    height = height,
                    translate = Offset(fall * 0.30f, fall)
                ),
                color = TealInk.copy(alpha = 0.050f)
            )
        }

        /*
         * Three streaks trailing the tail, each on its own phase so the motion
         * never pulses as a group.
         */
        if (motion) {
            val tail = width * 0.075f
            for (index in 0 until 3) {
                val progress = (phase + index * 0.31f) % 1f
                val fade = sin(progress * PI.toFloat())
                if (fade <= 0.03f) continue
                val y = center.y + (index - 1) * height * 0.17f
                val length = width * (0.05f + 0.13f * progress)
                drawLine(
                    color = HeroCyanLight.copy(alpha = 0.26f * fade),
                    start = Offset(tail - length, y),
                    end = Offset(tail, y),
                    strokeWidth = unit * 0.020f,
                    cap = StrokeCap.Round
                )
            }
        }

        /*
         * Extrusion. Two steps of the same silhouette, offset downward, which
         * gives the body a clean dimensional edge instead of a flat sticker.
         */
        drawPath(
            path = arrowPath(width, height, translate = Offset(0f, depth)),
            brush = Brush.verticalGradient(
                colors = listOf(HeroCyanDeep, HeroCyanEdge),
                startY = height * 0.5f,
                endY = height
            )
        )
        drawPath(
            path = arrowPath(width, height, translate = Offset(0f, depth * 0.42f)),
            brush = Brush.verticalGradient(
                colors = listOf(HeroCyan, HeroCyanDeep),
                startY = height * 0.4f,
                endY = height
            )
        )

        /*
         * Body, lit from the top left.
         */
        drawPath(
            path = body,
            brush = Brush.linearGradient(
                colors = listOf(HeroCyanLight, HeroCyan, HeroCyanDeep),
                start = Offset(width * 0.06f, height * 0.05f),
                end = Offset(width * 0.92f, height * 0.95f)
            )
        )

        /*
         * Inner light: an inset copy of the silhouette as a top bevel, clipped
         * to the body so it can never bleed past the edge. A flat vector arrow
         * reads better lit from one direction only, so there is no second
         * specular pass here: the rim below closes the silhouette.
         */
        clipPath(body) {
            drawPath(
                path = arrowPath(width, height, inset = 0.10f, lift = -0.012f),
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = height * 0.10f,
                    endY = height * 0.72f
                )
            )
        }

        /*
         * Rim light: bright where the light lands, tinted and quiet at the
         * bottom so the silhouette still closes.
         */
        drawPath(
            path = body,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.70f),
                    Color.White.copy(alpha = 0.14f),
                    HeroCyanEdge.copy(alpha = 0.28f)
                ),
                startY = height * 0.06f,
                endY = height * 0.96f
            ),
            style = Stroke(width = unit * 0.016f, join = StrokeJoin.Round)
        )
    }
}

/**
 * Builds the arrow outline in pixel space, optionally shifted down for the
 * extrusion, or shrunk and lifted for the inner bevel.
 */
private fun DrawScope.arrowPath(
    width: Float,
    height: Float,
    translate: Offset = Offset.Zero,
    inset: Float = 0f,
    lift: Float = 0f
): Path {
    val unit = min(width, height)
    val count = ArrowPoints.size

    val points = ArrayList<Offset>(count)
    for (point in ArrowPoints) {
        points += Offset(
            x = (0.5f + (point.x - 0.5f) * (1f - inset)) * width,
            y = (0.5f + (point.y - 0.5f) * (1f - inset) + lift) * height
        )
    }

    val entry = ArrayList<Offset>(count)
    val exit = ArrayList<Offset>(count)

    for (index in 0 until count) {
        val previous = points[(index - 1 + count) % count]
        val current = points[index]
        val next = points[(index + 1) % count]

        val incoming = current - previous
        val outgoing = next - current

        val incomingLength = incoming.getDistance().coerceAtLeast(0.0001f)
        val outgoingLength = outgoing.getDistance().coerceAtLeast(0.0001f)

        // A fillet may never eat more than half of either adjacent edge.
        val radius = min(
            ArrowRadii[index] * unit * (1f - inset),
            min(incomingLength, outgoingLength) * 0.5f
        )

        entry += current - incoming / incomingLength * radius
        exit += current + outgoing / outgoingLength * radius
    }

    val path = Path()
    path.moveTo(
        exit[count - 1].x + translate.x,
        exit[count - 1].y + translate.y
    )
    for (index in 0 until count) {
        path.lineTo(
            entry[index].x + translate.x,
            entry[index].y + translate.y
        )
        path.quadraticTo(
            points[index].x + translate.x,
            points[index].y + translate.y,
            exit[index].x + translate.x,
            exit[index].y + translate.y
        )
    }
    path.close()
    return path
}