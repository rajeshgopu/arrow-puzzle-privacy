package com.gopu.arrowpuzzle.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.gopu.arrowpuzzle.ads.defaultAdHost
import com.gopu.arrowpuzzle.data.ProgressRepository
import com.gopu.arrowpuzzle.game.ArrowTile
import com.gopu.arrowpuzzle.game.BoardPosition
import com.gopu.arrowpuzzle.game.Direction
import com.gopu.arrowpuzzle.game.PuzzleLevel
import com.gopu.arrowpuzzle.levels.LEVELS_PER_PACK
import com.gopu.arrowpuzzle.levels.LevelRepository
import com.gopu.arrowpuzzle.ui.GameplayScreen
import com.gopu.arrowpuzzle.ui.HomeScreen
import com.gopu.arrowpuzzle.ui.LevelSelectScreen
import com.gopu.arrowpuzzle.ui.PackInfo
import com.gopu.arrowpuzzle.ui.SettingsScreen
import com.gopu.arrowpuzzle.ui.SplashScreen

private enum class AppScreen { SPLASH, HOME, LEVEL_SELECT, GAMEPLAY, SETTINGS }

private const val PACK_COUNT = 5

private val FallbackLevel = PuzzleLevel(
    id = "pack-01-level-01",
    pack = 1,
    order = 1,
    width = 4,
    height = 4,
    tiles = buildList {
        repeat(4) { column -> add(ArrowTile(BoardPosition(0, column), Direction.UP)) }
        repeat(4) { column -> add(ArrowTile(BoardPosition(1, column), Direction.UP)) }
        repeat(4) { column -> add(ArrowTile(BoardPosition(2, column), Direction.DOWN)) }
        repeat(4) { column -> add(ArrowTile(BoardPosition(3, column), Direction.DOWN)) }
    },
    parMoves = 16
)

/**
 * Top-level screen routing for the game. Owns the current screen and the
 * selected level, and hands each screen its repository dependencies.
 */
@Composable
fun ArrowPuzzleApp() {
    var screen by remember { mutableStateOf(AppScreen.SPLASH) }
    var settingsReturn by remember { mutableStateOf(AppScreen.HOME) }
    val context = LocalContext.current
    val progressRepository = remember(context) { ProgressRepository(context) }
    val levelRepository = remember(context) { LevelRepository(context) }
    val adHost = remember(context) { defaultAdHost(context) }
    val bannerAvailable by adHost.bannerAvailable.collectAsState(initial = false)
    var currentLevel by remember {
        mutableStateOf(levelRepository.loadLevel(1, 1) ?: FallbackLevel)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            AppScreen.SPLASH -> SplashScreen(onFinished = { screen = AppScreen.HOME })
            AppScreen.HOME -> HomeScreen(
                progressRepository = progressRepository,
                onPlay = { screen = AppScreen.LEVEL_SELECT },
                onSettings = {
                    settingsReturn = AppScreen.HOME
                    screen = AppScreen.SETTINGS
                },
                showBanner = bannerAvailable
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
                    showBanner = bannerAvailable
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
}
