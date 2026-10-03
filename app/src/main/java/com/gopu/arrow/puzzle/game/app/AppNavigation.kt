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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.gopu.arrow.puzzle.game.ads.AdHost
import com.gopu.arrow.puzzle.game.ads.defaultAdHost
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.i18n.LocalAppContext
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
import kotlinx.coroutines.withTimeoutOrNull

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
 * Draws two screens at once while a swap is in flight, and cross-fades between
 * them.
 *
 * The destination is composed and measured on the same frame the swap starts, so
 * its cost is paid straight away; the outgoing screen is drawn underneath it and
 * still fully opaque, and [onDestinationReady] reports when the destination has
 * been laid out so the caller can hold the outgoing screen at full strength until
 * then. The outgoing screen also swallows touches, because for the length of the
 * swap it is still in the tree under a destination that may not be opaque yet.
 *
 * Both layers read the one clock inside a `graphicsLayer`, so a frame of the
 * swap repaints two layers and recomposes nothing.
 *
 * Only a screen that is genuinely arriving is driven by the incoming curve: that
 * curve is zero at rest, which is what a destination has to start at, and the
 * menu on show after the splash has to start at one. [screenAlphaAt] and
 * [screenScaleAt] are told which case they are in rather than inferring it from
 * the clock, so a screen that is simply sitting there is never faded out by a
 * clock that has not started.
 */
@Composable
private fun ScreenHost(
    screen: AppScreen,
    exiting: AppScreen?,
    swapId: Int,
    progress: Animatable<Float, AnimationVector1D>,
    onDestinationReady: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (AppScreen) -> Unit
) {
    Box(modifier = modifier) {
        if (exiting != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = screenOutAlphaAt(progress.value)
                        val scale = screenOutScaleAt(progress.value)
                        scaleX = scale
                        scaleY = scale
                    }
                    .swallowsTouches()
            ) {
                content(exiting)
            }
        }

        /*
         * Keyed by the swap, and this is load bearing rather than tidiness: the
         * destination sits at the same slot every time, so without the key it is
         * the same layout node on every swap, and onSizeChanged only reports a
         * size *change*. A full-size destination therefore never reports itself
         * ready again after the very first layout, and every swap would sit on
         * the backstop before it faded. The key makes each destination a new node,
         * so it measures from zero every time and reports itself ready for the
         * swap it belongs to - and nothing else.
         */
        key(swapId) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { onDestinationReady() }
                    .graphicsLayer {
                        val swapping = exiting != null
                        alpha = screenAlphaAt(progress.value, swapping)
                        val scale = screenScaleAt(progress.value, swapping)
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                content(screen)
            }
        }
    }
}

/**
 * One screen's content. Both screens of a swap are built by this, so a screen
 * that is being left is built exactly as it was.
 *
 * Being rebuilt in a new slot means the outgoing screen gets fresh state, which
 * is safe because it is on its way out and is opaque under a cross-fade while it
 * happens: nothing it starts is visible, and its one-off effects are all guarded
 * on state a fresh instance cannot reach (the clear celebration only fires from
 * `WON`, which only a board that has been played can be).
 */
@Composable
private fun Screen(
    screen: AppScreen,
    progressRepository: ProgressRepository,
    packs: List<PackInfo>,
    currentLevel: PuzzleLevel,
    onNavigate: (AppScreen) -> Unit,
    onOpenSettings: (AppScreen) -> Unit,
    onBackFromSettings: () -> Unit,
    onLevelClick: (PuzzleLevel) -> Unit,
    onPlayLevel: (Int) -> Unit,
    onNextLevel: (PuzzleLevel?) -> Unit,
    levelRepository: LevelRepository,
    adHost: AdHost
) {
    when (screen) {
        AppScreen.HOME -> HomeScreen(
            progressRepository = progressRepository,
            onPlay = { onNavigate(AppScreen.LEVEL_SELECT) },
            onSettings = { onOpenSettings(AppScreen.HOME) },
            onPlayLevel = onPlayLevel,
            ads = adHost
        )
        AppScreen.SETTINGS -> SettingsScreen(
            progressRepository = progressRepository,
            onBack = onBackFromSettings
        )
        AppScreen.LEVEL_SELECT -> LevelSelectScreen(
            packs = packs,
            progressRepository = progressRepository,
            onLevelClick = onLevelClick,
            onBack = { onNavigate(AppScreen.HOME) },
            onSettings = { onOpenSettings(AppScreen.LEVEL_SELECT) },
            ads = adHost
        )
        AppScreen.GAMEPLAY -> {
            val nextLevel = remember(currentLevel) {
                levelRepository.loadLevel(currentLevel.pack, currentLevel.order + 1)
                    ?: levelRepository.loadLevel(currentLevel.pack + 1, 1)
            }
            GameplayScreen(
                level = currentLevel,
                progressRepository = progressRepository,
                hasNextLevel = nextLevel != null,
                onNextLevel = { nextLevel?.let(onNextLevel) },
                onBackToSelect = { onNavigate(AppScreen.LEVEL_SELECT) },
                ads = adHost
            )
        }
    }
}

/**
 * Eats every touch that lands on the outgoing screen while a swap is in flight,
 * so a second tap on the screen being left cannot navigate out from under the
 * one already on its way. The modifier leaves with the screen, so the
 * destination is fully interactive again the moment the swap ends.
 */
private fun Modifier.swallowsTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

