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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gopu.arrow.puzzle.game.AppConfig
import com.gopu.arrow.puzzle.game.R
import com.gopu.arrow.puzzle.game.ads.findActivity
import com.gopu.arrow.puzzle.game.data.ProgressRepository
import com.gopu.arrow.puzzle.game.i18n.AppLanguage
import com.gopu.arrow.puzzle.game.ui.components.DimOverlay
import com.gopu.arrow.puzzle.game.ui.components.LaunchAnimationStyle
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
    val launchStyle by progressRepository.launchAnimation
        .collectAsState(initial = LaunchAnimationStyle.Default)
    val storedLanguageTag by progressRepository.languageTag.collectAsState(initial = null)

    /*
     * Resolved up here because the consent form answers on a callback, which runs
     * outside any composable scope - a `stringResource` read there would not have
     * one to read from.
     */
    val consentSaved = stringResource(R.string.consent_saved)
    val consentNotRequired = stringResource(R.string.consent_not_required)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sampleSounds = rememberSounds(enabled = true)
    val sampleHaptics = rememberHaptics(enabled = true)
    var showHowToPlay by remember { mutableStateOf(false) }
    var privacyOptionsMessage by remember { mutableStateOf<String?>(null) }
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
                RoundIconButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.a11y_back),
                    onBack
                )
                /*
                 * Weighted and two lines rather than centred between two fixed
                 * spacers at 20sp. "SETTINGS" is seven letters; German
                 * "EINSTELLUNGEN" is thirteen, which does not fit between a 48dp
                 * back button and a 48dp spacer on a 360dp phone. The title takes
                 * what is left and wraps into it, so the header grows instead of
                 * pushing the buttons off the row.
                 */
                Text(
                    text = stringResource(R.string.settings_title),
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(22.dp))

            SettingsSectionLabel(stringResource(R.string.settings_section_gameplay))
            SettingsCard {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_sound_title),
                    subtitle = stringResource(R.string.settings_sound_subtitle),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = soundEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { progressRepository.setSoundEnabled(enabled) }
                        if (enabled) sampleSounds.sample()
                    }
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = stringResource(R.string.settings_haptics_title),
                    subtitle = stringResource(R.string.settings_haptics_subtitle),
                    icon = Icons.Default.Vibration,
                    checked = hapticsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { progressRepository.setHapticsEnabled(enabled) }
                        if (enabled) sampleHaptics.sample()
                    }
                )
                SettingsDivider()
                LaunchAnimationRow(
                    style = launchStyle,
                    onSelect = { chosen ->
                        scope.launch { progressRepository.setLaunchAnimation(chosen) }
                    }
                )
                SettingsDivider()
                LanguageRow(
                    selectedTag = storedLanguageTag,
                    onSelect = { tag -> scope.launch { progressRepository.setLanguageTag(tag) } }
                )
            }

            Spacer(Modifier.height(20.dp))

            SettingsSectionLabel(stringResource(R.string.settings_section_about))
            SettingsCard {
                SettingsActionRow(
                    title = stringResource(R.string.about_how_to_play),
                    icon = Icons.Default.Lightbulb,
                    onClick = { showHowToPlay = true }
                )
                if (AppConfig.privacyOptionsAvailable) {
                    SettingsDivider()
                    SettingsActionRow(
                        title = stringResource(R.string.about_privacy_options),
                        icon = Icons.Default.Policy,
                        onClick = {
                            val host = context.findActivity() ?: return@SettingsActionRow
                            AppConfig.openPrivacyOptions(host) { formShown ->
                                privacyOptionsMessage = if (formShown) {
                                    consentSaved
                                } else {
                                    consentNotRequired
                                }
                            }
                        }
                    )
                }
                val policyUrl = AppConfig.privacyPolicyUrl
                if (policyUrl != null) {
                    SettingsDivider()
                    SettingsActionRow(
                        title = stringResource(R.string.about_privacy_policy),
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

            val optionsMessage = privacyOptionsMessage
            if (optionsMessage != null) {
                Text(
                    text = optionsMessage,
                    color = InkSoft,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )
            }

            Text(
                text = stringResource(R.string.about_version, versionName),
                color = InkSoft,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
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

/**
 * The launch animation picker.
 *
 * Every style in [LaunchAnimationStyle] is listed so the option set is visible,
 * and the current one is shown on the row rather than behind a second tap. The
 * `implemented` guard stays on each entry: a style that has been declared but not
 * built yet is listed and dimmed instead of quietly doing nothing when picked.
 */
@Composable
private fun LaunchAnimationRow(
    style: LaunchAnimationStyle,
    onSelect: (LaunchAnimationStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_launch_title),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                stringResource(R.string.settings_launch_subtitle),
                color = InkSoft,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
        Box {
            Row(
                modifier = Modifier
                    .background(Cloud, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = style.label,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                LaunchAnimationStyle.options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (option.implemented) Ink else InkSoft,
                                fontWeight = if (option == style) FontWeight.Black else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        },
                        trailingIcon = if (option == style) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Coral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        },
                        enabled = option.implemented,
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }
            }
        }
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

/**
 * The language picker.
 *
 * "System default" is the first row and is the state of a player who has never
 * opened this screen: a null [selectedTag] means the device locale decides, and
 * the check mark follows the stored tag rather than the resolved language, so a
 * player on a French phone who has explicitly chosen French sees the choice
 * they made rather than a row that appears to be set already.
 *
 * The list is built from [AppLanguage.selectable] and each entry is shown in its
 * own language, because that is what makes it findable - a player scanning for
 * their language is looking for the name they call it, not an English gloss.
 *
 * Picking a row writes the tag and nothing else. [LocalizedApp] watches the same
 * flow, so the strings on this screen change underneath the player as the write
 * lands, with no activity restart: the board, the level, the saved stars, the
 * toggles above and the tutorial flag all stay exactly as they were.
 */
@Composable
private fun LanguageRow(
    selectedTag: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // System default, then one row per shipped language.
    val options: List<String?> = remember { listOf(null) + AppLanguage.selectable.map { it.tag } }

    val selected = options.firstOrNull { it == selectedTag } ?: null
    val currentLabel = when (val language = AppLanguage.fromTag(selected)) {
        null -> stringResource(R.string.settings_language_system_default)
        else -> stringResource(language.labelRes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Language, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_language_title),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                stringResource(R.string.settings_language_subtitle),
                color = InkSoft,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
        Box {
            Row(
                modifier = Modifier
                    .background(Cloud, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLabel,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    val language = AppLanguage.fromTag(option)
                    val optionLabel = when (language) {
                        null -> stringResource(R.string.settings_language_system_default)
                        else -> stringResource(language.labelRes)
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel,
                                color = if (option == selected) Ink else InkSoft,
                                fontWeight = if (option == selected) FontWeight.Black else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        },
                        trailingIcon = if (option == selected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Coral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }
            }
        }
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
            Text(
                text = stringResource(R.string.how_to_play_title),
                color = Ink,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(18.dp))
            HowToRule("1", stringResource(R.string.how_to_rule_1))
            HowToRule("2", stringResource(R.string.how_to_rule_2))
            HowToRule("3", stringResource(R.string.how_to_rule_3))
            HowToRule("4", stringResource(R.string.how_to_rule_4))
            Spacer(Modifier.height(20.dp))
            PrimaryButton(label = stringResource(R.string.action_got_it), onClick = onDismiss, containerColor = Ink)
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
