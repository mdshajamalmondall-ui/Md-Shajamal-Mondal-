package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppFontScale
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemePalette
import com.example.data.model.BackgroundDoodleStyle
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.IncomingCallDisplayStyle
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.SlideToAnswerMode
import com.example.data.model.formatDisplayNumber
import com.example.ui.components.DoodleStylePicker
import com.example.ui.components.ThemePalettePicker
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: DialerUiState,
    strings: NovaStrings,
    signedInEmail: String?,
    onBack: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onOpenAppInfoSettings: () -> Unit,
    onOpenSystemDefaultApps: () -> Unit,
    // Appearance & Themes
    onSelectThemePalette: (AppThemePalette) -> Unit,
    onSelectBackgroundDoodle: (BackgroundDoodleStyle) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onSelectFontScale: (AppFontScale) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
    // Calling Preferences
    onPickRingtone: () -> Unit,
    onOpenSystemSoundSettings: () -> Unit,
    onToggleDtmfSound: (Boolean) -> Unit,
    onSelectSoundStyle: (KeypadSoundStyle) -> Unit,
    onChangeSoundVolume: (Float) -> Unit,
    onPreviewSound: () -> Unit,
    onToggleVibrateOnRing: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleAutoSpeaker: (Boolean) -> Unit,
    onSelectIncomingCallStyle: (IncomingCallDisplayStyle) -> Unit,
    onSelectSlideToAnswerMode: (SlideToAnswerMode) -> Unit,
    onAddQuickReply: (String) -> Unit,
    onRemoveQuickReply: (String) -> Unit,
    onUpdateDndConfig: (enabled: Boolean, startMinutes: Int, endMinutes: Int, allowFavorites: Boolean) -> Unit,
    // SIM & SIP Settings
    onSelectDefaultRoute: (DefaultRoutePreference) -> Unit,
    onSaveSimAlias: (Int, String) -> Unit,
    onCheckSimBalance: (Int) -> Unit,
    onSaveSimBalanceUssd: (Int, String) -> Unit,
    defaultUssdForSlot: (Int) -> String,
    onOpenSipCallingAccounts: () -> Unit,
    // Spam & Block List
    onToggleCloudCallerId: (Boolean) -> Unit,
    onToggleSpamProtection: (Boolean) -> Unit,
    onSyncCommunityContacts: () -> Unit,
    onOpenNumberSearchDashboard: () -> Unit,
    onOpenReportSpamDialog: () -> Unit,
    onToggleAutoRejectBlocked: (Boolean) -> Unit,
    onOpenAddBlockDialog: () -> Unit,
    onUnblockNumber: (String) -> Unit,
    // Backup & Restore
    onToggleAutoCallBackup: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    // Privacy & Security
    onOpenSystemNotificationSettings: () -> Unit,
    onToggleNotifyIncoming: (Boolean) -> Unit,
    onToggleNotifyMissed: (Boolean) -> Unit,
    onToggleNotifyOngoing: (Boolean) -> Unit,
    onClearLocalUserData: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val isBn = state.preferences.language == AppLanguage.BENGALI

    var sim1UssdInput by remember(state.preferences.sim1BalanceUssd, state.sim1.carrierName) {
        mutableStateOf(state.preferences.sim1BalanceUssd.ifBlank { defaultUssdForSlot(0) })
    }
    var sim2UssdInput by remember(state.preferences.sim2BalanceUssd, state.sim2.carrierName) {
        mutableStateOf(state.preferences.sim2BalanceUssd.ifBlank { defaultUssdForSlot(1) })
    }
    var sim1AliasInput by remember(state.preferences.sim1Alias) {
        mutableStateOf(state.preferences.sim1Alias)
    }
    var sim2AliasInput by remember(state.preferences.sim2Alias) {
        mutableStateOf(state.preferences.sim2Alias)
    }
    var quickReplyDraft by remember { mutableStateOf("") }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Top Settings Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = if (isBn) "সেটিংস" else "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Shajamal PRO DIALER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
            ) {
                // Default Phone App banner (shown only when Shajamal is not default dialer)
                if (!state.isDefaultDialer) {
                    item(key = "default_dialer") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("default_dialer_settings_card"),
                            shape = RoundedCornerShape(22.dp),
                            color = nova.goldContainer.copy(alpha = 0.78f),
                            border = BorderStroke(1.2.dp, nova.gold)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(nova.gold.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = nova.gold, modifier = Modifier.size(22.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBn) "ডিফল্ট ফোন অ্যাপ সেট করুন" else "Set as Default Phone App",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isBn) {
                                                "সব ইনকামিং ও আউটগোয়িং কল সরাসরি Shajamal-এ আনতে এটি অন করুন।"
                                            } else {
                                                "Enable so all incoming & outgoing calls stay directly inside Shajamal."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRequestDefaultDialer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("set_default_dialer_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "Shajamal-কে ডিফল্ট ফোন অ্যাপ করুন" else "Make Shajamal Default Dialer",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = onOpenAppInfoSettings, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                        Text("App Info", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(onClick = onOpenSystemDefaultApps, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                        Text("Default Apps", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. APPEARANCE & THEMES
                item(key = "appearance_and_themes") {
                    SettingsSectionCard(
                        title = if (isBn) "অ্যাপিয়ারেন্স ও থিম (Appearance)" else "Appearance & Themes",
                        icon = Icons.Filled.Palette,
                        iconTint = nova.gold
                    ) {
                        ThemePalettePicker(
                            selectedPalette = state.preferences.themePalette,
                            language = state.preferences.language,
                            onSelectPalette = onSelectThemePalette
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = nova.glassBorder.copy(alpha = 0.55f)
                        )

                        DoodleStylePicker(
                            selectedStyle = state.preferences.backgroundDoodle,
                            language = state.preferences.language,
                            onSelectStyle = onSelectBackgroundDoodle
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = nova.glassBorder.copy(alpha = 0.55f)
                        )

                        SettingsSwitchRow(
                            title = if (isBn) "লিকুইড গ্লাস ইফেক্ট (Liquid Glass)" else "Liquid Glass Effect",
                            subtitle = if (isBn) "কার্ড ও ন্যাভিগেশন বারে স্বচ্ছ কাঁচের ইফেক্ট" else "Translucent frosted glass surface cards & nav bar",
                            checked = state.preferences.liquidGlassEnabled,
                            onCheckedChange = onToggleLiquidGlass,
                            tag = "switch_liquid_glass"
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBn) "ফন্টের আকার (Font Size)" else "Font Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                AppFontScale.SMALL to (if (isBn) "ছোট" else "Small"),
                                AppFontScale.MEDIUM to (if (isBn) "মাঝারি" else "Medium"),
                                AppFontScale.LARGE to (if (isBn) "বড়" else "Large")
                            ).forEach { (scale, label) ->
                                FilterChip(
                                    selected = state.preferences.fontScale == scale,
                                    onClick = { onSelectFontScale(scale) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = nova.glassBorder.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.languageLabel,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = state.preferences.language == AppLanguage.BENGALI,
                                    onClick = { onToggleLanguage(AppLanguage.BENGALI) },
                                    modifier = Modifier.testTag("lang_bn_chip"),
                                    label = { Text("বাংলা") }
                                )
                                FilterChip(
                                    selected = state.preferences.language == AppLanguage.ENGLISH,
                                    onClick = { onToggleLanguage(AppLanguage.ENGLISH) },
                                    modifier = Modifier.testTag("lang_en_chip"),
                                    label = { Text("EN") }
                                )
                            }
                        }
                    }
                }

                // 2. CALLING PREFERENCES (Ringtone, Dialpad Tone, Quick Reply, DND Mode)
                item(key = "calling_preferences") {
                    SettingsSectionCard(
                        title = if (isBn) "কলিং পছন্দ ও DND মোড" else "Calling Preferences & DND",
                        icon = Icons.Filled.MusicNote,
                        iconTint = nova.sim2
                    ) {
                        val currentRingtone = state.preferences.customRingtoneTitle.ifBlank {
                            if (isBn) "ডিফল্ট সিস্টেম রিংটোন" else "Default System Ringtone"
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(0.8.dp, nova.glassBorder.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(nova.sim2Container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = nova.sim2, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "ইনকামিং কল রিংটোন" else "Incoming Call Ringtone",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = currentRingtone,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onPickRingtone,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_ringtone_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isBn) "রিংটোন বাছুন" else "Choose Ringtone", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenSystemSoundSettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_system_sound_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isBn) "সিস্টেম সাউন্ড" else "System Sound", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = nova.glassBorder.copy(alpha = 0.55f)
                        )

                        SettingsSwitchRow(
                            title = strings.keypadSoundLabel,
                            subtitle = if (isBn) "ডায়ালপ্যাডে নম্বর চাপলে টোন বাজবে" else "Play tone when tapping keypad digits",
                            checked = state.preferences.dtmfSoundEnabled,
                            onCheckedChange = onToggleDtmfSound,
                            tag = "switch_dtmf_sound"
                        )

                        if (state.preferences.dtmfSoundEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    KeypadSoundStyle.DTMF_CLASSIC to (if (isBn) "ক্লাসিক" else "Classic"),
                                    KeypadSoundStyle.STUDIO_CHIME to (if (isBn) "চাইম" else "Chime"),
                                    KeypadSoundStyle.SOFT_CLICK to (if (isBn) "সফট ক্লিক" else "Soft Click")
                                ).forEach { (style, label) ->
                                    FilterChip(
                                        selected = state.preferences.keypadSoundStyle == style,
                                        onClick = { onSelectSoundStyle(style) },
                                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = nova.gold, modifier = Modifier.size(18.dp))
                                Slider(
                                    value = state.preferences.keypadSoundVolume,
                                    onValueChange = onChangeSoundVolume,
                                    valueRange = 0.1f..1.0f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                OutlinedButton(
                                    onClick = onPreviewSound,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isBn) "শুনুন" else "Test", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        SettingsSwitchRow(
                            title = if (isBn) "কল এলে ভাইব্রেট" else "Vibrate on Ring",
                            subtitle = null,
                            checked = state.preferences.vibrateOnRing,
                            onCheckedChange = onToggleVibrateOnRing,
                            tag = "switch_vibrate_ring"
                        )
                        SettingsSwitchRow(
                            title = strings.hapticLabel,
                            subtitle = if (isBn) "ডায়ালপ্যাড ট্যাপে হালকা ভাইব্রেশন" else "Gentle vibration on keypad taps",
                            checked = state.preferences.hapticEnabled,
                            onCheckedChange = onToggleHaptic,
                            tag = "switch_haptic"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "অটো স্পিকারফোন" else "Auto Speakerphone",
                            subtitle = if (isBn) "কল রিসিভ হলে অটো স্পিকার অন করার চেষ্টা" else "Automatically switch audio to speaker on answer",
                            checked = state.preferences.autoSpeakerOnCall,
                            onCheckedChange = onToggleAutoSpeaker,
                            tag = "switch_auto_speaker"
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBn) "ইনকামিং কল স্টাইল ও স্লাইড অ্যান্সার" else "Incoming Call Display & Slide to Answer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = state.preferences.incomingCallDisplayStyle == IncomingCallDisplayStyle.FULL_SCREEN,
                                onClick = { onSelectIncomingCallStyle(IncomingCallDisplayStyle.FULL_SCREEN) },
                                label = { Text(if (isBn) "ফুল স্ক্রিন" else "Full Screen") }
                            )
                            FilterChip(
                                selected = state.preferences.incomingCallDisplayStyle == IncomingCallDisplayStyle.BANNER,
                                onClick = { onSelectIncomingCallStyle(IncomingCallDisplayStyle.BANNER) },
                                label = { Text(if (isBn) "ব্যানার" else "Banner") }
                            )
                            FilterChip(
                                selected = state.preferences.slideToAnswerMode == SlideToAnswerMode.LOCKED_ONLY,
                                onClick = { onSelectSlideToAnswerMode(SlideToAnswerMode.LOCKED_ONLY) },
                                label = { Text(if (isBn) "লক থাকলে স্লাইড" else "Slide Locked") }
                            )
                            FilterChip(
                                selected = state.preferences.slideToAnswerMode == SlideToAnswerMode.ALWAYS,
                                onClick = { onSelectSlideToAnswerMode(SlideToAnswerMode.ALWAYS) },
                                label = { Text(if (isBn) "সবসময় স্লাইড" else "Always Slide") }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = nova.glassBorder.copy(alpha = 0.55f)
                        )

                        // Quick Reply SMS Editor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = nova.gold, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isBn) "কুইক রিপ্লাই SMS টেমপ্লেট" else "Quick Reply SMS Templates",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.preferences.quickReplies.forEach { reply ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = reply,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onRemoveQuickReply(reply) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = nova.missed, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickReplyDraft,
                                onValueChange = { quickReplyDraft = it },
                                placeholder = { Text(if (isBn) "নতুন কুইক রিপ্লাই লিখুন..." else "Add quick reply...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (quickReplyDraft.isNotBlank()) {
                                        onAddQuickReply(quickReplyDraft)
                                        quickReplyDraft = ""
                                    }
                                },
                                enabled = quickReplyDraft.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isBn) "যোগ" else "Add")
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = nova.glassBorder.copy(alpha = 0.55f)
                        )

                        // DND Mode Quiet Hours
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.DoNotDisturbOn, contentDescription = null, tint = nova.missed, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isBn) "DND মোড (বিরক্তি কমানো)" else "DND Mode (Quiet Hours)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        SettingsSwitchRow(
                            title = if (isBn) "নির্দিষ্ট সময়ে অচেনা কল সাইলেন্ট রাখুন" else "Silence calls during DND schedule",
                            subtitle = if (isBn) {
                                "সময়: ${formatMinuteOfDay(state.preferences.dndStartMinutes)} – ${formatMinuteOfDay(state.preferences.dndEndMinutes)}"
                            } else {
                                "Window: ${formatMinuteOfDay(state.preferences.dndStartMinutes)} – ${formatMinuteOfDay(state.preferences.dndEndMinutes)}"
                            },
                            checked = state.preferences.dndEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateDndConfig(
                                    enabled,
                                    state.preferences.dndStartMinutes,
                                    state.preferences.dndEndMinutes,
                                    state.preferences.dndAllowFavorites
                                )
                            },
                            tag = "switch_dnd_mode"
                        )
                        if (state.preferences.dndEnabled) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val windows = listOf(
                                    Triple(22 * 60, 6 * 60, if (isBn) "রাত ১০ – সকাল ৬" else "10 PM – 6 AM"),
                                    Triple(23 * 60, 7 * 60, if (isBn) "রাত ১১ – সকাল ৭" else "11 PM – 7 AM"),
                                    Triple(0, 8 * 60, if (isBn) "রাত ১২ – সকাল ৮" else "12 AM – 8 AM")
                                )
                                windows.forEach { (startMin, endMin, lbl) ->
                                    FilterChip(
                                        selected = state.preferences.dndStartMinutes == startMin && state.preferences.dndEndMinutes == endMin,
                                        onClick = {
                                            onUpdateDndConfig(true, startMin, endMin, state.preferences.dndAllowFavorites)
                                        },
                                        label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            SettingsSwitchRow(
                                title = if (isBn) "প্রিয়জনদের কল অনুমোদন দিন" else "Allow Favorites during DND",
                                subtitle = null,
                                checked = state.preferences.dndAllowFavorites,
                                onCheckedChange = { allow ->
                                    onUpdateDndConfig(true, state.preferences.dndStartMinutes, state.preferences.dndEndMinutes, allow)
                                },
                                tag = "switch_dnd_favorites"
                            )
                        }
                    }
                }

                // 3. SIM & SIP SETTINGS (Dual SIM Manager + USSD Balance + System SIP Accounts; ICC Communication stays REMOVED!)
                item(key = "sim_and_sip") {
                    SettingsSectionCard(
                        title = if (isBn) "SIM ও SIP সেটিংস (Dual SIM Manager)" else "SIM & SIP Settings",
                        icon = Icons.Filled.SimCard,
                        iconTint = nova.sim1
                    ) {
                        Text(
                            text = if (isBn) "ডিফল্ট কলিং লাইন নির্বাচন" else "Default Calling Line",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                DefaultRoutePreference.ASK to (if (isBn) "প্রতিবার জিজ্ঞাসা" else "Ask Always"),
                                DefaultRoutePreference.SIM_1 to "SIM 1",
                                DefaultRoutePreference.SIM_2 to "SIM 2"
                            ).forEach { (pref, label) ->
                                FilterChip(
                                    selected = state.preferences.defaultRoute == pref,
                                    onClick = { onSelectDefaultRoute(pref) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sim1AliasInput,
                                onValueChange = { sim1AliasInput = it },
                                label = { Text(if (isBn) "SIM 1 নাম" else "SIM 1 Label") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sim2AliasInput,
                                onValueChange = { sim2AliasInput = it },
                                label = { Text(if (isBn) "SIM 2 নাম" else "SIM 2 Label") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    onSaveSimAlias(0, sim1AliasInput)
                                    onSaveSimAlias(1, sim2AliasInput)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                            ) {
                                Text(if (isBn) "সেভ" else "Save")
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = nova.glassBorder.copy(alpha = 0.55f)
                        )

                        SimBalanceRow(
                            slotIndex = 0,
                            simTitle = "SIM 1",
                            carrierName = state.sim1.displayName.ifBlank { state.sim1.carrierName },
                            accent = nova.sim1,
                            container = nova.sim1Container,
                            ussdCode = sim1UssdInput,
                            onUssdChange = { sim1UssdInput = it },
                            onSaveUssd = { onSaveSimBalanceUssd(0, sim1UssdInput) },
                            onCheckBalance = {
                                onSaveSimBalanceUssd(0, sim1UssdInput)
                                onCheckSimBalance(0)
                            },
                            isChecking = state.checkingBalanceSlot == 0,
                            balanceResult = state.sim1BalanceText,
                            isBn = isBn
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SimBalanceRow(
                            slotIndex = 1,
                            simTitle = "SIM 2",
                            carrierName = state.sim2.displayName.ifBlank { state.sim2.carrierName },
                            accent = nova.sim2,
                            container = nova.sim2Container,
                            ussdCode = sim2UssdInput,
                            onUssdChange = { sim2UssdInput = it },
                            onSaveUssd = { onSaveSimBalanceUssd(1, sim2UssdInput) },
                            onCheckBalance = {
                                onSaveSimBalanceUssd(1, sim2UssdInput)
                                onCheckSimBalance(1)
                            },
                            isChecking = state.checkingBalanceSlot == 1,
                            balanceResult = state.sim2BalanceText,
                            isBn = isBn
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenSipCallingAccounts,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.SettingsPhone, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isBn) "সিস্টেম SIP ও কলিং অ্যাকাউন্টস" else "System SIP & Calling Accounts")
                        }
                    }
                }

                // 4. SPAM & BLOCK LIST (Live Caller ID + Spam Protection + Number Lookup + Blocked Numbers)
                item(key = "spam_and_block_list") {
                    val callerIdOn = state.cloudCallerId.profile?.callerIdEnabled ?: true
                    SettingsSectionCard(
                        title = if (isBn) "স্প্যাম ও ব্লক লিস্ট (${state.blockedNumbers.size})" else "Spam & Block List (${state.blockedNumbers.size})",
                        icon = Icons.Filled.Shield,
                        iconTint = nova.missed,
                        trailing = {
                            OutlinedButton(
                                onClick = onOpenAddBlockDialog,
                                modifier = Modifier.testTag("settings_add_block_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "ব্লক যোগ" else "Add", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "লাইভ কলার আইডি (Live Caller ID)" else "Live Smart Caller ID",
                            subtitle = if (isBn) "অচেনা নম্বর থেকে কল এলে নাম ও পরিচয় দেখাবে" else "Identify unknown callers & businesses automatically",
                            checked = callerIdOn,
                            onCheckedChange = onToggleCloudCallerId,
                            tag = "switch_live_caller_id"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "স্প্যাম কল প্রোটেকশন" else "Smart Spam Call Protection",
                            subtitle = if (isBn) "প্রতারণা বা বেশি রিপোর্ট হওয়া স্প্যাম কল অটো-ব্লক করবে" else "Automatically screen known scam & high-report spam callers",
                            checked = state.preferences.spamProtectionEnabled,
                            onCheckedChange = onToggleSpamProtection,
                            tag = "switch_spam_protection"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "ব্লকড কল অটো-রিজেক্ট" else "Auto-Reject Blocked Calls",
                            subtitle = if (isBn) "ব্লক করা নম্বর থেকে কল এলে সাথে সাথে কেটে দেবে" else "Automatically reject incoming calls from blocked numbers",
                            checked = state.preferences.autoRejectBlocked,
                            onCheckedChange = onToggleAutoRejectBlocked,
                            tag = "switch_auto_reject_blocked"
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenNumberSearchDashboard,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "নম্বর সার্চ" else "Number Search", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenReportSpamDialog,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "ট্যাগ / স্প্যাম রিপোর্ট" else "Tag / Report Spam", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onSyncCommunityContacts,
                                enabled = !state.cloudCallerId.isSyncingContacts,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (state.cloudCallerId.isSyncingContacts) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "কমিউনিটি সিঙ্ক" else "Community Sync", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = nova.glassBorder.copy(alpha = 0.45f)
                        )

                        if (state.blockedNumbers.isEmpty()) {
                            Text(
                                text = if (isBn) "এখনো কোনো নম্বর ব্লক করা হয়নি।" else "No blocked numbers yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.blockedNumbers.forEach { entry ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.label.ifBlank { formatDisplayNumber(entry.displayNumber) },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = formatDisplayNumber(entry.displayNumber),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onUnblockNumber(entry.normalizedNumber) },
                                                modifier = Modifier.testTag("unblock_${entry.normalizedNumber}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = strings.unblockNumber,
                                                    tint = nova.missed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. BACKUP & RESTORE
                item(key = "backup_and_restore") {
                    val lastBackupText = if (state.preferences.lastBackupTimestamp > 0L) {
                        val t = dateFmt.format(Date(state.preferences.lastBackupTimestamp))
                        if (isBn) "সর্বশেষ ব্যাকআপ: $t" else "Last backup: $t"
                    } else {
                        if (isBn) "এখনো কোনো ব্যাকআপ তৈরি হয়নি" else "No backup snapshot created yet"
                    }

                    SettingsSectionCard(
                        title = if (isBn) "ব্যাকআপ ও রিস্টোর (Backup & Restore)" else "Backup & Restore",
                        icon = Icons.Filled.Backup,
                        iconTint = nova.sim1
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "অটো কল ব্যাকআপ" else "Auto Call Backup",
                            subtitle = lastBackupText,
                            checked = state.preferences.autoCallBackupEnabled,
                            onCheckedChange = onToggleAutoCallBackup,
                            tag = "switch_auto_call_backup"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onExportBackup,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "ব্যাকআপ সেভ" else "Export JSON", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onImportBackup,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "রিস্টোর করুন" else "Restore", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // 6. PRIVACY & SECURITY
                item(key = "privacy_and_security") {
                    SettingsSectionCard(
                        title = if (isBn) "প্রাইভেসি ও সিকিউরিটি" else "Privacy & Security",
                        icon = Icons.Filled.Security,
                        iconTint = nova.gold
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "ইনকামিং কল নোটিফিকেশন" else "Incoming Call Alerts",
                            subtitle = null,
                            checked = state.preferences.notifyIncomingCalls,
                            onCheckedChange = onToggleNotifyIncoming,
                            tag = "switch_notify_incoming"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "মিসড কল নোটিফিকেশন" else "Missed Call Alerts",
                            subtitle = null,
                            checked = state.preferences.notifyMissedCalls,
                            onCheckedChange = onToggleNotifyMissed,
                            tag = "switch_notify_missed"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "চলমান কল নোটিফিকেশন" else "Ongoing Call Notification",
                            subtitle = null,
                            checked = state.preferences.notifyOngoingCalls,
                            onCheckedChange = onToggleNotifyOngoing,
                            tag = "switch_notify_ongoing"
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenSystemNotificationSettings,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isBn) "নোটিফিকেশন সেটিংস" else "Notification Settings", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = onOpenAppInfoSettings,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isBn) "অ্যাপ পারমিশন" else "Permissions", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = nova.glassBorder.copy(alpha = 0.45f)
                        )

                        OutlinedButton(
                            onClick = { showClearDataConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, nova.missed.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = nova.missed, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "লোকাল কল নোট ও ডাটা মুছুন" else "Clear Local Notes & App Data",
                                color = nova.missed,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (!signedInEmail.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBn) "গুগল অ্যাকাউন্ট" else "Signed-in Account",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = signedInEmail,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(onClick = onSignOut) {
                                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = nova.missed, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isBn) "লগ আউট" else "Sign Out", color = nova.missed)
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. ABOUT PRO DIALER
                item(key = "about_pro_dialer") {
                    SettingsSectionCard(
                        title = if (isBn) "PRO DIALER সম্পর্কে (About)" else "About PRO DIALER",
                        icon = Icons.Filled.Info,
                        iconTint = nova.sim2
                    ) {
                        Text(
                            text = "Shajamal • PRO DIALER v2.0",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBn) {
                                "ডুয়াল সিম স্মার্ট ডায়ালার, লাইভ কলার আইডি, স্প্যাম কল প্রোটেকশন, কল অ্যানালিটিক্স ড্যাশবোর্ড, কন্টাক্ট প্রোফাইল, কল রিমাইন্ডার, নম্বর নোটস, কুইক রিপ্লাই, DND মোড এবং অটো কল ব্যাকআপ।"
                            } else {
                                "Liquid Glass Dual SIM Dialer with Live Caller ID, Spam Call Protection, Call Analytics Dashboard, Full-Screen Contact Profile, Call Reminders, Number Notes, Quick Reply, DND Mode & Auto Call Backup."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text(if (isBn) "লোকাল ডাটা মুছবেন?" else "Clear Local Data?") },
            text = {
                Text(
                    if (isBn) "আপনার সব কল নোট, ব্লক লিস্ট ও স্পিড ডায়াল ডাটা মুছে যাবে। (ব্যাকআপ থাকলে পরে রিস্টোর করতে পারবেন)"
                    else "This clears your local call notes, blocked list, and speed dials. Exported backups can still restore them."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataConfirm = false
                        onClearLocalUserData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = nova.missed, contentColor = Color.White)
                ) {
                    Text(if (isBn) "মুছে ফেলুন" else "Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

private fun formatMinuteOfDay(totalMinutes: Int): String {
    val h24 = (totalMinutes / 60).coerceIn(0, 23)
    val m = (totalMinutes % 60).coerceIn(0, 59)
    val amPm = if (h24 < 12) "AM" else "PM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, amPm)
}

@Composable
private fun SimBalanceRow(
    slotIndex: Int,
    simTitle: String,
    carrierName: String,
    accent: Color,
    container: Color,
    ussdCode: String,
    onUssdChange: (String) -> Unit,
    onSaveUssd: () -> Unit,
    onCheckBalance: () -> Unit,
    isChecking: Boolean,
    balanceResult: String?,
    isBn: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SimCard, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$simTitle • $carrierName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onCheckBalance,
                    enabled = !isChecking,
                    modifier = Modifier.testTag("check_balance_sim_${slotIndex + 1}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color(0xFF042619)
                    )
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF042619))
                    } else {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBn) "ব্যালেন্স চেক" else "Check Balance",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ussdCode,
                    onValueChange = onUssdChange,
                    label = { Text(if (isBn) "USSD কোড (যেমন *566#)" else "USSD Code (e.g. *566#)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_${slotIndex + 1}_ussd_input")
                )
                OutlinedButton(
                    onClick = onSaveUssd,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(if (isBn) "সেভ" else "Save", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (!balanceResult.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = balanceResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = nova.glassSurface,
        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
