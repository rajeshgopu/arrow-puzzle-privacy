package com.gopu.arrowpuzzle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrowpuzzle.data.ProgressRepository
import com.gopu.arrowpuzzle.game.PuzzleLevel

data class PackInfo(
    val packNumber: Int,
    val levelCount: Int,
    val levels: List<PuzzleLevel>
)

@Composable
fun LevelSelectScreen(
    packs: List<PackInfo>,
    progressRepository: ProgressRepository,
    onLevelClick: (PuzzleLevel) -> Unit,
    onBack: () -> Unit
) {
    val highestUnlocked by progressRepository.highestUnlockedLevel.collectAsState(initial = 1)
    val bestStars by progressRepository.bestStars.collectAsState(initial = emptyMap())

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("SELECT LEVEL", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Pack 1", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        val pack = packs.firstOrNull()
        if (pack == null) return
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pack.levels) { level ->
                val isUnlocked = level.order <= highestUnlocked
                val stars = bestStars[level.id] ?: 0
                LevelTile(
                    level = level,
                    isUnlocked = isUnlocked,
                    stars = stars,
                    onClick = { if (isUnlocked) onLevelClick(level) }
                )
            }
        }
    }
}

@Composable
private fun LevelTile(
    level: PuzzleLevel,
    isUnlocked: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clickable(enabled = isUnlocked, onClick = onClick).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isUnlocked) "Level ${level.order}" else "",
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(24.dp))
            }
            if (stars > 0) {
                Text("$stars ★", color = Color(0xFFF4BD4F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
