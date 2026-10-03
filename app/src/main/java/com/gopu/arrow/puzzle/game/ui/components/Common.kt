package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.ui.theme.Cloud
import com.gopu.arrow.puzzle.game.ui.theme.Coral
import com.gopu.arrow.puzzle.game.ui.theme.DiscDeep
import com.gopu.arrow.puzzle.game.ui.theme.DiscFace
import com.gopu.arrow.puzzle.game.ui.theme.DiscLight
import com.gopu.arrow.puzzle.game.ui.theme.GlassDeep
import com.gopu.arrow.puzzle.game.ui.theme.GlassMid
import com.gopu.arrow.puzzle.game.ui.theme.GlassRim
import com.gopu.arrow.puzzle.game.ui.theme.GlassTop
import com.gopu.arrow.puzzle.game.ui.theme.Gold
import com.gopu.arrow.puzzle.game.ui.theme.Ink
import com.gopu.arrow.puzzle.game.ui.theme.InkSoft
import com.gopu.arrow.puzzle.game.ui.theme.MarkExtrude
import com.gopu.arrow.puzzle.game.ui.theme.MarkGold
import com.gopu.arrow.puzzle.game.ui.theme.MarkGoldDeep
import com.gopu.arrow.puzzle.game.ui.theme.MarkGoldTop
import com.gopu.arrow.puzzle.game.ui.theme.MarkIce
import com.gopu.arrow.puzzle.game.ui.theme.MarkIceDeep
import com.gopu.arrow.puzzle.game.ui.theme.MarkIceTop
import com.gopu.arrow.puzzle.game.ui.theme.MarkOutline
import com.gopu.arrow.puzzle.game.ui.theme.MenuTextDim
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.NeonMagenta
import com.gopu.arrow.puzzle.game.ui.theme.NeonPanel
import com.gopu.arrow.puzzle.game.ui.theme.NeonPanelSoft
import com.gopu.arrow.puzzle.game.ui.theme.NeonText
import com.gopu.arrow.puzzle.game.ui.theme.NeonTextDim
import com.gopu.arrow.puzzle.game.ui.theme.PlayBottom
import com.gopu.arrow.puzzle.game.ui.theme.PlayEdge
import com.gopu.arrow.puzzle.game.ui.theme.PlayMid
import com.gopu.arrow.puzzle.game.ui.theme.PlayTop
import com.gopu.arrow.puzzle.game.ui.theme.UiSans
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The two lines of the ARROW PUZZLE wordmark and the gradient each face runs
 * through: ARROW is lit gold, PUZZLE lit ice, so the mark reads as one carved
 * object rather than as two differently coloured words.
 */
private val MarkLines = listOf(
    "ARROW" to listOf(MarkGoldTop, MarkGold, MarkGoldDeep),
    "PUZZLE" to listOf(MarkIceTop, MarkIce, MarkIceDeep)
)

/** The eight compass directions the outline is stamped out in. */
private val OutlineRing = listOf(
    1f to 0f,
    0.7071f to 0.7071f,
    0f to 1f,
    -0.7071f to 0.7071f,
    -1f to 0f,
    -0.7071f to -0.7071f,
    0f to -1f,
    0.7071f to -0.7071f
)

