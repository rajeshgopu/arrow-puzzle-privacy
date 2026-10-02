package com.gopu.arrow.puzzle.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.GameStatus
import com.gopu.arrow.puzzle.game.PuzzleLevel
import com.gopu.arrow.puzzle.game.PuzzleReducer
import com.gopu.arrow.puzzle.game.step
import com.gopu.arrow.puzzle.game.ads.AdHost
import com.gopu.arrow.puzzle.game.ads.NoAds
import com.gopu.arrow.puzzle.game.ads.ReservedBannerAdSlot
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.levels.LEVELS_PER_PACK
import com.gopu.arrow.puzzle.game.starsForInvalidTaps
import com.gopu.arrow.puzzle.game.ui.components.ArrowBlock
import com.gopu.arrow.puzzle.game.ui.components.ArrowBoard
import com.gopu.arrow.puzzle.game.ui.components.ArrowLaunch
import com.gopu.arrow.puzzle.game.ui.components.DimOverlay
import com.gopu.arrow.puzzle.game.ui.components.LivesRow
import com.gopu.arrow.puzzle.game.ui.components.LaunchAnimationStyle
import com.gopu.arrow.puzzle.game.ui.components.NeonActionButton
import com.gopu.arrow.puzzle.game.ui.components.NeonChip
import com.gopu.arrow.puzzle.game.ui.components.NeonIconButton
import com.gopu.arrow.puzzle.game.ui.components.NeonProgressBar
import com.gopu.arrow.puzzle.game.ui.components.ResultOverlay
import com.gopu.arrow.puzzle.game.ui.components.StarClearCelebration
import com.gopu.arrow.puzzle.game.ui.components.TileHighlight
import com.gopu.arrow.puzzle.game.ui.components.ZoomableBoard
import com.gopu.arrow.puzzle.game.ui.theme.Gold
import com.gopu.arrow.puzzle.game.ui.theme.NeonAmber
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.NeonGreen
import com.gopu.arrow.puzzle.game.ui.theme.NeonLime
import com.gopu.arrow.puzzle.game.ui.theme.NeonMagenta
import com.gopu.arrow.puzzle.game.ui.theme.NeonPanel
import com.gopu.arrow.puzzle.game.ui.theme.NeonRed
import com.gopu.arrow.puzzle.game.ui.theme.NeonText
import com.gopu.arrow.puzzle.game.ui.theme.NeonTextDim
import com.gopu.arrow.puzzle.game.ui.theme.NeonViolet
import com.gopu.arrow.puzzle.game.ui.theme.StageDeep
import com.gopu.arrow.puzzle.game.ui.theme.StageVoid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private enum class TutorialStage { PREPARE, FIRST_MOVE, COMPLETE }

private const val HINTS_PER_LEVEL = 3

/**
 * The game screen: a neon HUD over the glowing maze, with pause, hint, and
 * victory/failure overlays layered on top. All moves are dispatched to the pure
 * [PuzzleReducer]; this composable only renders state and plays feedback, so the
 * level and progress logic is untouched by the presentation work.
 */
