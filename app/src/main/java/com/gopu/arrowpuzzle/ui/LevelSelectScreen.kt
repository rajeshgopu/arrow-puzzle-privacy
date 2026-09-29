package com.gopu.arrowpuzzle.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrowpuzzle.data.ProgressRepository
import com.gopu.arrowpuzzle.game.PuzzleLevel
import com.gopu.arrowpuzzle.levels.LEVELS_PER_PACK
import com.gopu.arrowpuzzle.ads.BannerAdSlot
import com.gopu.arrowpuzzle.ui.components.RoundIconButton
import com.gopu.arrowpuzzle.ui.components.StatChip
import com.gopu.arrowpuzzle.ui.theme.Cloud
import com.gopu.arrowpuzzle.ui.theme.Coral
import com.gopu.arrowpuzzle.ui.theme.Gold
import com.gopu.arrowpuzzle.ui.theme.Ink
import com.gopu.arrowpuzzle.ui.theme.InkSoft
import com.gopu.arrowpuzzle.ui.theme.Locked
import com.gopu.arrowpuzzle.ui.theme.Mint
import com.gopu.arrowpuzzle.ui.theme.TileBlue
import kotlinx.coroutines.launch

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
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    showBanner: Boolean = false
) {
    val highestUnlocked by progressRepository.highestUnlockedLevel.collectAsState(initial = 1)
    val bestStars by progressRepository.bestStars.collectAsState(initial = emptyMap())
    val totalStars = remember(packs, bestStars) {
        packs.sumOf { pack -> pack.levels.sumOf { bestStars[it.id] ?: 0 } }
    }

    val pagerState = rememberPagerState(pageCount = { packs.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Cloud)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            Text("SELECT LEVEL", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("STARS", "$totalStars ★")
                RoundIconButton(Icons.Default.Settings, "Settings", onSettings)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (packs.isEmpty()) return@Column

        PackPagerControls(
            current = pagerState.currentPage,
            total = packs.size,
            onPrevious = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } },
            onNext = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(packs.size - 1)) } }
        )

        Spacer(Modifier.height(14.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            val pack = packs[page]
            if (pack.levels.isEmpty()) {
                ComingSoonPack(pack.packNumber)
            } else {
                PackPage(
                    pack = pack,
                    highestUnlocked = highestUnlocked,
                    bestStars = bestStars,
                    onLevelClick = onLevelClick
                )
            }
        }

        BannerAdSlot(available = showBanner)
    }
}

@Composable
private fun PackPagerControls(
    current: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous pack",
            onClick = onPrevious,
            tint = if (current > 0) Ink else Locked
        )
        Text(
            text = "PACK ${current + 1} / $total",
            color = Ink,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(150.dp)
        )
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next pack",
            onClick = onNext,
            tint = if (current < total - 1) Ink else Locked
        )
    }
}

@Composable
private fun PackPage(
    pack: PackInfo,
    highestUnlocked: Int,
    bestStars: Map<String, Int>,
    onLevelClick: (PuzzleLevel) -> Unit
) {
    val cleared = remember(pack, bestStars) {
        pack.levels.count { (bestStars[it.id] ?: 0) > 0 }
    }
    val collectedStars = remember(pack, bestStars) {
        pack.levels.sumOf { bestStars[it.id] ?: 0 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PackHeader(
            packNumber = pack.packNumber,
            cleared = cleared,
            total = pack.levels.size,
            stars = collectedStars
        )
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pack.levels) { level ->
                val globalOrder = (pack.packNumber - 1) * LEVELS_PER_PACK + level.order
                val isUnlocked = globalOrder <= highestUnlocked
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
private fun ComingSoonPack(packNumber: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Locked, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(14.dp))
            Text("PACK $packNumber", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "More puzzles are on the way",
                color = InkSoft,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PackHeader(
    packNumber: Int,
    cleared: Int,
    total: Int,
    stars: Int
) {
    val ratio by animateFloatAsState(
        targetValue = if (total == 0) 0f else cleared.toFloat() / total,
        animationSpec = tween(400),
        label = "packProgress"
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PACK $packNumber", color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                    Text("$cleared of $total cleared", color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$stars", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Mint,
                trackColor = Cloud
            )
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
    val shape = RoundedCornerShape(18.dp)
    val accent = when (level.order % 4) {
        0 -> Mint
        1 -> Coral
        2 -> TileBlue
        else -> Gold
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(104.dp),
        shape = shape,
        color = Color.White,
        shadowElevation = if (isUnlocked) 5.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    if (isUnlocked) {
                        Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.72f), Color.White))
                    } else {
                        Brush.verticalGradient(listOf(Cloud, Cloud))
                    }
                )
                .clickable(enabled = isUnlocked, onClick = onClick)
                .semantics {
                    contentDescription = if (isUnlocked) {
                        "Level ${level.order}, $stars of 3 stars"
                    } else {
                        "Level ${level.order}, locked"
                    }
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = level.order.toString(),
                        color = Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(3) { index ->
                            Icon(
                                imageVector = if (index < stars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (index < stars) Gold else Locked,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Locked, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("${level.order}", color = Locked, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
