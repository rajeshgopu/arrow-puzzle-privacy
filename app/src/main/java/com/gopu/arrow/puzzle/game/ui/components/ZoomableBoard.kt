package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gopu.arrow.puzzle.game.R

/**
 * Wraps the board with pinch-to-zoom and drag-to-pan so larger grids stay
 * readable and tappable. Single taps still reach the tiles: transform gestures
 * only consume once a pan/zoom exceeds touch slop. Zoom resets automatically
 * when [resetKey] changes (a new level), and a small control appears while
 * zoomed to return to 1:1.
 */
@Composable
fun ZoomableBoard(
    modifier: Modifier = Modifier,
    resetKey: Any? = Unit,
    minScale: Float = 1f,
    maxScale: Float = 4f,
    content: @Composable () -> Unit
) {
    var scale by remember(resetKey) { mutableStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    fun clamped(target: Offset, atScale: Float): Offset {
        val maxX = ((atScale - 1f) * boxSize.width) / 2f
        val maxY = ((atScale - 1f) * boxSize.height) / 2f
        return Offset(target.x.coerceIn(-maxX, maxX), target.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { boxSize = it }
            .pointerInput(resetKey) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    scale = newScale
                    offset = if (newScale <= minScale + 0.001f) {
                        Offset.Zero
                    } else {
                        clamped(offset + pan, newScale)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            content()
        }

        if (scale > minScale + 0.01f) {
            RoundIconButton(
                icon = Icons.Default.ZoomOutMap,
                contentDescription = stringResource(R.string.a11y_reset_zoom),
                onClick = {
                    scale = minScale
                    offset = Offset.Zero
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            )
        }
    }
}