/**
 * The ARROW PUZZLE logotype: a chunky, toy-like wordmark rather than UI copy.
 *
 * Each line is stamped eight times in the cocoa outline colour, once per compass
 * offset, which dilates the glyph into an even border; three more passes in a
 * darker brown step down and to the right for the extruded edge; then the face
 * once, in its own vertical gradient. It is measured through
 * [rememberTextMeasurer] and drawn into a single canvas, so the mark costs two
 * text layouts, never reflows, and stays crisp at any size.
 */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    fontSize: Int = 52,
    tracking: Float = -0.015f
) {
    val measurer = rememberTextMeasurer()

    Canvas(modifier) {
        val unit = fontSize.sp.toPx()
        val step = unit * MarkLineStep
        val outline = unit * 0.082f
        val extrude = unit * 0.085f

        val metrics = MarkLines.map { line ->
            measurer.measure(line.first, MarkStyle(fontSize, tracking))
        }
        val blockHeight = step * (metrics.size - 1) + metrics.last().size.height
        val top = ((size.height - blockHeight) / 2f).coerceAtLeast(0f)

        val layouts = MarkLines.mapIndexed { index, line ->
            measurer.measure(
                text = line.first,
                style = MarkStyle(fontSize, tracking).copy(
                    brush = Brush.verticalGradient(
                        colors = line.second,
                        startY = index * step,
                        endY = (index + 1) * step
                    )
                )
            )
        }

        val extrusions = listOf(
            Offset(extrude * 0.90f, extrude * 0.50f),
            Offset(extrude * 0.50f, extrude * 0.85f),
            Offset(0f, extrude * 1.05f)
        )

        // The face gradients are laid out in the mark's own space, so the whole
        // block is drawn inside one translation instead of moving the brushes.
        translate(0f, top) {
            for ((index, layout) in layouts.withIndex()) {
                val left = (size.width - layout.size.width) / 2f
                val lineTop = index * step

                for (offset in extrusions) {
                    drawText(
                        textLayoutResult = layout,
                        color = MarkExtrude,
                        topLeft = Offset(left + offset.x, lineTop + offset.y)
                    )
                }
                for ((dx, dy) in OutlineRing) {
                    drawText(
                        textLayoutResult = layout,
                        color = MarkOutline,
                        topLeft = Offset(left + dx * outline, lineTop + dy * outline)
                    )
                }
                drawText(textLayoutResult = layout, topLeft = Offset(left, lineTop))
            }
        }
    }
}

/** The shared text style behind every line of the wordmark. */
private fun MarkStyle(fontSize: Int, tracking: Float): TextStyle = TextStyle(
    fontFamily = UiSans,
    fontWeight = FontWeight.ExtraBold,
    fontSize = fontSize.sp,
    letterSpacing = (fontSize * tracking).sp,
    lineHeight = (fontSize * MarkLineStep).sp
)

/**
 * Line advance as a fraction of the display size. Under one, so the two lines
 * overlap slightly and the mark reads as one carved object.
 */
private const val MarkLineStep = 0.84f

/** The badge a stat card carries: the crown on level, the star on stars. */
enum class StatBadge { CROWN, STAR }

/**
 * A frosted blue plate carrying one number: badge on the left, label over value
 * on the right. The plate is a vertical glass gradient with a white rim and a
 * sheen across its top half, and the value is shadowed in deep blue so it keeps
 * its edge against the sky behind it.
 *
 * Given an [onClick] the plate is a button - it takes a press, dims and settles
 * back, and announces itself as one - which is what lets the level plate double
 * as a way straight into the level it is showing.
 */
@Composable
fun GlassStatCard(
    label: String,
    value: String,
    badge: StatBadge,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val settle = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        settle.snapTo(if (pressed) 0.96f else 1f)
        settle.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val plate = modifier
        .height(84.dp)
        .graphicsLayer {
            scaleX = settle.value
            scaleY = settle.value
        }
        .clip(shape)
        .background(
            Brush.verticalGradient(
                0f to GlassTop,
                0.45f to GlassMid,
                1f to GlassDeep
            )
        )
        .border(1.5.dp, GlassRim, shape)

    Box(
        modifier = if (onClick == null) {
            plate
        } else {
            plate
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClickLabel = "Play level $value",
                    onClick = onClick
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "$label $value"
                }
        }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (pressed) 0.10f else 0.26f),
                        0.55f to Color.Transparent
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatBadgeIcon(badge = badge, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = MenuTextDim,
                    fontFamily = UiSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.6.sp
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontFamily = UiSans,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = DiscDeep.copy(alpha = 0.85f),
                            offset = Offset(0f, with(density) { 2.dp.toPx() }),
                            blurRadius = 0f
                        )
                    )
                )
            }
        }
    }
}

