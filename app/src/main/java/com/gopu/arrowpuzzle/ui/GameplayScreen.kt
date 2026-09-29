package com.gopu.arrowpuzzle.ui

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrowpuzzle.data.ProgressRepository
import com.gopu.arrowpuzzle.ads.AdHost
import com.gopu.arrowpuzzle.ads.NoAds
import com.gopu.arrowpuzzle.game.BoardPosition
import com.gopu.arrowpuzzle.game.GameStatus
import com.gopu.arrowpuzzle.game.PuzzleLevel
import com.gopu.arrowpuzzle.game.PuzzleReducer
import com.gopu.arrowpuzzle.game.starsForInvalidTaps
import com.gopu.arrowpuzzle.levels.LEVELS_PER_PACK
import com.gopu.arrowpuzzle.ui.components.ArrowBoard
import com.gopu.arrowpuzzle.ui.components.BoardPulse
import com.gopu.arrowpuzzle.ui.components.DimOverlay
import com.gopu.arrowpuzzle.ui.components.LivesRow
import com.gopu.arrowpuzzle.ui.components.ResultOverlay
import com.gopu.arrowpuzzle.ui.components.RoundIconButton
import com.gopu.arrowpuzzle.ui.components.TileBurst
import com.gopu.arrowpuzzle.ui.components.TileHighlight
import com.gopu.arrowpuzzle.ui.components.ZoomableBoard
import com.gopu.arrowpuzzle.ui.theme.Cloud
import com.gopu.arrowpuzzle.ui.theme.Coral
import com.gopu.arrowpuzzle.ui.theme.Gold
import com.gopu.arrowpuzzle.ui.theme.Ink
import com.gopu.arrowpuzzle.ui.theme.InkSoft
import com.gopu.arrowpuzzle.ui.theme.Mint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TutorialStage { PREPARE, FIRST_MOVE, COMPLETE }

private const val HINTS_PER_LEVEL = 3

