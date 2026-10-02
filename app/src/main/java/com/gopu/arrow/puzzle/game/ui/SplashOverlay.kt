package com.gopu.arrow.puzzle.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.components.ArrowMark
import com.gopu.arrow.puzzle.game.ui.components.Wordmark
import com.gopu.arrow.puzzle.game.ui.theme.Ivory
import com.gopu.arrow.puzzle.game.ui.theme.IvoryBottom
import com.gopu.arrow.puzzle.game.ui.theme.IvoryTop
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan

/**
 * The splash, drawn as an overlay on top of the menu that is already composed
 * behind it rather than as a screen that gets swapped for one.
 *
 * It owns no timing of its own: [intro] and [reveal] are driven by the app
 * shell, and both are only ever read inside `graphicsLayer` blocks, so a frame
 * of the handoff repaints two layers and recomposes nothing.
 *
 * The field is painted at full strength from the very first frame, in the same
 * ivory gradient the menu backdrop draws and in the same colour as the window
 * background, so the launch never flashes a different colour on its way in and
 * the cross-fade on its way out has nothing to change.
 */
@Composable
fun SplashOverlay(
    intro: Animatable<Float, AnimationVector1D>,
    reveal: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(IvoryTop, Ivory, IvoryBottom)))
                .graphicsLayer { alpha = splashFieldAlphaAt(reveal.value) }
        )

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
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(200.dp)) {
                    ArrowMark(
                        direction = Direction.RIGHT,
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        color = NeonCyan,
                        glow = true
                    )
                }

                Spacer(Modifier.height(30.dp))

                Wordmark(fontSize = 44)
            }
        }
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