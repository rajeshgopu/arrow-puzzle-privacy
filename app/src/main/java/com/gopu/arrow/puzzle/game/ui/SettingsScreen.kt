package com.gopu.arrow.puzzle.game.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.AppConfig
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.ui.components.DimOverlay
import com.gopu.arrow.puzzle.game.ui.components.PrimaryButton
import com.gopu.arrow.puzzle.game.ui.components.RoundIconButton
import com.gopu.arrow.puzzle.game.ui.theme.Cloud
import com.gopu.arrow.puzzle.game.ui.theme.Coral
import com.gopu.arrow.puzzle.game.ui.theme.Ink
import com.gopu.arrow.puzzle.game.ui.theme.InkSoft
import com.gopu.arrow.puzzle.game.ui.theme.Mint
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    progressRepository: ProgressRepository,
    onBack: () -> Unit
) {
    val soundEnabled by progressRepository.soundEnabled.collectAsState(initial = true)
    val hapticsEnabled by progressRepository.hapticsEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sampleSounds = rememberSounds(enabled = true)
    val sampleHaptics = rememberHaptics(enabled = true)
    var showHowToPlay by remember { mutableStateOf(false) }
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Cloud)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Text("SETTINGS", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(22.dp))

            SettingsSectionLabel("GAMEPLAY")
            SettingsCard {
                SettingsToggleRow(
                    title = "Sound",
                    subtitle = "Tap and tile sounds",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = soundEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { progressRepository.setSoundEnabled(enabled) }
                        if (enabled) sampleSounds.sample()
                    }
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Haptics",
                    subtitle = "Vibration on taps",
                    icon = Icons.Default.Vibration,
                    checked = hapticsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { progressRepository.setHapticsEnabled(enabled) }
                        if (enabled) sampleHaptics.sample()
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            SettingsSectionLabel("ABOUT")
            SettingsCard {
                SettingsActionRow(
                    title = "How to play",
                    icon = Icons.Default.Lightbulb,
                    onClick = { showHowToPlay = true }
                )
                if (AppConfig.privacyOptionsAvailable) {
                    SettingsDivider()
                    SettingsActionRow(
                        title = "Privacy Options",
                        icon = Icons.Default.Policy,
                        onClick = { /* UMP form opens here once integrated (Phase 4) */ }
                    )
                }
                val policyUrl = AppConfig.privacyPolicyUrl
                if (policyUrl != null) {
                    SettingsDivider()
                    SettingsActionRow(
                        title = "Privacy Policy",
                        icon = Icons.Default.Policy,
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl)))
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Arrow Puzzle  •  v$versionName",
                color = InkSoft,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        AnimatedVisibility(
            visible = showHowToPlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            HowToPlayOverlay(onDismiss = { showHowToPlay = false })
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        color = InkSoft,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(1.dp)
            .background(Cloud)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = InkSoft, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Mint)
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = InkSoft,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun HowToPlayOverlay(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DimOverlay(modifier = Modifier.fillMaxSize(), onClick = onDismiss)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(Color.White, RoundedCornerShape(26.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("HOW TO PLAY", color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(18.dp))
            HowToRule("1", "An arrow can leave the board only when every cell in front of it, up to the edge, is empty.")
            HowToRule("2", "Tap an arrow with a clear path to remove it.")
            HowToRule("3", "Tap a blocked arrow and you lose one of your three lives.")
            HowToRule("4", "Clear every arrow to win. Fewer blocked taps means more stars.")
            Spacer(Modifier.height(20.dp))
            PrimaryButton(label = "GOT IT", onClick = onDismiss, containerColor = Ink)
        }
    }
}

@Composable
private fun HowToRule(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Coral, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = InkSoft,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
    }
}