/** The gold badge on a stat card: a carved crown, or a faceted star. */
@Composable
private fun StatBadgeIcon(badge: StatBadge, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val edge = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.38f),
                    1f to Color.Transparent
                ),
                center = center,
                radius = edge * 0.54f
            ),
            radius = edge * 0.54f,
            center = center
        )

        // The outline is built in absolute pixels about the centre of the plate,
        // so nothing depends on a transform pivot to land where it is meant to.
        val path = Path().apply {
            when (badge) {
                StatBadge.CROWN -> {
                    moveTo(center.x - 0.42f * edge, center.y + 0.28f * edge)
                    lineTo(center.x - 0.48f * edge, center.y - 0.14f * edge)
                    lineTo(center.x - 0.22f * edge, center.y + 0.05f * edge)
                    lineTo(center.x, center.y - 0.32f * edge)
                    lineTo(center.x + 0.22f * edge, center.y + 0.05f * edge)
                    lineTo(center.x + 0.48f * edge, center.y - 0.14f * edge)
                    lineTo(center.x + 0.42f * edge, center.y + 0.28f * edge)
                }
                StatBadge.STAR -> {
                    for (point in 0 until 10) {
                        val angle = (-Math.PI / 2.0 + point * Math.PI / 5.0).toFloat()
                        val reach = if (point % 2 == 0) 0.48f else 0.22f
                        val x = center.x + cos(angle) * reach * edge
                        val y = center.y + sin(angle) * reach * edge
                        if (point == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
            }
            close()
        }

        // Painted twice: a dark pass dropped a few pixels for its carved edge,
        // then the lit face over the top of it.
        val from = center.y - edge * 0.5f
        val to = center.y + edge * 0.5f
        translate(0f, edge * 0.06f) {
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(MarkGoldDeep, PlayEdge),
                    startY = from,
                    endY = to
                )
            )
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(MarkGoldTop, MarkGold, MarkGoldDeep),
                startY = from,
                endY = to
            )
        )

        // A bright bevel just under the top edge, which is what reads as carving.
        clipPath(path) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
                    startY = center.y - edge * 0.5f,
                    endY = center.y + edge * 0.1f
                ),
                topLeft = Offset(center.x - edge, center.y - edge),
                size = Size(edge * 2f, edge * 2f)
            )
        }

        if (badge == StatBadge.CROWN) {
            val jewels = listOf(
                -0.45f to -0.12f,
                0f to -0.31f,
                0.45f to -0.12f
            )
            for ((x, y) in jewels) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = edge * 0.062f,
                    center = center + Offset(x * edge, y * edge)
                )
            }
        } else {
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.40f),
                style = Stroke(width = edge * 0.05f, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * The signed-in player's name, on a wide shallow plate in the same glass as the
 * stat cards.
 *
 * It is a plate rather than a line of text because the menu reads light on a
 * dark field and a bare name would sit on the sky unsupported. It carries no
 * badge and no number, so the value runs at a size that suits a word rather
 * than a tally, and it is only ever composed when there is a name to show: a
 * guest sees exactly the menu they saw before, with nothing reserved and no gap
 * where this would have been.
 *
 * A name can be long, so it is trimmed to one line and shortened at the end
 * rather than being allowed to wrap or ellipsize on width alone - the plate is a
 * fixed height, so a second line would have nowhere to go.
 */
@Composable
fun PlayerNamePlate(
    name: String,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to GlassTop,
                    0.45f to GlassMid,
                    1f to GlassDeep
                )
            )
            .border(1.5.dp, GlassRim, shape)
    ) {
        // The same sheen across the top half the stat cards carry.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.26f),
                        0.55f to Color.Transparent
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PLAYER",
                color = MenuTextDim,
                fontFamily = UiSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.2.sp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = name,
                color = Color.White,
                fontFamily = UiSans,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = DiscDeep.copy(alpha = 0.85f),
                        offset = Offset(0f, with(density) { 1.dp.toPx() }),
                        blurRadius = 0f
                    )
                )
            )
        }
    }
}

/**
 * The menu's corner control: a glossy blue disc with a white rim and its own
 * glow, so the one small target on the screen still looks moulded like the rest.
 */
@Composable
fun GlossyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 54
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clickable(onClickLabel = contentDescription, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val edge = this.size.width
            val center = Offset(edge / 2f, edge / 2f)
            val radius = edge * 0.46f

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to DiscLight.copy(alpha = 0.50f),
                        0.6f to DiscLight.copy(alpha = 0.18f),
                        1f to Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.55f
                ),
                radius = radius * 1.55f,
                center = center
            )

            drawCircle(color = DiscDeep, radius = radius, center = center.copy(y = center.y + edge * 0.045f))
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to DiscLight,
                        0.62f to DiscFace,
                        1f to DiscDeep
                    ),
                    center = center.copy(y = center.y - edge * 0.10f),
                    radius = radius * 1.30f
                ),
                radius = radius,
                center = center
            )

            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f))
                ),
                topLeft = Offset(center.x - radius * 0.72f, center.y - radius * 0.98f),
                size = Size(radius * 1.44f, radius * 0.86f)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = radius,
                center = center,
                style = Stroke(width = edge * 0.035f)
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size((size * 0.46f).dp)
        )
    }
}

