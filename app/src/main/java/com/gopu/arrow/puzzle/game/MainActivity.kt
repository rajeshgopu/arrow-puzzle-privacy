package com.gopu.arrow.puzzle.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gopu.arrow.puzzle.game.app.ArrowPuzzleApp
import com.gopu.arrow.puzzle.game.ads.AdConsent
import com.gopu.arrow.puzzle.game.ui.theme.ArrowPuzzleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveMode()
        setContent { ArrowPuzzleTheme { ArrowPuzzleApp() } }
    }

    /**
     * Consent is re-read on every foreground pass rather than once at launch,
     * so a form answered (or left open) across a pause still releases the ad
     * gate, and a declined player is asked again next time they open the app.
     */
    override fun onStart() {
        super.onStart()
        AdConsent.request(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    private fun applyImmersiveMode() {
        runCatching {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
