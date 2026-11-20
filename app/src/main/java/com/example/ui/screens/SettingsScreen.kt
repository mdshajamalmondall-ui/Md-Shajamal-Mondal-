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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SimCard
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
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemePalette
import com.example.data.model.BackgroundDoodleStyle
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.formatDisplayNumber
import com.example.ui.components.DoodleStylePicker
import com.example.ui.components.ThemePalettePicker
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: DialerUiState,
    strings: NovaStrings,
    onBack: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onOpenAppInfoSettings: () -> Unit,
    onOpenSystemDefaultApps: () -> Unit,
    onCheckSimBalance: (Int) -> Unit,
    onSaveSimBalanceUssd: (Int, String) -> Unit,
    defaultUssdForSlot: (Int) -> String,
    onSelectThemePalette: (AppThemePalette) -> Unit,
    onSelectBackgroundDoodle: (BackgroundDoodleStyle) -> Unit,
    onPickRingtone: () -> Unit,
    onOpenSystemSoundSettings: () -> Unit,
    onToggleDtmfSound: (Boolean) -> Unit,
    onSelectSoundStyle: (KeypadSoundStyle) -> Unit,
    onChangeSoundVolume: (Float) -> Unit,
    onPreviewSound: () -> Unit,
    onOpenSystemNotificationSettings: () -> Unit,
    onToggleNotifyIncoming: (Boolean) -> Unit,
    onToggleNotifyMissed: (Boolean) -> Unit,
    onToggleNotifyOngoing: (Boolean) -> Unit,
    onToggleVibrateOnRing: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleAutoRejectBlocked: (Boolean) -> Unit,
    onOpenAddBlockDialog: () -> Unit,
    onUnblockNumber: (String) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
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
                Text(
                    text = if (isBn) "সেটিংস" else "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
            ) {
                // 1. Default Phone App (Shown ONLY when Shajamal is not yet the default dialer)
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
                                        Icon(
                                            imageVector = Icons.Filled.PhoneAndroid,
                                            contentDescription = null,
                                            tint = nova.gold,
                                            modifier = Modifier.size(22.dp)
                                        )
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
                                                "Enable so all incoming and outgoing calls stay directly inside Shajamal."
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
                                    OutlinedButton(
                                        onClick = onOpenAppInfoSettings,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_app_info_restricted_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isBn) "App Info" else "App Info",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onOpenSystemDefaultApps,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_system_default_apps_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Default Apps",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. SIM 1 & SIM 2 Balance Check (Real USSD)
                item(key = "sim_balance") {
                    SettingsSectionCard(
                        title = if (isBn) "SIM 1 ও SIM 2 ব্যালেন্স চেক" else "SIM 1 & SIM 2 Balance Check",
                        icon = Icons.Filled.AccountBalanceWallet,
                        iconTint = nova.sim1
                    ) {
                        Text(
                            text = if (isBn) {
                                "সিমের অপারেটর অনুযায়ী অটো USSD কোড সেট করা আছে। চাইলে নিজের কোড পরিবর্তন করে সরাসরি ব্যালেন্স চেক করুন।"
                            } else {
                                "Carrier USSD codes are auto-detected. Customize the USSD code if needed and check live SIM balance."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

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
                    }
                }

                // 3. Unified Theme & Doodle Card (Black / White / Sapphire Ocean + All Doodles with Backgrounds)
                item(key = "theme_and_doodle") {
                    SettingsSectionCard(
                        title = if (isBn) "থিম ও থিম ডুডল" else "Theme & Theme Doodle",
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
                    }
                }

                // 5. Call Ringtone & Dialpad Tone
                item(key = "ringtone_and_dialpad_tone") {
                    SettingsSectionCard(
                        title = if (isBn) "কল টোন ও ডায়ালপ্যাড সাউন্ড" else "Call Ringtone & Dialpad Tone",
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
                            Spacer(modifier = Modifier.height(8.dp))
                            val soundStyles = listOf(
                                KeypadSoundStyle.DTMF_CLASSIC to (if (isBn) "ক্লাসিক" else "Classic"),
                                KeypadSoundStyle.STUDIO_CHIME to (if (isBn) "চাইম" else "Chime"),
                                KeypadSoundStyle.SOFT_CLICK to (if (isBn) "সফট ক্লিক" else "Soft Click")
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                soundStyles.forEach { (style, label) ->
                                    FilterChip(
                                        selected = state.preferences.keypadSoundStyle == style,
                                        onClick = { onSelectSoundStyle(style) },
                                        modifier = Modifier.testTag("sound_style_${style.name.lowercase()}"),
                                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = nova.gold,
                                    modifier = Modifier.size(18.dp)
                                )
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
                    }
                }

                // 6. Notifications & Vibration
                item(key = "notifications_and_vibration") {
                    SettingsSectionCard(
                        title = if (isBn) "নোটিফিকেশন ও ভাইব্রেশন" else "Notifications & Vibration",
                        icon = Icons.Filled.Notifications,
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
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = nova.glassBorder.copy(alpha = 0.45f)
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "কল এলে ভাইব্রেট" else "Vibrate on Ring",
                            subtitle = null,
                            checked = state.preferences.vibrateOnRing,
                            onCheckedChange = onToggleVibrateOnRing,
                            tag = "switch_vibrate_ring"
                        )
                        SettingsSwitchRow(
                            title = strings.hapticLabel,
                            subtitle = if (isBn) "ডায়ালপ্যাডে বাটন চাপলে হালকা ভাইব্রেশন" else "Gentle vibration on dialpad taps",
                            checked = state.preferences.hapticEnabled,
                            onCheckedChange = onToggleHaptic,
                            tag = "switch_haptic"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onOpenSystemNotificationSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_notification_settings_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isBn) "সিস্টেম নোটিফিকেশন সেটিংস" else "System Notification Settings",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // 7. Block List
                item(key = "block_list") {
                    SettingsSectionCard(
                        title = "${strings.blockedListTitle} (${state.blockedNumbers.size})",
                        icon = Icons.Filled.Block,
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
                            title = if (isBn) "ব্লকড কল অটো-রিজেক্ট" else "Auto-Reject Blocked Calls",
                            subtitle = if (isBn) "ব্লক করা নম্বর থেকে কল এলে সাথে সাথে কেটে দেবে" else "Automatically reject incoming calls from blocked numbers",
                            checked = state.preferences.autoRejectBlocked,
                            onCheckedChange = onToggleAutoRejectBlocked,
                            tag = "switch_auto_reject_blocked"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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

                // 8. Language
                item(key = "language") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = nova.glassSurface,
                        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.75f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Language, contentDescription = null, tint = nova.gold, modifier = Modifier.size(20.dp))
                                Text(
                                    text = strings.languageLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
            }
        }
    }
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
