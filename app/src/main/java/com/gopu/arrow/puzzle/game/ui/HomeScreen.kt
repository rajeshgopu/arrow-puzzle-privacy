package com.gopu.arrow.puzzle.game.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.ads.BannerAdSlot
import com.gopu.arrow.puzzle.game.ui.components.ArrowMark
import com.gopu.arrow.puzzle.game.ui.components.RoundIconButton
import com.gopu.arrow.puzzle.game.ui.components.StatChip
import com.gopu.arrow.puzzle.game.ui.components.Wordmark
import com.gopu.arrow.puzzle.game.ui.theme.ArrowPuzzleTheme
import com.gopu.arrow.puzzle.game.ui.theme.CanvasWhite
import com.gopu.arrow.puzzle.game.ui.theme.Cloud
import com.gopu.arrow.puzzle.game.ui.theme.Coral
import com.gopu.arrow.puzzle.game.ui.theme.NeonCyan
import com.gopu.arrow.puzzle.game.ui.theme.InkSoft

@Composable
fun HomeScreen(
    progressRepository: ProgressRepository,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    showBanner: Boolean = false
) {
    val highestUnlocked by progressRepository.highestUnlockedLevel.collectAsState(initial = 1)
    val bestStars by progressRepository.bestStars.collectAsState(initial = emptyMap())
    val totalStars = remember(bestStars) { bestStars.values.sum() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CanvasWhite, Cloud)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Wordmark(fontSize = 46)
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clickable(onClickLabel = "Play") { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                ArrowMark(
                    direction = Direction.RIGHT,
                    modifier = Modifier.size(150.dp),
                    color = NeonCyan,
                    glow = true
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("TAP TO PLAY", color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip("LEVEL", highestUnlocked.toString())
                StatChip("STARS", "$totalStars ★")
            }
            Spacer(Modifier.weight(1f))
            BannerAdSlot(available = showBanner)
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("PLAY", fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        RoundIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomePreview() {
    ArrowPuzzleTheme {
        HomeScreen(progressRepository = ProgressRepository(LocalContext.current), onPlay = {}, onSettings = {})
    }
}
