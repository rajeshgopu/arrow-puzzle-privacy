package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.ui.theme.AmberGold
import com.gopu.arrow.puzzle.game.ui.theme.CardSurface
import com.gopu.arrow.puzzle.game.ui.theme.CharcoalDeep
import com.gopu.arrow.puzzle.game.ui.theme.Cloud
import com.gopu.arrow.puzzle.game.ui.theme.Coral
import com.gopu.arrow.puzzle.game.ui.theme.DisplaySerif
import com.gopu.arrow.puzzle.game.ui.theme.Ember
import com.gopu.arrow.puzzle.game.ui.theme.Gold
import com.gopu.arrow.puzzle.game.ui.theme.Ink
import com.gopu.arrow.puzzle.game.ui.theme.InkSoft
import com.gopu.arrow.puzzle.game.ui.theme.NeonCore
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.NeonMagenta
import com.gopu.arrow.puzzle.game.ui.theme.NeonPanel
import com.gopu.arrow.puzzle.game.ui.theme.NeonPanelSoft
import com.gopu.arrow.puzzle.game.ui.theme.NeonText
import com.gopu.arrow.puzzle.game.ui.theme.NeonTextDim
import com.gopu.arrow.puzzle.game.ui.theme.PlayBottom
import com.gopu.arrow.puzzle.game.ui.theme.PlayMid
import com.gopu.arrow.puzzle.game.ui.theme.PlayTop
import com.gopu.arrow.puzzle.game.ui.theme.Rust
import com.gopu.arrow.puzzle.game.ui.theme.StarGold
import com.gopu.arrow.puzzle.game.ui.theme.TealInk
import com.gopu.arrow.puzzle.game.ui.theme.TealMuted
import com.gopu.arrow.puzzle.game.ui.theme.UiSans
import kotlinx.coroutines.delay

/**
 * Two-line ARROW PUZZLE wordmark used on the splash and menu screens.
 *
 * Both lines are set in Playfair Display Bold: ARROW fades from deep charcoal
 * into rust, PUZZLE from coral into gold, so the wordmark reads as a logo rather
 * than as UI copy. Tracking and line height are driven off [fontSize] to keep
 * the two lines tight and optically centred at any size.
 */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    fontSize: Int = 52,
    tracking: Float = 0.05f
) {
    val base = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        letterSpacing = (fontSize * tracking).sp,
        lineHeight = (fontSize * 0.98f).sp
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "ARROW",
            style = base.copy(
                brush = Brush.verticalGradient(listOf(CharcoalDeep, Rust))
            )
        )
        Text(
            text = "PUZZLE",
            style = base.copy(
                brush = Brush.verticalGradient(listOf(Ember, AmberGold))
            )
        )
    }
}

/**
 * Menu stat card: small tracked label over a much larger number, on an
 * off-white card with a soft, slightly cool shadow.
 */
@Composable
fun ProgressCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showStar: Boolean = false
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        color = CardSurface,
        modifier = modifier.shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = TealInk.copy(alpha = 0.10f),
            spotColor = TealInk.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = TealMuted,
                fontFamily = UiSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = TealInk,
                    fontFamily = UiSans,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                if (showStar) {
                    Spacer(Modifier.width(7.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

/**
 * Small rounded-square glass button for the menu corner. White plate, hairline
 * border, top sheen and an ambient shadow, which is all the depth it needs.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TealInk,
    size: Int = 48
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        shape = shape,
        color = Color.White,
        modifier = modifier
            .size(size.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = TealInk.copy(alpha = 0.12f),
                spotColor = TealInk.copy(alpha = 0.18f)
            )
            .border(1.dp, TealInk.copy(alpha = 0.06f), shape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick, modifier = Modifier.size(size.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

/**
 * The menu's primary action: a wide coral pill with a white play glyph, a top
 * sheen and a single soft shadow so it is unmistakably the strongest element.
 */
@Composable
fun PlayButton(
    label: String = "PLAY",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    Surface(
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = PlayBottom.copy(alpha = 0.26f),
                spotColor = PlayBottom.copy(alpha = 0.32f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.horizontalGradient(listOf(PlayTop, PlayMid, PlayBottom))
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.07f)
                            )
                        )
                    )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayGlyph(modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontFamily = UiSans,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
            }
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