/**
 * The menu's primary action: a wide gold pill with a white play glyph, an orange
 * 3D underside, a wet top sheen and a glow behind it, so it is the loudest thing
 * on the field by a clear margin.
 */
@Composable
fun PlayButton(
    label: String = "PLAY",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val face = 70.dp
    val pill = 7.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(face + pill)
            .clickable(onClickLabel = "Play", onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val depth = with(density) { pill.toPx() }
            val height = with(density) { face.toPx() }
            val radius = CornerRadius(height / 2f, height / 2f)
            val center = Offset(width / 2f, height / 2f)

            /*
             * The bloom. Three rounded plates of falling alpha, each a little
             * larger than the pill and a little fainter, plus a hot core: that
             * stacks into light spilling off the button's own edge rather than
             * the button sitting flat on the field. The canvas is not clipped,
             * so the spill is free to leave the button's own box.
             */
            for (spread in 0 until 3) {
                val grow = height * (0.10f + 0.15f * spread)
                val fall = 1f - 0.30f * spread
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.40f to MarkGold.copy(alpha = 0.26f * fall),
                            0.62f to PlayMid.copy(alpha = 0.40f * fall),
                            1f to Color.Transparent
                        )
                    ),
                    topLeft = Offset(-grow, depth * 0.4f - grow * 0.55f),
                    size = Size(width + grow * 2f, height + grow * 1.1f),
                    cornerRadius = CornerRadius(radius.x + grow, radius.y + grow * 0.8f)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to MarkGold.copy(alpha = 0.55f),
                        0.5f to PlayMid.copy(alpha = 0.22f),
                        1f to Color.Transparent
                    ),
                    center = center,
                    radius = height * 1.15f
                ),
                radius = height * 1.15f,
                center = center
            )

            // The 3D underside, then the face sitting on top of it.
            drawRoundRect(
                color = PlayEdge,
                topLeft = Offset(0f, depth),
                size = Size(width, height),
                cornerRadius = radius
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to PlayTop,
                    0.50f to PlayMid,
                    1f to PlayBottom
                ),
                topLeft = Offset.Zero,
                size = Size(width, height),
                cornerRadius = radius
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0f))
                ),
                topLeft = Offset(0f, depth * 1.4f),
                size = Size(width, height * 0.52f),
                cornerRadius = radius
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.50f),
                topLeft = Offset(0f, depth * 1.2f),
                size = Size(width, height),
                cornerRadius = radius,
                style = Stroke(width = depth * 0.40f)
            )
        }

        Row(
            modifier = Modifier.height(face),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayGlyph(modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                color = Color.White,
                fontFamily = UiSans,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = PlayEdge.copy(alpha = 0.95f),
                        offset = Offset(0f, with(density) { 2.dp.toPx() }),
                        blurRadius = 0f
                    )
                )
            )
        }
    }
}

/** White triangular play icon, drawn so it sits optically centred. */
@Composable
private fun PlayGlyph(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val inset = height * 0.10f
        val path = Path().apply {
            moveTo(width * 0.10f, inset)
            quadraticTo(width * 0.10f, inset - height * 0.06f, width * 0.22f, inset + height * 0.10f)
            lineTo(width * 0.94f, height * 0.5f)
            lineTo(width * 0.22f, height - inset - height * 0.10f)
            quadraticTo(width * 0.10f, height - inset + height * 0.06f, width * 0.10f, height - inset)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink
) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 4.dp, modifier = modifier) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.9f),
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = InkSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun LivesRow(
    lives: Int,
    total: Int = 3,
    modifier: Modifier = Modifier,
    activeColor: Color = Coral,
    emptyColor: Color = Cloud
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val filled = index < lives
            val scale = remember { Animatable(1f) }
            var wasFilled by remember { mutableStateOf(filled) }
            LaunchedEffect(filled) {
                if (wasFilled && !filled) {
                    scale.snapTo(1f)
                    scale.animateTo(0.25f, tween(130))
                    scale.animateTo(1.2f, spring(dampingRatio = 0.5f, stiffness = 400f))
                    scale.animateTo(1f, tween(110))
                }
                wasFilled = filled
            }
            Icon(
                imageVector = if (filled) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (filled) activeColor else emptyColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            )
        }
    }
}