/**
 * The game screen: board plus a calm single-row HUD, with pause, hint, and
 * victory/failure overlays layered on top. All moves are dispatched to the
 * pure [PuzzleReducer]; this composable only renders state and plays feedback.
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
    val rewardedAvailable by ads.rewardedAvailable.collectAsState(initial = false)
    val tutorialSeen by progressRepository.tutorialSeen.collectAsState(initial = false)
    val isIntroLevel = level.pack == 1 && level.order == 1
    val showTutorial = isIntroLevel && !tutorialSeen
    val firstMoveTarget = remember(level) { PuzzleReducer.firstValidMove(PuzzleReducer.start(level)) }
    var tutorialStage by remember(level, showTutorial) {
        mutableStateOf(if (showTutorial) TutorialStage.PREPARE else TutorialStage.COMPLETE)
    }
    var invalidPulse by remember(level) { mutableStateOf<BoardPulse?>(null) }
    var statusMessage by remember(level) { mutableStateOf<String?>(null) }
    var paused by remember(level) { mutableStateOf(false) }
    var showSettings by remember(level) { mutableStateOf(false) }
    var hintsRemaining by remember(level) { mutableStateOf(HINTS_PER_LEVEL) }
    var hintTarget by remember(level) { mutableStateOf<BoardPosition?>(null) }
    var hintPulse by remember(level) { mutableStateOf(0) }
    var rewardedUsed by remember(level) { mutableStateOf(false) }
    var burst by remember(level) { mutableStateOf<TileBurst?>(null) }

    val totalTiles = level.tiles.size
    val remaining = gameState.remainingTiles.size
    val cleared = totalTiles - remaining
    val progress by animateFloatAsState(
        targetValue = if (totalTiles == 0) 1f else cleared.toFloat() / totalTiles,
        animationSpec = tween(350),
        label = "progress"
    )

    val restartGame = {
        gameState = PuzzleReducer.restart(gameState)
        tutorialStage = if (showTutorial) TutorialStage.PREPARE else TutorialStage.COMPLETE
        invalidPulse = null
        statusMessage = null
        hintTarget = null
        hintsRemaining = HINTS_PER_LEVEL
        rewardedUsed = false
        burst = null
        paused = false
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Cloud)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBackToSelect)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEVEL ${level.order}", color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("PACK ${level.pack}", color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundIconButton(Icons.Default.Settings, "Settings", onClick = { showSettings = true })
                    RoundIconButton(Icons.Default.Pause, "Pause", onClick = { paused = true })
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LivesRow(gameState.lives)
                Text("$remaining LEFT", color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Mint,
                trackColor = Cloud
            )

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = when {
                    gameState.status == GameStatus.WON -> "CLEAR"
                    gameState.status == GameStatus.LOST -> "OUT OF LIVES"
                    tutorialStage == TutorialStage.PREPARE -> "TAP START TO BEGIN"
                    tutorialStage == TutorialStage.FIRST_MOVE -> "TAP THE HIGHLIGHTED ARROW"
                    statusMessage != null -> statusMessage!!
                    else -> "CLEAR A PATH TO THE EDGE"
                },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "status"
            ) { message ->
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth().height(22.dp),
                    color = if (statusMessage != null && message == statusMessage) Coral else Ink,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BoxWithConstraints {
                    val boardSide = minOf(maxWidth, maxHeight).coerceAtMost(760.dp)
                    ZoomableBoard(
                        modifier = Modifier.size(boardSide),
                        resetKey = level.id
                    ) {
                        ArrowBoard(
                            state = gameState,
                            modifier = Modifier.fillMaxSize(),
                            inputEnabled = boardEnabled,
                            canTap = { position ->
                                tutorialStage != TutorialStage.FIRST_MOVE || position == firstMoveTarget
                            },
                            highlightPosition = when {
                                tutorialStage == TutorialStage.FIRST_MOVE -> firstMoveTarget
                                hintTarget != null -> hintTarget
                                else -> null
                            },
                            highlight = when {
                                tutorialStage == TutorialStage.FIRST_MOVE -> TileHighlight.TUTORIAL
                                hintTarget != null -> TileHighlight.HINT
                                else -> TileHighlight.NONE
                            },
                            pulse = invalidPulse,
                            pulseCount = hintPulse,
                            bursts = listOfNotNull(burst),
                            onArrowTap = { position ->
                                val nextState = PuzzleReducer.tap(gameState, position)
                                val wasInvalid = nextState.invalidTaps > gameState.invalidTaps
                                gameState = nextState
                                if (wasInvalid) {
                                    invalidPulse = BoardPulse(position, (invalidPulse?.count ?: 0) + 1)
                                    statusMessage = "PATH BLOCKED"
                                    haptics.reject()
                                    sounds.reject()
                                    scope.launch {
                                        delay(900)
                                        if (statusMessage == "PATH BLOCKED") statusMessage = null
                                    }
                                } else {
                                    statusMessage = null
                                    hintTarget = null
                                    haptics.success()
                                    sounds.rocketLaunch()
                                    val direction = gameState.level.tileAt(position)?.direction
                                    if (direction != null) {
                                        val id = (burst?.id ?: 0L) + 1L
                                        burst = TileBurst(position, direction, id)
                                        scope.launch {
                                            delay(420)
                                            if (burst?.id == id) burst = null
                                        }
                                    }
                                    if (tutorialStage == TutorialStage.FIRST_MOVE && position == firstMoveTarget) {
                                        tutorialStage = TutorialStage.COMPLETE
                                        scope.launch { progressRepository.setTutorialSeen(true) }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (tutorialStage == TutorialStage.PREPARE) {
                PrimaryStartButton { tutorialStage = TutorialStage.FIRST_MOVE }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HintButton(
                    hintsRemaining = hintsRemaining,
                    enabled = boardEnabled && tutorialStage == TutorialStage.COMPLETE && hintsRemaining > 0,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val target = PuzzleReducer.firstValidMove(gameState)
                        if (target != null) {
                            hintTarget = target
                            hintPulse += 1
                            hintsRemaining -= 1
                            haptics.success()
                        }
                    }
                )
                RoundIconButton(Icons.Default.Refresh, "Restart", restartGame)
            }
            Spacer(Modifier.height(4.dp))
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
                    accent = Ink,
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

        AnimatedVisibility(
            visible = gameState.status == GameStatus.WON,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            ResultOverlay(
                title = if (hasNextLevel) "LEVEL CLEAR" else "PACK CLEAR",
                stars = earnedStars,
                accent = Mint,
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
                    subtitle = "$cleared of $totalTiles cleared",
                    accent = Gold,
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
                    subtitle = "$cleared of $totalTiles cleared",
                    accent = Coral,
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
private fun PrimaryStartButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Ink,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("START", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun HintButton(
    hintsRemaining: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val container = if (enabled) Color.White else Cloud
    val content = if (enabled) Ink else InkSoft.copy(alpha = 0.5f)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        shadowElevation = if (enabled) 4.dp else 0.dp,
        modifier = modifier.height(50.dp).clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = "Hint", tint = if (enabled) Gold else content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("HINT", color = content, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = if (enabled) Gold.copy(alpha = 0.25f) else Cloud) {
                Text(
                    "$hintsRemaining",
                    color = content,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
