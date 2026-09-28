package com.gopu.arrowpuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrowpuzzle.data.ProgressRepository
import com.gopu.arrowpuzzle.game.ArrowTile
import com.gopu.arrowpuzzle.game.BoardPosition
import com.gopu.arrowpuzzle.game.Direction
import com.gopu.arrowpuzzle.game.GameStatus
import com.gopu.arrowpuzzle.game.PuzzleLevel
import com.gopu.arrowpuzzle.game.PuzzleReducer
import com.gopu.arrowpuzzle.game.PuzzleState
import com.gopu.arrowpuzzle.game.starsForInvalidTaps
import com.gopu.arrowpuzzle.levels.LevelRepository
import com.gopu.arrowpuzzle.ui.LevelSelectScreen
import com.gopu.arrowpuzzle.ui.PackInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArrowPuzzleApp() }
    }
}

private val Ink = Color(0xFF113C47)
private val CanvasWhite = Color(0xFFF3F5EC)
private val Coral = Color(0xFFF05D3A)
private val Mint = Color(0xFF72C9A5)
private val Gold = Color(0xFFF4BD4F)
private val TileBlue = Color(0xFF3E99C1)

private enum class AppScreen { HOME, LEVEL_SELECT, GAMEPLAY }
private enum class TutorialStage { PREPARE, FIRST_MOVE, COMPLETE }