@Composable
fun StarRow(stars: Int, total: Int = 3, starSize: Int = 38, tint: Color = Gold) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val earned = index < stars
            val scale = remember { Animatable(if (earned) 0f else 1f) }
            LaunchedEffect(stars, index) {
                if (earned) {
                    scale.snapTo(0f)
                    delay(index * 130L)
                    scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
                } else {
                    scale.snapTo(1f)
                }
            }
            Icon(
                imageVector = if (earned) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(starSize.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            )
        }
    }
}

/** A dark neon HUD button: outlined plate, glowing icon, no elevation. */
@Composable
fun NeonIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 46
) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = NeonPanel.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(size.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size((size * 0.46f).dp)
            )
        }
    }
}

/**
 * A wide dark action button for the gameplay HUD, with an optional trailing
 * counter chip (used by the hint button).
 */
@Composable
fun NeonActionButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    val border = if (enabled) accent.copy(alpha = 0.65f) else NeonPanelSoft
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) accent.copy(alpha = 0.12f) else NeonPanel.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, border),
        modifier = modifier.height(52.dp).clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accent else NeonTextDim.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = if (enabled) NeonText else NeonTextDim.copy(alpha = 0.5f),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.2f)) {
                    Text(
                        text = trailing,
                        color = if (enabled) accent else NeonTextDim.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/** Compact dark HUD readout, e.g. arrows left or the pack name. */
@Composable
fun NeonChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NeonPanel.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = NeonTextDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Glowing progress bar that matches the neon board. */
@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan
) {
    Canvas(modifier = modifier.fillMaxWidth().height(10.dp)) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = NeonPanel.copy(alpha = 0.95f), cornerRadius = radius)
        val width = size.width * progress.coerceIn(0f, 1f)
        if (width > 1f) {
            drawRoundRect(
                color = accent.copy(alpha = 0.22f),
                size = Size(width + size.height, size.height),
                cornerRadius = radius
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(lerp(accent, NeonMagenta, 0.35f), accent)
                ),
                size = Size(width, size.height),
                cornerRadius = radius
            )
            drawCircle(
                color = NeonCore.copy(alpha = 0.75f),
                radius = size.height * 0.26f,
                center = Offset(width, size.height / 2f)
            )
        }
        drawRoundRect(
            color = NeonPanelSoft,
            cornerRadius = radius,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Coral,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp)
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Cloud,
    contentColor: Color = Ink
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/**
 * Centered result card for victory / failure / pause. Keeps the board visible
 * behind a scrim instead of replacing the whole screen.
 */
@Composable
fun ResultOverlay(
    title: String,
    modifier: Modifier = Modifier,
    stars: Int? = null,
    subtitle: String? = null,
    accent: Color = Coral,
    surfaceColor: Color = Color.White,
    titleColor: Color = Ink,
    subtitleColor: Color = InkSoft,
    secondaryContainerColor: Color = Cloud,
    secondaryContentColor: Color = Ink,
    starTint: Color = Gold,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null,
    quaternaryLabel: String? = null,
    onQuaternary: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = surfaceColor,
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth(0.86f)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = titleColor, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
            if (stars != null) {
                Spacer(Modifier.height(14.dp))
                StarRow(stars = stars, tint = starTint)
            }
            if (subtitle != null) {
                Spacer(Modifier.height(10.dp))
                Text(subtitle, color = subtitleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton(label = primaryLabel, onClick = onPrimary, containerColor = accent)
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    label = secondaryLabel,
                    onClick = onSecondary,
                    containerColor = secondaryContainerColor,
                    contentColor = secondaryContentColor
                )
            }
            if (tertiaryLabel != null && onTertiary != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    label = tertiaryLabel,
                    onClick = onTertiary,
                    containerColor = secondaryContainerColor,
                    contentColor = secondaryContentColor
                )
            }
            if (quaternaryLabel != null && onQuaternary != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    label = quaternaryLabel,
                    onClick = onQuaternary,
                    containerColor = secondaryContainerColor,
                    contentColor = secondaryContentColor
                )
            }
        }
    }
}

@Composable
fun DimOverlay(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}
