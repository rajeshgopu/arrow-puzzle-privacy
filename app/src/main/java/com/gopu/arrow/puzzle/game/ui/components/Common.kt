package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.ui.theme.Cloud
import com.gopu.arrow.puzzle.game.ui.theme.Coral
import com.gopu.arrow.puzzle.game.ui.theme.Gold
import com.gopu.arrow.puzzle.game.ui.theme.Ink
import com.gopu.arrow.puzzle.game.ui.theme.InkSoft
import kotlinx.coroutines.delay

/**
 * Stylised two-line wordmark used on the splash and home screens: a heavy
 * italic serif with a vertical gradient, so the title reads as a logo rather
 * than plain text.
 */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    fontSize: Int = 46,
    letterSpacing: Int = 5
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "ARROW",
            style = TextStyle(
                brush = Brush.verticalGradient(listOf(Ink, Coral)),
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
                fontSize = fontSize.sp,
                letterSpacing = letterSpacing.sp
            )
        )
        Text(
            text = "PUZZLE",
            style = TextStyle(
                brush = Brush.verticalGradient(listOf(Coral, Gold)),
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
                fontSize = fontSize.sp,
                letterSpacing = letterSpacing.sp
            )
        )
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
fun LivesRow(lives: Int, total: Int = 3, modifier: Modifier = Modifier) {
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
                tint = if (filled) Coral else Cloud,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            )
        }
    }
}

@Composable
fun StarRow(stars: Int, total: Int = 3, starSize: Int = 38) {
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
                tint = Gold,
                modifier = Modifier
                    .size(starSize.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            )
        }
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Cloud, contentColor = Ink)
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
        color = Color.White,
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth(0.86f)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
            if (stars != null) {
                Spacer(Modifier.height(14.dp))
                StarRow(stars = stars)
            }
            if (subtitle != null) {
                Spacer(Modifier.height(10.dp))
                Text(subtitle, color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton(label = primaryLabel, onClick = onPrimary, containerColor = accent)
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(label = secondaryLabel, onClick = onSecondary)
            }
            if (tertiaryLabel != null && onTertiary != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(label = tertiaryLabel, onClick = onTertiary)
            }
            if (quaternaryLabel != null && onQuaternary != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(label = quaternaryLabel, onClick = onQuaternary)
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
