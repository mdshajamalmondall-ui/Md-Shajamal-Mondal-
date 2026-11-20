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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppFontScale
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemePalette
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.DisplayThemeMode
import com.example.data.model.IncomingCallDisplayStyle
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.SlideToAnswerMode
import com.example.data.telephony.CallRecorderManager
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
    onBack: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onOpenAppInfoSettings: () -> Unit,
    onOpenSystemDefaultApps: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSystemNotificationSettings: () -> Unit,
    onToggleNotifyIncoming: (Boolean) -> Unit,
    onToggleNotifyMissed: (Boolean) -> Unit,
    onToggleNotifyOngoing: (Boolean) -> Unit,
    onToggleVibrateOnRing: (Boolean) -> Unit,
    onToggleAutoRejectBlocked: (Boolean) -> Unit,
    onOpenAddBlockDialog: () -> Unit,
    onUnblockNumber: (String) -> Unit,
    onToggleDtmfSound: (Boolean) -> Unit,
    onSelectSoundStyle: (KeypadSoundStyle) -> Unit,
    onChangeSoundVolume: (Float) -> Unit,
    onPreviewSound: () -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleAutoSpeaker: (Boolean) -> Unit,
    onUpdateDefaultRoute: (DefaultRoutePreference) -> Unit,
    onUpdateSimAlias: (Int, String) -> Unit,
    onAddQuickReply: (String) -> Unit,
    onRemoveQuickReply: (String) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectThemePalette: (AppThemePalette) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onSelectDisplayThemeMode: (DisplayThemeMode) -> Unit,
    onSelectFontScale: (AppFontScale) -> Unit,
    onSelectIncomingCallStyle: (IncomingCallDisplayStyle) -> Unit,
    onSelectSlideToAnswerMode: (SlideToAnswerMode) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
    signedInEmail: String?,
    onToggleCloudCallerId: (Boolean) -> Unit,
    onSyncCommunityContacts: () -> Unit,
    onOpenTagCallerDialog: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val nova = LocalNovaColors.current
    val isBn = state.preferences.language == AppLanguage.BENGALI
    var sim1Input by remember(state.preferences.sim1Alias) { mutableStateOf(state.preferences.sim1Alias) }
    var sim2Input by remember(state.preferences.sim2Alias) { mutableStateOf(state.preferences.sim2Alias) }
    var newReply by remember { mutableStateOf("") }
    var recordings by remember { mutableStateOf(CallRecorderManager.listRecordings(context)) }
    var playingRecordingPath by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            CallRecorderManager.stopPlayback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
                contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
            ) {
                // 1. Default Phone App (Mandatory for In-App Incoming & Outgoing Calls!)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("default_dialer_settings_card"),
                        shape = RoundedCornerShape(22.dp),
                        color = if (state.isDefaultDialer) nova.sim1Container.copy(alpha = 0.7f) else nova.goldContainer.copy(alpha = 0.75f),
                        border = BorderStroke(
                            1.2.dp,
                            if (state.isDefaultDialer) nova.sim1 else nova.gold
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isDefaultDialer) nova.sim1.copy(alpha = 0.2f) else nova.gold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.isDefaultDialer) Icons.Filled.CheckCircle else Icons.Filled.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (state.isDefaultDialer) nova.sim1 else nova.gold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (state.isDefaultDialer) {
                                            if (isBn) "ডিফল্ট ফোন অ্যাপ সক্রিয় ✓" else "Default Phone App Active ✓"
                                        } else {
                                            if (isBn) "ডিফল্ট ফোন অ্যাপ সেট করুন" else "Set as Default Phone App"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isBn) {
                                            "এটি অন করলে আউটগোয়িং ও ইনকামিং সব কল সরাসরি Shajamal-এই আসবে এবং অন্য কোনো ফোন অ্যাপে যাবে না।"
                                        } else {
                                            "Enable this so all outgoing and incoming calls stay directly inside Shajamal instead of switching to another dialer."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!state.isDefaultDialer) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRequestDefaultDialer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("set_default_dialer_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = if (isBn) "Shajamal-কে ডিফল্ট ফোন অ্যাপ করুন" else "Make Shajamal Default Dialer",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, nova.glassBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (isBn) {
                                                "যদি 'App was denied access' দেখায় (Android 14/15):\n১) নিচের 'App Info (অনুমতি)' বাটনে চাপুন\n২) উপরের ডান কোনায় তিন ডট (⋮) মেনু থেকে 'Allow restricted settings' চালু করুন\n৩) ফিরে এসে আবার 'ডিফল্ট ফোন অ্যাপ করুন' চাপুন"
                                            } else {
                                                "If Android shows 'App was denied access':\n1) Tap 'App Info (Unlock)' below\n2) Tap the top-right 3-dot (⋮) menu -> 'Allow restricted settings'\n3) Return here and tap 'Make Shajamal Default Dialer'"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = onOpenAppInfoSettings,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("open_app_info_restricted_button"),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = if (isBn) "App Info (অনুমতি)" else "App Info (Unlock)",
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
                                                    text = if (isBn) "Default Apps" else "Default Apps",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1A. Display Options (Liquid Glass, Display Theme, Font Size, Incoming Calls, Slide to Answer)
                item {
                    SettingsSectionCard(
                        title = if (isBn) "ডিসপ্লে অপশন (Liquid Glass)" else "Display Options",
                        icon = Icons.Filled.Tune,
                        iconTint = nova.gold
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "Liquid Glass ইফেক্ট" else "Liquid Glass",
                            subtitle = if (isBn) {
                                "কল স্ক্রিন ও বাটনে স্বচ্ছ গ্লাস ব্লার ইফেক্ট চালু রাখবে (কম র‍্যামের ফোনে বন্ধ রাখতে পারেন)।"
                            } else {
                                "Enable glass blur effect on call screen & controls; may impact performance on low-end devices."
                            },
                            checked = state.preferences.liquidGlassEnabled,
                            onCheckedChange = onToggleLiquidGlass,
                            tag = "switch_liquid_glass"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = nova.glassBorder.copy(alpha = 0.5f)
                        )

                        // Display Theme (System / Light / Dark)
                        Text(
                            text = if (isBn) "ডিসপ্লে থিম (Display Theme)" else "Display Theme",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBn) {
                                "লাইট, ডার্ক অথবা ফোনের সিস্টেম অনুযায়ী অটো থিম বেছে নিন।"
                            } else {
                                "Choose Light, Dark, or System mode that adapts automatically to your device."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val themeModes = listOf(
                            DisplayThemeMode.SYSTEM to (if (isBn) "সিস্টেম" else "System"),
                            DisplayThemeMode.LIGHT to (if (isBn) "লাইট" else "Light"),
                            DisplayThemeMode.DARK to (if (isBn) "ডার্ক" else "Dark")
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            themeModes.forEach { (mode, label) ->
                                FilterChip(
                                    selected = state.preferences.displayThemeMode == mode,
                                    onClick = { onSelectDisplayThemeMode(mode) },
                                    modifier = Modifier.testTag("display_theme_${mode.name.lowercase()}"),
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = nova.sim1Container,
                                        selectedLabelColor = nova.sim1
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = nova.glassBorder.copy(alpha = 0.5f)
                        )

                        // Font Size (Small / Medium / Large)
                        Text(
                            text = if (isBn) "ফন্ট সাইজ (Font Size)" else "Font Size",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBn) {
                                "অ্যাপের লেখাগুলো পড়তে আরও আরামদায়ক করতে সাইজ ঠিক করুন।"
                            } else {
                                "Adjust the text size throughout the app to make it more comfortable for reading."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val fontSizes = listOf(
                            AppFontScale.SMALL to (if (isBn) "ছোট" else "Small"),
                            AppFontScale.MEDIUM to (if (isBn) "মাঝারি" else "Medium"),
                            AppFontScale.LARGE to (if (isBn) "বড়" else "Large")
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fontSizes.forEach { (scale, label) ->
                                FilterChip(
                                    selected = state.preferences.fontScale == scale,
                                    onClick = { onSelectFontScale(scale) },
                                    modifier = Modifier.testTag("font_scale_${scale.name.lowercase()}"),
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = nova.goldContainer,
                                        selectedLabelColor = nova.gold
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = nova.glassBorder.copy(alpha = 0.5f)
                        )

                        // Incoming Calls Style (Full Screen / Banner)
                        Text(
                            text = if (isBn) "ইনকামিং কল স্টাইল (Incoming Calls)" else "Incoming Calls",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBn) {
                                "ফোন আনলক থাকা অবস্থায় অন্য অ্যাপ চালানোর সময় ইনকামিং কল কীভাবে দেখাবে তা নির্ধারণ করুন।"
                            } else {
                                "Control how incoming calls appear while you are using other apps and the phone is unlocked."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val incomingStyles = listOf(
                            IncomingCallDisplayStyle.FULL_SCREEN to (if (isBn) "ফুল স্ক্রিন" else "Full Screen"),
                            IncomingCallDisplayStyle.BANNER to (if (isBn) "ব্যানার" else "Banner")
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            incomingStyles.forEach { (style, label) ->
                                FilterChip(
                                    selected = state.preferences.incomingCallDisplayStyle == style,
                                    onClick = { onSelectIncomingCallStyle(style) },
                                    modifier = Modifier.testTag("incoming_style_${style.name.lowercase()}"),
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = nova.sim1Container,
                                        selectedLabelColor = nova.sim1
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = nova.glassBorder.copy(alpha = 0.5f)
                        )

                        // Slide to Answer (Show only when locked / Always / Never)
                        Text(
                            text = if (isBn) "স্লাইড করে রিসিভ (Slide to Answer)" else "Slide to Answer",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBn) {
                                "ফোন লক থাকলে পকেটে ভুল করে কল রিসিভ হওয়া আটকাতে স্লাইড বার ব্যবহার করুন।"
                            } else {
                                "Use a horizontal Slide to Answer bar when the phone is locked or always."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val slideModes = listOf(
                            SlideToAnswerMode.LOCKED_ONLY to (if (isBn) "শুধু লক থাকলে" else "Only when locked"),
                            SlideToAnswerMode.ALWAYS to (if (isBn) "সবসময়" else "Always"),
                            SlideToAnswerMode.NEVER to (if (isBn) "কখনো নয়" else "Never")
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            slideModes.forEach { (mode, label) ->
                                FilterChip(
                                    selected = state.preferences.slideToAnswerMode == mode,
                                    onClick = { onSelectSlideToAnswerMode(mode) },
                                    modifier = Modifier.testTag("slide_mode_${mode.name.lowercase()}"),
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = nova.goldContainer,
                                        selectedLabelColor = nova.gold
                                    )
                                )
                            }
                        }
                    }
                }

                // 1B. Truecaller-Style Community Caller ID & Google Account
                item {
                    val cloudProfile = state.cloudCallerId.profile
                    val truecallerBlue = Color(0xFF0087FF)
                    SettingsSectionCard(
                        title = if (isBn) "Truecaller কলার আইডি • কে কল করছে জানুন" else "Truecaller Caller ID • Know Who's Calling",
                        icon = Icons.Filled.VerifiedUser,
                        iconTint = truecallerBlue
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "অচেনা নম্বরের নাম অটো-খুঁজুন" else "Live Unknown Caller ID",
                            subtitle = if (isBn) {
                                "অচেনা কল এলে বা ডায়ালপ্যাডে নম্বর তুললেই Shajamal কমিউনিটি ডেটাবেজ থেকে নাম ও অপারেটর দেখাবে।"
                            } else {
                                "Automatically identifies unknown incoming calls and dialed numbers from Shajamal's community directory."
                            },
                            checked = cloudProfile?.callerIdEnabled ?: true,
                            onCheckedChange = onToggleCloudCallerId,
                            tag = "switch_cloud_caller_id"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onSyncCommunityContacts,
                                enabled = !state.cloudCallerId.isSyncingContacts,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_community_contacts_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = truecallerBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.cloudCallerId.isSyncingContacts) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "কন্টাক্ট সিঙ্ক (${cloudProfile?.sharedContactCount ?: 0})"
                                    else "Share Names (${cloudProfile?.sharedContactCount ?: 0})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenTagCallerDialog,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_tag_caller_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isBn) "নম্বর ট্যাগ করুন" else "Tag Number",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBn) {
                                "আপনার অনুমতিতেই কেবল কন্টাক্টের নাম কমিউনিটি কলার আইডিতে শেয়ার হবে। আপনার ট্যাগ করা নম্বর: ${state.cloudCallerId.myTags.size}টি।"
                            } else {
                                "Contacts are only shared when you tap Share Names. Your contributed tags: ${state.cloudCallerId.myTags.size}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (signedInEmail != null || cloudProfile != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = nova.glassBorder.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = if (isBn) "সংযুক্ত Google অ্যাকাউন্ট" else "Signed-In Account",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = signedInEmail ?: cloudProfile?.displayName ?: "Google User",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("settings_sign_out_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBn) "সাইন আউট" else "Sign Out", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // 1C. Call Recordings Library (In-Call Recorded Audio)
                item {
                    SettingsSectionCard(
                        title = if (isBn) "কল রেকর্ডিংস (${recordings.size})" else "Call Recordings (${recordings.size})",
                        icon = Icons.Filled.FiberManualRecord,
                        iconTint = nova.missed,
                        trailing = {
                            IconButton(
                                onClick = { recordings = CallRecorderManager.listRecordings(context) },
                                modifier = Modifier.testTag("refresh_recordings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh recordings",
                                    tint = nova.gold
                                )
                            }
                        }
                    ) {
                        Text(
                            text = if (isBn) {
                                "কল করার সময় অথবা কল রিসিভ করার পর কল স্ক্রিনের 'রেকর্ড' বাটনে চাপলে অডিও এখানে সেভ হবে। পরিষ্কার ভয়েস পেতে কল চলাকালীন স্পিকার অন রাখতে পারেন।"
                            } else {
                                "Tap 'Record' on the call screen during outgoing or answered calls to save recordings here. Turning on Speaker improves remote voice capture on Android 10+."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (recordings.isEmpty()) {
                            Text(
                                text = if (isBn) "এখনো কোনো কল রেকর্ড করা হয়নি।" else "No call recordings saved yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        } else {
                            val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recordings.forEach { rec ->
                                    val isPlaying = playingRecordingPath == rec.filePath
                                    val sizeKb = (rec.sizeBytes / 1024L).coerceAtLeast(1L)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isPlaying) nova.sim1Container.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val started = CallRecorderManager.togglePlayback(rec.filePath) {
                                                        playingRecordingPath = null
                                                    }
                                                    playingRecordingPath = if (started) rec.filePath else null
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isPlaying) nova.missed else nova.sim1)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                    contentDescription = if (isPlaying) "Stop" else "Play",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rec.displayTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${dateFmt.format(Date(rec.timestampMillis))} • ${sizeKb} KB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (playingRecordingPath == rec.filePath) {
                                                        playingRecordingPath = null
                                                    }
                                                    CallRecorderManager.deleteRecording(rec.filePath)
                                                    recordings = CallRecorderManager.listRecordings(context)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = strings.delete,
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

                // 2. Notification Settings
                item {
                    SettingsSectionCard(
                        title = if (isBn) "নোটিফিকেশন সেটিংস" else "Notification Settings",
                        icon = Icons.Filled.Notifications,
                        iconTint = nova.gold
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "ইনকামিং কল ফুল-স্ক্রিন অ্যালার্ট" else "Incoming Call Full-Screen Alert",
                            subtitle = if (isBn) "কেউ কল দিলে লক স্ক্রিনে Shajamal কল স্ক্রিন দেখাবে" else "Show Shajamal full-screen incoming UI on lock screen",
                            checked = state.preferences.notifyIncomingCalls,
                            onCheckedChange = onToggleNotifyIncoming,
                            tag = "switch_notify_incoming"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "মিসড কল নোটিফিকেশন" else "Missed Call Alerts",
                            subtitle = if (isBn) "কল রিসিভ না করলে নোটিফিকেশন দেখাবে" else "Notify immediately when an incoming call is missed",
                            checked = state.preferences.notifyMissedCalls,
                            onCheckedChange = onToggleNotifyMissed,
                            tag = "switch_notify_missed"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "চলমান কল নোটিফিকেশন বার" else "Ongoing Call Status Bar",
                            subtitle = if (isBn) "কল চলাকালীন স্ট্যাটাস বারে কুইক কন্ট্রোল রাখবে" else "Show active call timer and hang-up action in status bar",
                            checked = state.preferences.notifyOngoingCalls,
                            onCheckedChange = onToggleNotifyOngoing,
                            tag = "switch_notify_ongoing"
                        )
                        SettingsSwitchRow(
                            title = if (isBn) "রিং করার সময় ভাইব্রেশন" else "Vibrate on Ring",
                            subtitle = null,
                            checked = state.preferences.vibrateOnRing,
                            onCheckedChange = onToggleVibrateOnRing,
                            tag = "switch_vibrate_ring"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!state.permissions.hasPostNotifications) {
                                Button(
                                    onClick = onRequestPermissions,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "নোটিফিকেশন অনুমতি দিন" else "Allow Notifications",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenSystemNotificationSettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("system_notif_settings_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isBn) "সিস্টেম নোটিফিকেশন সেটিংস" else "System Channel Settings",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // 3. Dialpad Sound & Haptics
                item {
                    SettingsSectionCard(
                        title = if (isBn) "ডায়ালপ্যাড সাউন্ড ও ভাইব্রেশন" else "Dialpad Sound & Vibration",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        iconTint = nova.sim1
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "ডায়ালপ্যাড কী সাউন্ড" else "Dialpad Key Sound",
                            subtitle = if (isBn) "নম্বর চাপলে স্পষ্ট ও পরিষ্কার শব্দ হবে" else "Play crisp audible tones on every keypad press",
                            checked = state.preferences.dtmfSoundEnabled,
                            onCheckedChange = onToggleDtmfSound,
                            tag = "switch_dialpad_sound"
                        )

                        if (state.preferences.dtmfSoundEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBn) "সাউন্ডের ধরন বেছে নিন (ট্যাপ করে শুনুন)" else "Sound Style (tap to preview)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val styles = listOf(
                                KeypadSoundStyle.DTMF_CLASSIC to (if (isBn) "ক্লাসিক DTMF" else "Classic DTMF"),
                                KeypadSoundStyle.STUDIO_CHIME to (if (isBn) "স্টুডিও চাইম" else "Studio Chime"),
                                KeypadSoundStyle.SOFT_CLICK to (if (isBn) "সফট ক্লিক" else "Soft Click")
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                styles.forEach { (style, label) ->
                                    FilterChip(
                                        selected = state.preferences.keypadSoundStyle == style,
                                        onClick = { onSelectSoundStyle(style) },
                                        modifier = Modifier.testTag("sound_style_${style.name.lowercase()}"),
                                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = nova.sim1Container,
                                            selectedLabelColor = nova.sim1
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "সাউন্ড ভলিউম" else "Sound Volume",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(state.preferences.keypadSoundVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = nova.gold
                                )
                            }
                            Slider(
                                value = state.preferences.keypadSoundVolume,
                                onValueChange = onChangeSoundVolume,
                                onValueChangeFinished = onPreviewSound,
                                valueRange = 0.25f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = nova.gold,
                                    activeTrackColor = nova.gold
                                )
                            )
                        }

                        SettingsSwitchRow(
                            title = strings.hapticLabel,
                            subtitle = null,
                            checked = state.preferences.hapticEnabled,
                            onCheckedChange = onToggleHaptic,
                            tag = "switch_settings_haptic"
                        )
                    }
                }

                // 4. Block List & Auto-Reject
                item {
                    SettingsSectionCard(
                        title = "${strings.blockedListTitle} (${state.blockedNumbers.size})",
                        icon = Icons.Filled.Block,
                        iconTint = nova.missed,
                        trailing = {
                            IconButton(
                                onClick = onOpenAddBlockDialog,
                                modifier = Modifier.testTag("settings_add_block_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = strings.blockNumber, tint = nova.missed)
                            }
                        }
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "ব্লকড কল অটো-রিজেক্ট করুন" else "Auto-Reject Blocked Callers",
                            subtitle = if (isBn) "ব্লক লিস্টের নম্বর থেকে কল এলে স্বয়ংক্রিয়ভাবে কেটে যাবে" else "Automatically block & reject incoming calls from blocked numbers",
                            checked = state.preferences.autoRejectBlocked,
                            onCheckedChange = onToggleAutoRejectBlocked,
                            tag = "switch_auto_reject_blocked"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.blockedNumbers.isEmpty()) {
                            Text(
                                text = if (isBn) "কোনো নম্বর ব্লক করা নেই। উপরে + চেপে নতুন নম্বর যোগ করুন।" else "No blocked numbers yet. Tap + above to block a number.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.blockedNumbers.forEach { blocked ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = nova.missedContainer.copy(alpha = 0.45f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = blocked.label,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = listOf(blocked.displayNumber, blocked.reason)
                                                        .filter { it.isNotBlank() }
                                                        .joinToString(" • "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { onUnblockNumber(blocked.normalizedNumber) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(strings.unblockNumber, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Call & Dual-SIM Settings
                item {
                    SettingsSectionCard(
                        title = if (isBn) "কল ও ডুয়াল সিম সেটিংস" else "Call & Dual-SIM Settings",
                        icon = Icons.Filled.SimCard,
                        iconTint = nova.sim1
                    ) {
                        SettingsSwitchRow(
                            title = if (isBn) "কল শুরু হলে অটো স্পিকার" else "Auto Speakerphone on Call",
                            subtitle = null,
                            checked = state.preferences.autoSpeakerOnCall,
                            onCheckedChange = onToggleAutoSpeaker,
                            tag = "switch_auto_speaker"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.defaultLineTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val routes = listOf(
                            DefaultRoutePreference.ASK to strings.askEveryTime,
                            DefaultRoutePreference.SIM_1 to "SIM 1",
                            DefaultRoutePreference.SIM_2 to "SIM 2"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            routes.forEach { (pref, label) ->
                                FilterChip(
                                    selected = state.preferences.defaultRoute == pref,
                                    onClick = { onUpdateDefaultRoute(pref) },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = sim1Input,
                                onValueChange = { sim1Input = it },
                                label = { Text("SIM 1") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            OutlinedTextField(
                                value = sim2Input,
                                onValueChange = { sim2Input = it },
                                label = { Text("SIM 2") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    onUpdateSimAlias(0, sim1Input)
                                    onUpdateSimAlias(1, sim2Input)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(strings.save, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // 6. Quick SMS Reject Templates
                item {
                    SettingsSectionCard(
                        title = strings.quickRepliesTitle,
                        icon = Icons.AutoMirrored.Filled.Message,
                        iconTint = nova.sim2
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.preferences.quickReplies.forEach { reply ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = strings.delete,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newReply,
                                    onValueChange = { newReply = it },
                                    placeholder = { Text(strings.addQuickReplyHint) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Button(
                                    onClick = {
                                        if (newReply.isNotBlank()) {
                                            onAddQuickReply(newReply)
                                            newReply = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = strings.save)
                                }
                            }
                        }
                    }
                }

                // 7. Appearance & Language
                item {
                    SettingsSectionCard(
                        title = strings.personalizationTitle,
                        icon = Icons.Filled.Tune,
                        iconTint = nova.gold
                    ) {
                        ThemePalettePicker(
                            selectedPalette = state.preferences.themePalette,
                            language = state.preferences.language,
                            onSelectPalette = onSelectThemePalette
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = nova.glassBorder.copy(alpha = 0.5f)
                        )
                        SettingsSwitchRow(
                            title = strings.darkModeLabel,
                            subtitle = null,
                            checked = state.preferences.darkThemeEnabled,
                            onCheckedChange = onToggleDarkTheme,
                            tag = "settings_dark_theme"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = nova.glassBorder.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = state.preferences.language == AppLanguage.BENGALI,
                                    onClick = { onToggleLanguage(AppLanguage.BENGALI) },
                                    label = { Text("বাংলা") }
                                )
                                FilterChip(
                                    selected = state.preferences.language == AppLanguage.ENGLISH,
                                    onClick = { onToggleLanguage(AppLanguage.ENGLISH) },
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
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