@Composable
fun GameplayScreen(
    level: PuzzleLevel,
    progressRepository: ProgressRepository,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onBackToSelect: () -> Unit,
    ads: AdHost = AdHost(NoAds)
) {
    var gameState by remember(level) { mutableStateOf(PuzzleReducer.start(level)) }
    val scope = rememberCoroutineScope()
    val hapticsEnabled by progressRepository.hapticsEnabled.collectAsState(initial = true)
    val haptics = rememberHaptics(hapticsEnabled)
    val soundEnabled by progressRepository.soundEnabled.collectAsState(initial = true)
    val sounds = rememberSounds(soundEnabled)
    // Presentation only, and read at the moment an arrow is launched, so a change
    // made from the settings overlay applies to the next tap without a restart and
    // never reaches the reducer, the hit test or the clear condition.
    val launchStyle by progressRepository.launchAnimation
        .collectAsState(initial = LaunchAnimationStyle.Default)
    val rewardedAvailable by ads.rewardedAvailable.collectAsState(initial = false)
    val tutorialSeen by progressRepository.tutorialSeen.collectAsState(initial = false)
    val isIntroLevel = level.pack == 1 && level.order == 1
    val showTutorial = isIntroLevel && !tutorialSeen

    // Every signal below is keyed by tile index, so feedback follows the whole
    // arrow instead of the cell that happened to be tapped.
    val firstMoveTileIndex = remember(level) {
        PuzzleReducer.firstValidMove(PuzzleReducer.start(level))?.let(level::ownerAt)
    }
    var tutorialStage by remember(level, showTutorial) {
        mutableStateOf(if (showTutorial) TutorialStage.PREPARE else TutorialStage.COMPLETE)
    }
    var block by remember(level) { mutableStateOf<ArrowBlock?>(null) }
    var departure by remember(level) { mutableStateOf<ArrowLaunch?>(null) }
    var statusMessage by remember(level) { mutableStateOf<String?>(null) }
    var paused by remember(level) { mutableStateOf(false) }
    var showSettings by remember(level) { mutableStateOf(false) }
    var hintsRemaining by remember(level) { mutableStateOf(HINTS_PER_LEVEL) }
    var hintTileIndex by remember(level) { mutableStateOf<Int?>(null) }
    var rewardedUsed by remember(level) { mutableStateOf(false) }
    // The celebration is a presentation gate only: the result card waits for it,
    // nothing about the level, the star score or the arrows changes.
    var celebrationStars by remember(level) { mutableStateOf<Int?>(null) }
    var resultReady by remember(level) { mutableStateOf(false) }
    var winExit by remember(level) { mutableStateOf<CompletableDeferred<Unit>?>(null) }
    var winTileIndex by remember(level) { mutableStateOf(-1) }

    val totalArrows = level.tiles.size
    // remainingTiles holds board cells, not arrows: a bent arrow owns several.
    // Count arrows that still have at least one cell left, or the counters
    // below mix two units and go negative on any level with multi-cell arrows.
    val remainingArrows = level.tiles.count { tile ->
        tile.cells.any { it in gameState.remainingTiles }
    }
    val cleared = totalArrows - remainingArrows
    val progress by animateFloatAsState(
        targetValue = if (totalArrows == 0) 1f else cleared.toFloat() / totalArrows,
        animationSpec = tween(350),
        label = "progress"
    )

    val restartGame = {
        gameState = PuzzleReducer.restart(gameState)
        tutorialStage = if (showTutorial) TutorialStage.PREPARE else TutorialStage.COMPLETE
        block = null
        departure = null
        statusMessage = null
        hintTileIndex = null
        hintsRemaining = HINTS_PER_LEVEL
        rewardedUsed = false
        paused = false
        celebrationStars = null
        resultReady = false
        winExit = null
        winTileIndex = -1
    }

    val earnedStars = if (rewardedUsed) 1 else starsForInvalidTaps(gameState.invalidTaps)

    LaunchedEffect(gameState.status, level.id) {
        if (gameState.status == GameStatus.WON) {
            progressRepository.unlockLevel(
                levelId = level.id,
                stars = earnedStars,
                levelOrder = (level.pack - 1) * LEVELS_PER_PACK + level.order + 1
            )
            haptics.celebrate()
            sounds.celebrate()
        }
    }

    /*
     * The clear celebration starts once the final arrow has actually left the
     * board and the screen has had a beat to settle: 150ms of quiet, then the
     * tier that matches the star score. The timeout is only a safety net, so a
     * dropped launch callback can never strand the player without a result.
     */
    LaunchedEffect(winExit) {
        val exit = winExit ?: return@LaunchedEffect
        withTimeoutOrNull(1_500L) { exit.await() }
        delay(150)
        if (gameState.status == GameStatus.WON) celebrationStars = earnedStars
    }

    BackHandler {
        when {
            paused -> paused = false
            gameState.status == GameStatus.PLAYING -> paused = true
            else -> onBackToSelect()
        }
    }

    val boardEnabled = !paused &&
        tutorialStage != TutorialStage.PREPARE &&
        gameState.status == GameStatus.PLAYING

    val statusTone = when {
        statusMessage != null -> NeonRed
        gameState.status == GameStatus.WON -> NeonGreen
        gameState.status == GameStatus.LOST -> NeonRed
        tutorialStage == TutorialStage.PREPARE -> NeonAmber
        tutorialStage == TutorialStage.FIRST_MOVE -> NeonCyan
        else -> NeonTextDim
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(StageVoid, StageDeep, StageVoid)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan,
                    onClick = onBackToSelect
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "LEVEL ${level.order}",
                        color = NeonText,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "PACK ${level.pack}",
                        color = NeonTextDim,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonViolet,
                        onClick = { showSettings = true }
                    )
                    NeonIconButton(
                        icon = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = NeonMagenta,
                        onClick = { paused = true }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LivesRow(
                    lives = gameState.lives,
                    activeColor = NeonRed,
                    emptyColor = NeonPanel
                )
                NeonChip(label = "ARROWS", value = "$remainingArrows")
                NeonChip(label = "MOVES", value = "$cleared", accent = NeonLime)
            }

            Spacer(Modifier.height(10.dp))

            NeonProgressBar(progress = progress, accent = NeonCyan)

            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                targetState = when {
                    gameState.status == GameStatus.WON -> "MAZE CLEARED"
                    gameState.status == GameStatus.LOST -> "OUT OF LIVES"
                    tutorialStage == TutorialStage.PREPARE -> "TAP START TO BEGIN"
                    tutorialStage == TutorialStage.FIRST_MOVE -> "TAP THE GLOWING ARROW"
                    statusMessage != null -> statusMessage!!
                    else -> "TAP ANY ARROW WITH A CLEAR ROUTE"
                },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "status"
            ) { message ->
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    color = statusTone,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // The board takes the whole play area rather than being letterboxed
                // into it, and the shipped levels are portrait boards sized to
                // match this area, so the grid fills it and the arrows grow with
                // the space instead of sitting in a square with plate to spare.
                // Nothing here is fixed to a dp, so a short screen shrinks the maze
                // and a tablet grows it.
                ZoomableBoard(
                    modifier = Modifier.fillMaxSize(),
                    resetKey = level.id
                ) {
                    ArrowBoard(
                        state = gameState,
                        modifier = Modifier.fillMaxSize(),
                        inputEnabled = boardEnabled,
                        canSelect = { index ->
                            tutorialStage != TutorialStage.FIRST_MOVE || index == firstMoveTileIndex
                        },
                        highlightTileIndex = when {
                            tutorialStage == TutorialStage.FIRST_MOVE -> firstMoveTileIndex
                            hintTileIndex != null -> hintTileIndex
                            else -> null
                        },
                        highlight = when {
                            tutorialStage == TutorialStage.FIRST_MOVE -> TileHighlight.TUTORIAL
                            hintTileIndex != null -> TileHighlight.HINT
                            else -> TileHighlight.NONE
                        },
                        block = block,
                        launchArrow = departure,
                        launchStyle = launchStyle,
                        onLaunchFinished = { finishedIndex ->
                            if (finishedIndex == winTileIndex) winExit?.complete(Unit)
                        },
                        onArrowTap = { index ->
                            val tile = level.tiles[index]
                            val nextState = PuzzleReducer.tap(gameState, tile.position)
                            val wasInvalid = nextState.invalidTaps > gameState.invalidTaps
                            gameState = nextState
                            if (wasInvalid) {
                                // The whole arrow flashes red, shakes, and the
                                // cell that stops it lights up as the impact.
                                val blocker = PuzzleReducer.blockingCell(nextState, tile)
                                block = ArrowBlock(
                                    tileIndex = index,
                                    cell = blocker ?: tile.position.step(tile.direction),
                                    count = (block?.count ?: 0) + 1
                                )
                                statusMessage = "PATH BLOCKED"
                                haptics.reject()
                                sounds.reject()
                                scope.launch {
                                    delay(900)
                                    if (statusMessage == "PATH BLOCKED") statusMessage = null
                                }
                            } else {
                                statusMessage = null
                                hintTileIndex = null
                                haptics.success()
                                sounds.rocketLaunch()
                                departure = ArrowLaunch(index, (departure?.trigger ?: 0L) + 1L)
                                if (nextState.status == GameStatus.WON) {
                                    // Arm the clear celebration. It waits for the
                                    // board to report this arrow fully off the
                                    // plate, then plays before the result card.
                                    winTileIndex = index
                                    winExit = CompletableDeferred()
                                }
                                if (tutorialStage == TutorialStage.FIRST_MOVE && index == firstMoveTileIndex) {
                                    tutorialStage = TutorialStage.COMPLETE
                                    scope.launch { progressRepository.setTutorialSeen(true) }
                                }
                            }
                        }
                    )
                }

                // The clear celebration sits on the board itself, so it stays
                // inside the play area and never reaches the HUD or the action
                // buttons. It is pure presentation: it reads the star score and
                // draws, and touches nothing else.
                celebrationStars?.let { stars ->
                    StarClearCelebration(
                        stars = stars,
                        modifier = Modifier.fillMaxSize(),
                        onFinished = {
                            celebrationStars = null
                            resultReady = true
                        }
                    )
                }
            }

            if (tutorialStage == TutorialStage.PREPARE) {
                NeonStartButton { tutorialStage = TutorialStage.FIRST_MOVE }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonActionButton(
                    icon = Icons.Default.Lightbulb,
                    label = "HINT",
                    accent = Gold,
                    trailing = "$hintsRemaining",
                    enabled = boardEnabled && tutorialStage == TutorialStage.COMPLETE && hintsRemaining > 0,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = PuzzleReducer.firstValidMove(gameState)
                        val index = target?.let(gameState.level::ownerAt)
                        if (index != null) {
                            hintTileIndex = index
                            hintsRemaining -= 1
                            haptics.success()
                        }
                    }
                )
                NeonIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Restart",
                    tint = NeonCyan,
                    size = 52,
                    onClick = restartGame
                )
            }
            Spacer(Modifier.height(2.dp))

            /*
             * The banner is the last child of the column, below the HINT row,
             * so it sits furthest from the board - the one big tap target on
             * this screen - and the HINT and Restart controls keep their place
             * whatever the ad does. The band is reserved up front and never
             * resizes, so a banner that fills late cannot shrink the board
             * under the player's fingers; it only costs the board the height
             * once, for the whole level.
             */
            ReservedBannerAdSlot(adHost = ads)
        }

        AnimatedVisibility(
            visible = paused,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DimOverlay(modifier = Modifier.fillMaxSize())
                ResultOverlay(
                    title = "PAUSED",
                    accent = NeonCyan,
                    surfaceColor = NeonPanel,
                    titleColor = NeonText,
                    subtitleColor = NeonTextDim,
                    secondaryContainerColor = NeonPanel,
                    secondaryContentColor = NeonText,
                    primaryLabel = "RESUME",
                    onPrimary = { paused = false },
                    secondaryLabel = "RESTART",
                    onSecondary = restartGame,
                    tertiaryLabel = "SETTINGS",
                    onTertiary = { showSettings = true },
                    quaternaryLabel = "LEVELS",
                    onQuaternary = onBackToSelect
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsScreen(
                progressRepository = progressRepository,
                onBack = { showSettings = false }
            )
        }

        // The result card only arrives once the clear celebration has played out,
        // so the celebration is never hidden behind it.
        AnimatedVisibility(
            visible = gameState.status == GameStatus.WON && resultReady,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.94f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            ResultOverlay(
                title = if (hasNextLevel) "LEVEL CLEAR" else "PACK CLEAR",
                stars = earnedStars,
                accent = NeonGreen,
                surfaceColor = NeonPanel,
                titleColor = NeonText,
                subtitleColor = NeonTextDim,
                secondaryContainerColor = NeonPanel,
                secondaryContentColor = NeonText,
                starTint = Gold,
                primaryLabel = if (hasNextLevel) "NEXT LEVEL" else "BACK TO LEVELS",
                onPrimary = {
                    scope.launch {
                        ads.onLevelCompleted()
                        if (hasNextLevel) onNextLevel() else onBackToSelect()
                    }
                },
                secondaryLabel = "REPLAY",
                onSecondary = restartGame
            )
        }

        AnimatedVisibility(
            visible = gameState.status == GameStatus.LOST,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val offerContinue = rewardedAvailable && !rewardedUsed
            if (offerContinue) {
                ResultOverlay(
                    title = "OUT OF LIVES",
                    subtitle = "$cleared of $totalArrows cleared",
                    accent = Gold,
                    surfaceColor = NeonPanel,
                    titleColor = NeonText,
                    subtitleColor = NeonTextDim,
                    secondaryContainerColor = NeonPanel,
                    secondaryContentColor = NeonText,
                    primaryLabel = "CONTINUE  +1 ♥  (AD)",
                    onPrimary = {
                        scope.launch {
                            if (ads.showRewarded()) {
                                rewardedUsed = true
                                gameState = gameState.copy(lives = 1, status = GameStatus.PLAYING)
                            } else {
                                statusMessage = "AD UNAVAILABLE"
                            }
                        }
                    },
                    secondaryLabel = "TRY AGAIN",
                    onSecondary = restartGame,
                    tertiaryLabel = "LEVELS",
                    onTertiary = onBackToSelect
                )
            } else {
                ResultOverlay(
                    title = "OUT OF LIVES",
                    stars = 0,
                    subtitle = "$cleared of $totalArrows cleared",
                    accent = NeonRed,
                    surfaceColor = NeonPanel,
                    titleColor = NeonText,
                    subtitleColor = NeonTextDim,
                    secondaryContainerColor = NeonPanel,
                    secondaryContentColor = NeonText,
                    starTint = NeonTextDim,
                    primaryLabel = "TRY AGAIN",
                    onPrimary = restartGame,
                    secondaryLabel = "LEVELS",
                    onSecondary = onBackToSelect
                )
            }
        }
    }
}

@Composable
private fun NeonStartButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(NeonCyan.copy(alpha = 0.18f), NeonMagenta.copy(alpha = 0.18f))
                )
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "START",
            color = NeonText,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            letterSpacing = 3.sp
        )
    }
}