private val LevelOne = PuzzleLevel(
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

@Composable
private fun ArrowPuzzleApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    val context = LocalContext.current
    val progressRepository = ProgressRepository(context)
    val levelRepository = LevelRepository(context)
    var currentLevel by remember {
        mutableStateOf(levelRepository.loadLevel(1, 1) ?: LevelOne)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = CanvasWhite) {
            when (screen) {
                AppScreen.HOME -> HomeScreen(onPlay = { screen = AppScreen.LEVEL_SELECT })
                AppScreen.LEVEL_SELECT -> {
                    val packs = listOf(
                        PackInfo(
                            packNumber = 1,
                            levelCount = 10,
                            levels = levelRepository.loadAllLevels(1, 10)
                        )
                    )
                    LevelSelectScreen(
                        packs = packs,
                        progressRepository = progressRepository,
                        onLevelClick = { level ->
                            currentLevel = level
                            screen = AppScreen.GAMEPLAY
                        },
                        onBack = { screen = AppScreen.HOME }
                    )
                }
                AppScreen.GAMEPLAY -> LevelScreen(
                    level = currentLevel,
                    progressRepository = progressRepository,
                    onBackToSelect = { screen = AppScreen.LEVEL_SELECT },
                    onHome = { screen = AppScreen.HOME }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onPlay: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text("ARROW", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Text("PUZZLE", color = Coral, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(42.dp))
        ArrowMark(modifier = Modifier.size(176.dp))
        Spacer(Modifier.weight(1f))
        Text("LEVEL 1", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            Spacer(Modifier.width(10.dp))
            Text("PLAY", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LevelScreen(
    level: PuzzleLevel,
    progressRepository: ProgressRepository,
    onBackToSelect: () -> Unit,
    onHome: () -> Unit
) {
    var gameState by remember(level) { mutableStateOf(PuzzleReducer.start(level)) }
    val scope = rememberCoroutineScope()
    val tutorialSeen by progressRepository.tutorialSeen.collectAsState(initial = false)
    val isIntroLevel = level.pack == 1 && level.order == 1
    val showTutorial = isIntroLevel && !tutorialSeen
    val firstMoveTarget = remember(level) { PuzzleReducer.firstValidMove(PuzzleReducer.start(level)) }
    var tutorialStage by remember(level, showTutorial) {
        mutableStateOf(if (showTutorial) TutorialStage.PREPARE else TutorialStage.COMPLETE)
    }
    val remaining = gameState.remainingTiles.size
    val restartGame = {
        gameState = PuzzleReducer.restart(gameState)
        tutorialStage = if (showTutorial) TutorialStage.PREPARE else TutorialStage.COMPLETE
    }

    LaunchedEffect(gameState.status, level.id) {
        if (gameState.status == GameStatus.WON) {
            progressRepository.unlockLevel(
                levelId = level.id,
                stars = starsForInvalidTaps(gameState.invalidTaps),
                levelOrder = level.order + 1
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToSelect) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("LEVEL ${level.order}", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${gameState.lives} LIVES", color = Coral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = restartGame) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Ink)
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        Text(
            text = "$remaining ARROWS LEFT",
            modifier = Modifier.fillMaxWidth(),
            color = Ink,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        when (tutorialStage) {
            TutorialStage.PREPARE -> TutorialStart {
                tutorialStage = TutorialStage.FIRST_MOVE
            }
            TutorialStage.FIRST_MOVE -> TutorialPrompt("TAP THE HIGHLIGHTED ARROW")
            TutorialStage.COMPLETE -> Unit
        }
        ArrowBoard(
            state = gameState,
            tutorialStage = tutorialStage,
            tutorialTarget = firstMoveTarget,
            onArrowTap = { position ->
                val nextState = PuzzleReducer.tap(gameState, position)
                gameState = nextState
                if (tutorialStage == TutorialStage.FIRST_MOVE &&
                    position == firstMoveTarget &&
                    position !in nextState.remainingTiles
                ) {
                    tutorialStage = TutorialStage.COMPLETE
                    scope.launch { progressRepository.setTutorialSeen(true) }
                }
            }
        )
        Spacer(Modifier.weight(1f))
        when (gameState.status) {
            GameStatus.PLAYING -> Text(
                text = if (tutorialStage == TutorialStage.COMPLETE) "CLEAR A PATH" else "READY",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Ink,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            GameStatus.WON -> ResultAction("LEVEL CLEAR", Mint, restartGame)
            GameStatus.LOST -> ResultAction("TRY AGAIN", Coral, restartGame)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ArrowBoard(
    state: PuzzleState,
    tutorialStage: TutorialStage,
    tutorialTarget: BoardPosition?,
    onArrowTap: (BoardPosition) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(state.level.height) { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(state.level.width) { column ->
                    val position = BoardPosition(row, column)
                    val tile = state.level.tileAt(position)
                    val isRemaining = position in state.remainingTiles
                    val isTutorialTarget = tutorialStage == TutorialStage.FIRST_MOVE && position == tutorialTarget
                    val inputEnabled = when (tutorialStage) {
                        TutorialStage.PREPARE -> false
                        TutorialStage.FIRST_MOVE -> isTutorialTarget
                        TutorialStage.COMPLETE -> true
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRemaining) tileColor(row * state.level.width + column) else Color.Transparent)
                            .then(
                                if (isTutorialTarget && isRemaining) {
                                    Modifier.border(3.dp, Color.White, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(enabled = isRemaining && inputEnabled && state.status == GameStatus.PLAYING) {
                                onArrowTap(position)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRemaining && tile != null) {
                            ArrowMark(direction = tile.direction, modifier = Modifier.size(42.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialStart(onStart: () -> Unit) {
    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White)
    ) {
        Text("START", fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TutorialPrompt(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        textAlign = TextAlign.Center,
        color = Ink,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp
    )
}

@Composable
private fun ResultAction(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

private fun tileColor(index: Int): Color = when (index % 4) {
    0 -> Coral
    1 -> TileBlue
    2 -> Gold
    else -> Mint
}

@Composable
private fun ArrowMark(modifier: Modifier, direction: Direction = Direction.RIGHT) {
    Canvas(modifier = modifier) {
        drawArrow(direction)
    }
}

private fun DrawScope.drawArrow(direction: Direction) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val shaft = size.minDimension * 0.30f
    val head = size.minDimension * 0.22f
    val path = Path()

    fun point(x: Float, y: Float): Offset = when (direction) {
        Direction.RIGHT -> Offset(center.x + x, center.y + y)
        Direction.LEFT -> Offset(center.x - x, center.y + y)
        Direction.UP -> Offset(center.x + y, center.y - x)
        Direction.DOWN -> Offset(center.x + y, center.y + x)
    }

    path.moveTo(point(-shaft, -head / 2f).x, point(-shaft, -head / 2f).y)
    path.lineTo(point(shaft * 0.35f, -head / 2f).x, point(shaft * 0.35f, -head / 2f).y)
    path.lineTo(point(shaft * 0.35f, -head).x, point(shaft * 0.35f, -head).y)
    path.lineTo(point(shaft, 0f).x, point(shaft, 0f).y)
    path.lineTo(point(shaft * 0.35f, head).x, point(shaft * 0.35f, head).y)
    path.lineTo(point(shaft * 0.35f, head / 2f).x, point(shaft * 0.35f, head / 2f).y)
    path.lineTo(point(-shaft, head / 2f).x, point(-shaft, head / 2f).y)
    path.close()
    drawPath(path = path, color = Color.White)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    ArrowPuzzleApp()
}