/**
 * Top-level screen routing for the game. Owns the current screen and the
 * selected level, and hands each screen its repository dependencies.
 *
 * A swap keeps both screens composed for the length of the cross-fade and holds
 * the one being left on top until the one being arrived at has been measured and
 * drawn, so the player never sees a blank window between two screens - see
 * [ScreenTransition.kt] for why that is the only way to avoid one.
 *
 * The level packs are loaded here, at the top of the app, rather than inside the
 * level-select branch. That puts the JSON work on the launch, behind the splash,
 * instead of on the frame the player taps Play, which is the difference between
 * the level select being ready to draw immediately and it being ready a moment
 * later.
 *
 * The splash is not a screen in this switch: the menu is composed underneath it
 * from the first frame and the overlay dissolves off the top of it, so the handoff
 * never composes, loads or measures anything in the one frame where the change
 * would be visible.
 */
@Composable
fun ArrowPuzzleApp(progressRepository: ProgressRepository) {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var settingsReturn by remember { mutableStateOf(AppScreen.HOME) }
    var exiting by remember { mutableStateOf<AppScreen?>(null) }
    var swapId by remember { mutableIntStateOf(0) }
    var destinationReady by remember { mutableStateOf(false) }
    /*
     * The app context, not the localised one. A language change hands the tree a
     * different Context, and rebuilding the ad host when that happens would tear
     * down a banner view and a consent gate over a settings change - so the
     * repository and the ad host are keyed on the activity that will outlive it.
     */
    val context = LocalAppContext.current
    val levelRepository = remember(context) { LevelRepository(context) }
    val adHost = remember(context) { defaultAdHost(context) }
    val packs = remember(levelRepository) {
        (1..PACK_COUNT).map { packNumber ->
            PackInfo(
                packNumber = packNumber,
                levelCount = LEVELS_PER_PACK,
                levels = levelRepository.loadAllLevels(packNumber, LEVELS_PER_PACK)
            )
        }
    }
    var currentLevel by remember {
        mutableStateOf(levelRepository.loadLevel(1, 1) ?: FallbackLevel)
    }

    val splashIntro = remember { Animatable(0f) }
    val splashReveal = remember { Animatable(0f) }
    var menuLaidOut by remember { mutableStateOf(false) }
    var splashMounted by remember { mutableStateOf(true) }

    /*
     * One clock per swap rather than one clock for the app. It is rebuilt at zero
     * on the same frame the swap starts, so the destination is already on screen
     * at no opacity and no fill can flash it at full strength for a frame; and
     * because it is a new object, the effect below is cancelled and restarted by
     * the swap id, so a second tap mid-swap never leaves two clocks running.
     */
    val swap = remember(swapId) { Animatable(0f) }

    /*
     * A swap does not start its clock until the destination has been measured and
     * one real frame has been produced. Waiting on a real frame rather than on a
     * timer is what keeps the outgoing screen exactly as the player left it for
     * as long as the destination needs, on a fast device and a slow one alike.
     * The timeout is a backstop only: a full-size destination always reports its
     * size within a frame.
     */
    LaunchedEffect(swapId) {
        if (swapId == 0) return@LaunchedEffect
        withTimeoutOrNull(ScreenReadyTimeoutMillis) {
            snapshotFlow { destinationReady }.first { it }
        }
        withFrameNanos { }
        swap.animateTo(1f, tween(ScreenSwapMillis, easing = FastOutSlowInEasing))
        // The destination is opaque again, so leaving the outgoing screen here is
        // invisible and the app stops drawing two screens.
        exiting = null
    }

    /*
     * Starts a swap. Re-arming the clock is a fresh object rather than a snap, so
     * there is no moment between the tap and the swap starting in which the
     * destination could be drawn at the end of the previous transition, and
     * swapping to the screen that is already up does nothing at all.
     */
    fun navigateTo(target: AppScreen) {
        if (target == screen) return
        exiting = screen
        screen = target
        destinationReady = false
        swapId++
    }

    /*
     * The menu's level plate: straight into the level it reports. Loads the level
     * the same way the level select does, falling back to the first level rather
     * than dropping the player on a board that was never read.
     */
    fun openLevelOrder(order: Int) {
        val bounded = order.coerceIn(1, PACK_COUNT * LEVELS_PER_PACK)
        val pack = (bounded - 1) / LEVELS_PER_PACK + 1
        val within = (bounded - 1) % LEVELS_PER_PACK + 1
        currentLevel = levelRepository.loadLevel(pack, within)
            ?: levelRepository.loadLevel(1, 1)
            ?: FallbackLevel
        navigateTo(AppScreen.GAMEPLAY)
    }

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
            ScreenHost(
                screen = screen,
                exiting = exiting,
                swapId = swapId,
                progress = swap,
                onDestinationReady = { destinationReady = true },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { menuLaidOut = true }
                    .graphicsLayer {
                        alpha = homeAlphaAt(splashReveal.value)
                        val scale = homeScaleAt(splashReveal.value)
                        scaleX = scale
                        scaleY = scale
                    }
            ) { target ->
                Screen(
                    screen = target,
                    progressRepository = progressRepository,
                    packs = packs,
                    currentLevel = currentLevel,
                    onNavigate = { next -> navigateTo(next) },
                    onOpenSettings = { from ->
                        settingsReturn = from
                        navigateTo(AppScreen.SETTINGS)
                    },
                    onBackFromSettings = { navigateTo(settingsReturn) },
                    onLevelClick = { level ->
                        currentLevel = level
                        navigateTo(AppScreen.GAMEPLAY)
                    },
                    onPlayLevel = { order -> openLevelOrder(order) },
                    onNextLevel = { level -> level?.let { currentLevel = it } },
                    levelRepository = levelRepository,
                    adHost = adHost
                )
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