package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.BrilliantLaunchMode
import com.example.data.model.CallRoute
import com.example.data.model.DefaultRoutePreference
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrilliantToolsScreen(
    state: DialerUiState,
    strings: NovaStrings,
    onTestBridge: () -> Unit,
    onInstallBrilliant: () -> Unit,
    onUpdateBrilliantPackage: (String) -> Unit,
    onUpdateLaunchMode: (BrilliantLaunchMode) -> Unit,
    onUpdateAutoBd: (Boolean) -> Unit,
    onUpdateAutoCopy: (Boolean) -> Unit,
    onUpdateSmartPrefixes: (String) -> Unit,
    onUpdateDefaultRoute: (DefaultRoutePreference) -> Unit,
    onUpdateSimAlias: (Int, String) -> Unit,
    onOpenAddNoteDialog: () -> Unit,
    onDeleteNote: (String) -> Unit,
    onCallNumber: (String, CallRoute, String?) -> Unit,
    onOpenAddBlockDialog: () -> Unit,
    onUnblockNumber: (String) -> Unit,
    onAddQuickReply: (String) -> Unit,
    onRemoveQuickReply: (String) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleDtmf: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    var pkgInput by remember(state.preferences.brilliantPackageName) {
        mutableStateOf(state.preferences.brilliantPackageName)
    }
    var prefixInput by remember(state.preferences.brilliantSmartPrefixes) {
        mutableStateOf(state.preferences.brilliantSmartPrefixes)
    }
    var sim1Input by remember(state.preferences.sim1Alias) { mutableStateOf(state.preferences.sim1Alias) }
    var sim2Input by remember(state.preferences.sim2Alias) { mutableStateOf(state.preferences.sim2Alias) }
    var newReplyInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            // 1. Hero Brilliant Connect Bridge Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brilliant_bridge_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = nova.glassSurface,
                    border = BorderStroke(1.2.dp, nova.brilliant.copy(alpha = 0.55f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_brilliant_bridge_banner_1795084765603),
                            contentDescription = strings.bridgeTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(212.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(212.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xBB090B10),
                                            Color(0xDD090B10),
                                            Color(0xF5090B10)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (state.brilliantStatus.isInstalled) {
                                        nova.sim1Container.copy(alpha = 0.92f)
                                    } else {
                                        nova.goldContainer.copy(alpha = 0.92f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (state.brilliantStatus.isInstalled) nova.sim1 else nova.gold
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (state.brilliantStatus.isInstalled) nova.sim1 else nova.gold)
                                        )
                                        Text(
                                            text = if (state.brilliantStatus.isInstalled) {
                                                strings.brilliantConnected
                                            } else {
                                                strings.brilliantNotInstalled
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = nova.brilliant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = strings.bridgeTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.bridgeSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onTestBridge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_brilliant_bridge_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = nova.brilliant,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Launch,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.brilliantStatus.isInstalled) strings.openBrilliant else strings.testBridge,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!state.brilliantStatus.isInstalled) {
                                    OutlinedButton(
                                        onClick = onInstallBrilliant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("install_brilliant_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, nova.gold),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = nova.gold)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Play Store",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Brilliant Routing Configuration Card
            item {
                ToolsSectionCard(
                    title = strings.bridgeModeLabel,
                    icon = Icons.Filled.Bolt,
                    iconTint = nova.brilliant
                ) {
                    val modes = listOf(
                        BrilliantLaunchMode.SMART_AUTO to "Smart Auto",
                        BrilliantLaunchMode.DIRECT_CALL_INTENT to "Direct Call",
                        BrilliantLaunchMode.DIAL_INTENT to "Dial Intent",
                        BrilliantLaunchMode.SIP_URI to "SIP URI",
                        BrilliantLaunchMode.LAUNCH_AND_COPY to "App + Copy"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        modes.forEach { (mode, label) ->
                            FilterChip(
                                selected = state.preferences.brilliantLaunchMode == mode,
                                onClick = { onUpdateLaunchMode(mode) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = nova.brilliantContainer,
                                    selectedLabelColor = nova.brilliant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ToggleSettingRow(
                        title = strings.bridgeAutoBdTitle,
                        subtitle = strings.bridgeAutoBdDesc,
                        checked = state.preferences.brilliantAutoFormatBd,
                        onCheckedChange = onUpdateAutoBd,
                        testTag = "toggle_auto_bd"
                    )

                    ToggleSettingRow(
                        title = strings.bridgeCopyBackupTitle,
                        subtitle = strings.bridgeCopyBackupDesc,
                        checked = state.preferences.brilliantAutoCopyClipboard,
                        onCheckedChange = onUpdateAutoCopy,
                        testTag = "toggle_copy_backup"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prefixInput,
                            onValueChange = { prefixInput = it },
                            label = { Text(strings.bridgeSmartPrefixTitle) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = nova.brilliant,
                                unfocusedBorderColor = nova.glassBorder
                            )
                        )
                        Button(
                            onClick = { onUpdateSmartPrefixes(prefixInput) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.save, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pkgInput,
                            onValueChange = { pkgInput = it },
                            label = { Text(strings.bridgePackageTitle) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = nova.brilliant,
                                unfocusedBorderColor = nova.glassBorder
                            )
                        )
                        Button(
                            onClick = { onUpdateBrilliantPackage(pkgInput) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.save, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 3. Dual-SIM Manager Card
            item {
                ToolsSectionCard(
                    title = strings.dualSimManagerTitle,
                    icon = Icons.Filled.SimCard,
                    iconTint = nova.sim1
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SimStatusTile(
                            slotLabel = "SIM 1",
                            carrier = state.sim1.carrierName,
                            isActive = state.sim1.isDetected,
                            statusText = if (state.sim1.isDetected) strings.simActive else strings.simStandby,
                            accent = nova.sim1,
                            container = nova.sim1Container,
                            modifier = Modifier.weight(1f)
                        )
                        SimStatusTile(
                            slotLabel = "SIM 2",
                            carrier = state.sim2.carrierName,
                            isActive = state.sim2.isDetected,
                            statusText = if (state.sim2.isDetected) strings.simActive else strings.simStandby,
                            accent = nova.sim2,
                            container = nova.sim2Container,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.defaultLineTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val routes = listOf(
                        DefaultRoutePreference.ASK to strings.askEveryTime,
                        DefaultRoutePreference.SIM_1 to "SIM 1",
                        DefaultRoutePreference.SIM_2 to "SIM 2",
                        DefaultRoutePreference.BRILLIANT to strings.brilliantShort
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
                            label = { Text("SIM 1 Alias") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = sim2Input,
                            onValueChange = { sim2Input = it },
                            label = { Text("SIM 2 Alias") },
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

            // 4. Call Notes & Callback Reminders
            item {
                ToolsSectionCard(
                    title = "${strings.notesRemindersTitle} (${state.callNotes.size})",
                    icon = Icons.Filled.StickyNote2,
                    iconTint = nova.gold,
                    trailingAction = {
                        IconButton(
                            onClick = onOpenAddNoteDialog,
                            modifier = Modifier.testTag("add_note_tools_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = strings.addNote, tint = nova.gold)
                        }
                    }
                ) {
                    if (state.callNotes.isEmpty()) {
                        Text(
                            text = strings.notePlaceholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.callNotes.forEach { note ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = note.contactName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (note.hasReminder) {
                                                    Icon(
                                                        imageVector = Icons.Filled.NotificationsActive,
                                                        contentDescription = strings.reminderLabel,
                                                        tint = nova.gold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = note.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onCallNumber(note.displayNumber, CallRoute.BRILLIANT, note.contactName) }) {
                                            Icon(Icons.Filled.Bolt, contentDescription = strings.brilliant, tint = nova.brilliant)
                                        }
                                        IconButton(onClick = { onCallNumber(note.displayNumber, CallRoute.SIM_1, note.contactName) }) {
                                            Icon(Icons.Filled.Call, contentDescription = strings.call, tint = nova.sim1)
                                        }
                                        IconButton(onClick = { onDeleteNote(note.normalizedNumber) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = strings.delete, tint = nova.missed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Spam & Blocked List
            item {
                ToolsSectionCard(
                    title = "${strings.blockedListTitle} (${state.blockedNumbers.size})",
                    icon = Icons.Filled.Block,
                    iconTint = nova.missed,
                    trailingAction = {
                        IconButton(
                            onClick = onOpenAddBlockDialog,
                            modifier = Modifier.testTag("add_block_tools_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = strings.blockNumber, tint = nova.missed)
                        }
                    }
                ) {
                    if (state.blockedNumbers.isEmpty()) {
                        Text(
                            text = strings.addBlockedHint,
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
                                            val subtitle = listOf(blocked.displayNumber, blocked.reason)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • ")
                                            Text(
                                                text = subtitle,
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

            // 6. Quick SMS Templates
            item {
                ToolsSectionCard(
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
                                value = newReplyInput,
                                onValueChange = { newReplyInput = it },
                                placeholder = { Text(strings.addQuickReplyHint) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    if (newReplyInput.isNotBlank()) {
                                        onAddQuickReply(newReplyInput)
                                        newReplyInput = ""
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

            // 7. Personalization & Theme
            item {
                ToolsSectionCard(
                    title = strings.personalizationTitle,
                    icon = Icons.Filled.Tune,
                    iconTint = nova.gold
                ) {
                    ToggleSettingRow(
                        title = strings.darkModeLabel,
                        subtitle = null,
                        checked = state.preferences.darkThemeEnabled,
                        onCheckedChange = onToggleDarkTheme,
                        testTag = "toggle_dark_theme"
                    )
                    ToggleSettingRow(
                        title = strings.keypadSoundLabel,
                        subtitle = null,
                        checked = state.preferences.dtmfSoundEnabled,
                        onCheckedChange = onToggleDtmf,
                        testTag = "toggle_dtmf_sound"
                    )
                    ToggleSettingRow(
                        title = strings.hapticLabel,
                        subtitle = null,
                        checked = state.preferences.hapticEnabled,
                        onCheckedChange = onToggleHaptic,
                        testTag = "toggle_haptic"
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

@Composable
private fun ToolsSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    trailingAction: (@Composable () -> Unit)? = null,
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                trailingAction?.invoke()
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
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
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SimStatusTile(
    slotLabel: String,
    carrier: String,
    isActive: Boolean,
    statusText: String,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = container.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = slotLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) accent else accent.copy(alpha = 0.35f))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = carrier,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
