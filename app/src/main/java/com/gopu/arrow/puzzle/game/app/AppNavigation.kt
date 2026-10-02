package com.gopu.arrow.puzzle.game.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import com.gopu.arrow.puzzle.game.ads.defaultAdHost
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.ArrowTile
import com.gopu.arrow.puzzle.game.BoardPosition
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.levels.LEVELS_PER_PACK
import com.gopu.arrow.puzzle.game.levels.LevelRepository
import com.gopu.arrow.puzzle.game.ui.GameplayScreen
import com.gopu.arrow.puzzle.game.ui.HomeScreen
import com.gopu.arrow.puzzle.game.ui.LevelSelectScreen
import com.gopu.arrow.puzzle.game.ui.PackInfo
import com.gopu.arrow.puzzle.game.ui.SettingsScreen
import com.gopu.arrow.puzzle.game.ui.SplashHoldMillis
import com.gopu.arrow.puzzle.game.ui.SplashIntroMillis
import com.gopu.arrow.puzzle.game.ui.SplashOverlay
import com.gopu.arrow.puzzle.game.ui.SplashRevealMillis
import com.gopu.arrow.puzzle.game.ui.homeAlphaAt
import com.gopu.arrow.puzzle.game.ui.homeScaleAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private enum class AppScreen { HOME, LEVEL_SELECT, GAMEPLAY, SETTINGS }

private const val PACK_COUNT = 5

/**
 * Stand-in for pack-01-level-01 when the level assets cannot be read at all, so
 * the game still starts on a legal, solvable board. It matches the shape of the
 * real first level - 4x6 with ten arrows - and is trivially solvable: the two top
 * rows leave upwards, then the bottom pair leaves downwards.
 */
private val FallbackLevel = PuzzleLevel(
    id = "pack-01-level-01",
    pack = 1,
    order = 1,
    width = 4,
    height = 6,
    tiles = buildList {
        repeat(2) { row -> repeat(4) { column -> add(ArrowTile(BoardPosition(row, column), Direction.UP)) } }
        repeat(2) { column -> add(ArrowTile(BoardPosition(4, column), Direction.DOWN)) }
    },
    parMoves = 10
)

/**
 * Top-level screen routing for the game. Owns the current screen and the
 * selected level, and hands each screen its repository dependencies.
 *
 * The splash is not a screen in this switch: the menu is composed underneath it
 * from the first frame and the overlay dissolves off the top of it. That is what
 * removes the launch jerk — the menu is measured, laid out and drawn while the
 * brand is still on screen, so the handoff never composes, loads or measures
 * anything in the one frame where the change would be visible, and the two
 * layers cross-fade in the same ivory field instead of one background cutting to
 * another.
 */
@Composable
fun ArrowPuzzleApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var settingsReturn by remember { mutableStateOf(AppScreen.HOME) }
    val context = LocalContext.current
    val progressRepository = remember(context) { ProgressRepository(context) }
    val levelRepository = remember(context) { LevelRepository(context) }
    val adHost = remember(context) { defaultAdHost(context) }
    var currentLevel by remember {
        mutableStateOf(levelRepository.loadLevel(1, 1) ?: FallbackLevel)
    }

    val splashIntro = remember { Animatable(0f) }
    val splashReveal = remember { Animatable(0f) }
    var menuLaidOut by remember { mutableStateOf(false) }
    var splashMounted by remember { mutableStateOf(true) }

    /*
     * Nothing starts moving until the menu underneath has been measured and
     * drawn. Waiting on a real frame rather than on a timer is what keeps the
     * first animated frame the frame the user sees, on a fast device and a slow
     * one alike: a slow device has simply reached this point later, not later in
     * the animation.
     */
    LaunchedEffect(Unit) {
        snapshotFlow { menuLaidOut }.first { it }
        withFrameNanos { }
        splashIntro.animateTo(1f, tween(SplashIntroMillis, easing = LinearOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        delay(SplashHoldMillis)
        snapshotFlow { menuLaidOut }.first { it }
        withFrameNanos { }
        splashReveal.animateTo(1f, tween(SplashRevealMillis, easing = FastOutSlowInEasing))
        // Both layers have reached their end state, so leaving the composition
        // here is invisible and the menu stops being drawn twice.
        splashMounted = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { menuLaidOut = true }
                    .graphicsLayer {
                        alpha = homeAlphaAt(splashReveal.value)
                        val scale = homeScaleAt(splashReveal.value)
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                when (screen) {
                    AppScreen.HOME -> HomeScreen(
                        progressRepository = progressRepository,
                        onPlay = { screen = AppScreen.LEVEL_SELECT },
                        onSettings = {
                            settingsReturn = AppScreen.HOME
                            screen = AppScreen.SETTINGS
                        },
                        ads = adHost
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        progressRepository = progressRepository,
                        onBack = { screen = settingsReturn }
                    )
                    AppScreen.LEVEL_SELECT -> {
                        val packs = remember(levelRepository) {
                            (1..PACK_COUNT).map { packNumber ->
                                PackInfo(
                                    packNumber = packNumber,
                                    levelCount = LEVELS_PER_PACK,
                                    levels = levelRepository.loadAllLevels(packNumber, LEVELS_PER_PACK)
                                )
                            }
                        }
                        LevelSelectScreen(
                            packs = packs,
                            progressRepository = progressRepository,
                            onLevelClick = { level ->
                                currentLevel = level
                                screen = AppScreen.GAMEPLAY
                            },
                            onBack = { screen = AppScreen.HOME },
                            onSettings = {
                                settingsReturn = AppScreen.LEVEL_SELECT
                                screen = AppScreen.SETTINGS
                            },
                            ads = adHost
                        )
                    }
                    AppScreen.GAMEPLAY -> {
                        val nextLevel = remember(currentLevel) {
                            levelRepository.loadLevel(currentLevel.pack, currentLevel.order + 1)
                                ?: levelRepository.loadLevel(currentLevel.pack + 1, 1)
                        }
                        GameplayScreen(
                            level = currentLevel,
                            progressRepository = progressRepository,
                            hasNextLevel = nextLevel != null,
                            onNextLevel = { nextLevel?.let { currentLevel = it } },
                            onBackToSelect = { screen = AppScreen.LEVEL_SELECT },
                            ads = adHost
                        )
                    }
                }
            }

            SplashLayer(
                intro = splashIntro,
                reveal = splashReveal,
                mounted = splashMounted
            )
        }
    }
}

/**
 * Holds the splash overlay, and unmounts it once the handoff has finished.
 *
 * This is its own composable on purpose: the unmount invalidates only this scope,
 * so leaving the composition at the end of the handoff cannot recompose the menu
 * that is sitting underneath it.
 */
@Composable
private fun SplashLayer(
    intro: Animatable<Float, AnimationVector1D>,
    reveal: Animatable<Float, AnimationVector1D>,
    mounted: Boolean
) {
    if (mounted) {
        SplashOverlay(intro = intro, reveal = reveal, modifier = Modifier.fillMaxSize())
    }
}