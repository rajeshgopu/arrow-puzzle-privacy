package com.gopu.arrow.puzzle.game.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ui.components.ArrowMark
import com.gopu.arrow.puzzle.game.ui.components.Wordmark
import com.gopu.arrow.puzzle.game.ui.theme.CanvasWhite
import com.gopu.arrow.puzzle.game.ui.theme.Cloud
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import kotlinx.coroutines.delay

/**
 * Static intro that shows the wordmark and arrow, then hands off to Home. No
 * motion or sound: it only waits a beat so the brand is visible.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CanvasWhite, Cloud)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
