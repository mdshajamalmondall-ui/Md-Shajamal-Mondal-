package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppLanguage
import com.example.data.model.CallRoute
import com.example.data.model.formatDisplayNumber
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.CallerDetailTarget
import com.example.ui.viewmodel.DialerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallerDetailBottomSheet(
    target: CallerDetailTarget,
    state: DialerUiState,
    strings: NovaStrings,
    onDismiss: () -> Unit,
    onCallRoute: (CallRoute) -> Unit,
    onWhatsApp: () -> Unit,
    onQuickSms: (String) -> Unit,
    onCopy: () -> Unit,
    onSaveContact: () -> Unit,
    onOpenNoteDialog: () -> Unit,
    onOpenSpeedDialDialog: () -> Unit,
    onToggleBlock: (Boolean) -> Unit
) {
    val nova = LocalNovaColors.current
    val isBlocked = state.blockedNumbers.any { it.normalizedNumber == target.normalizedNumber }
    val existingNote = state.callNotes.firstOrNull { it.normalizedNumber == target.normalizedNumber }
    val matchedPhotoUri = remember(target.normalizedNumber, state.contacts) {
        val tail = target.normalizedNumber.takeLast(10)
        state.contacts.firstOrNull {
            it.normalizedNumber == target.normalizedNumber ||
                (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
        }?.photoUri
    }
    var showSmsTemplates by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(
                    name = target.name ?: target.number,
                    photoUri = matchedPhotoUri,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.name?.takeIf { it.isNotBlank() } ?: formatDisplayNumber(target.number),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDisplayNumber(target.number),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (existingNote != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${strings.callerNotePrefix}: ${existingNote.note}",
                            style = MaterialTheme.typography.labelMedium,
                            color = nova.gold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Triple-Line Call Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SheetCallButton(
                    icon = Icons.Filled.Call,
                    label = state.sim1.displayName,
                    sub = "SIM 1",
                    fg = nova.sim1,
                    bg = nova.sim1Container,
                    onClick = {
                        onDismiss()
                        onCallRoute(CallRoute.SIM_1)
                    },
                    modifier = Modifier.weight(1f)
                )
                SheetCallButton(
                    icon = Icons.Filled.Bolt,
                    label = strings.brilliantShort,
                    sub = "VoIP",
                    fg = Color.White,
                    bg = nova.brilliant,
                    onClick = {
                        onDismiss()
                        onCallRoute(CallRoute.BRILLIANT)
                    },
                    modifier = Modifier.weight(1.1f)
                )
                SheetCallButton(
                    icon = Icons.Filled.SimCard,
                    label = state.sim2.displayName,
                    sub = "SIM 2",
                    fg = nova.sim2,
                    bg = nova.sim2Container,
                    onClick = {
                        onDismiss()
                        onCallRoute(CallRoute.SIM_2)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = nova.glassBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            // Action Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SheetMiniAction(Icons.AutoMirrored.Filled.Message, strings.whatsapp, nova.whatsApp) {
                    onDismiss()
                    onWhatsApp()
                }
                SheetMiniAction(Icons.AutoMirrored.Filled.Message, strings.sms, nova.sim2) {
                    showSmsTemplates = !showSmsTemplates
                }
                SheetMiniAction(Icons.Filled.StickyNote2, strings.callerNotePrefix, nova.gold) {
                    onDismiss()
                    onOpenNoteDialog()
                }
                SheetMiniAction(Icons.Filled.Speed, "Speed", nova.brilliant) {
                    onDismiss()
                    onOpenSpeedDialDialog()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SheetMiniAction(Icons.Filled.PersonAdd, strings.addContact, nova.gold) {
                    onDismiss()
                    onSaveContact()
                }
                SheetMiniAction(Icons.Filled.ContentCopy, "Copy", MaterialTheme.colorScheme.onSurfaceVariant) {
                    onCopy()
                    onDismiss()
                }
                SheetMiniAction(
                    icon = Icons.Filled.Block,
                    label = if (isBlocked) strings.unblockNumber else strings.blockNumber,
                    tint = nova.missed
                ) {
                    onToggleBlock(isBlocked)
                    onDismiss()
                }
            }

            if (showSmsTemplates) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = strings.quickSmsTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = nova.gold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.preferences.quickReplies.forEach { template ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismiss()
                                    onQuickSms(template)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = template,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseLineBottomSheet(
    target: CallerDetailTarget,
    state: DialerUiState,
    strings: NovaStrings,
    onDismiss: () -> Unit,
    onSelectRoute: (CallRoute) -> Unit
) {
    val nova = LocalNovaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.chooseLineTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = target.name?.takeIf { it.isNotBlank() } ?: formatDisplayNumber(target.number),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            RouteOptionRow(
                icon = Icons.Filled.Call,
                title = "SIM 1 • ${state.sim1.displayName}",
                subtitle = state.sim1.carrierName,
                accent = nova.sim1,
                container = nova.sim1Container,
                onClick = {
                    onDismiss()
                    onSelectRoute(CallRoute.SIM_1)
                }
            )
            RouteOptionRow(
                icon = Icons.Filled.SimCard,
                title = "SIM 2 • ${state.sim2.displayName}",
                subtitle = state.sim2.carrierName,
                accent = nova.sim2,
                container = nova.sim2Container,
                onClick = {
                    onDismiss()
                    onSelectRoute(CallRoute.SIM_2)
                }
            )
            RouteOptionRow(
                icon = Icons.Filled.Bolt,
                title = strings.brilliant,
                subtitle = if (state.brilliantStatus.isInstalled) strings.brilliantConnected else strings.brilliantNotInstalled,
                accent = nova.brilliant,
                container = nova.brilliantContainer,
                onClick = {
                    onDismiss()
                    onSelectRoute(CallRoute.BRILLIANT)
                }
            )
        }
    }
}

@Composable
private fun RouteOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    container: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = container.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SheetCallButton(
    icon: ImageVector,
    label: String,
    sub: String,
    fg: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SheetMiniAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CallNoteDialog(
    initialNumber: String,
    initialName: String,
    initialNote: String,
    initialReminder: Boolean,
    strings: NovaStrings,
    onDismiss: () -> Unit,
    onSave: (number: String, name: String, note: String, hasReminder: Boolean) -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf(initialNote) }
    var hasReminder by remember { mutableStateOf(initialReminder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addNote) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(strings.numberPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.namePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.notePlaceholder) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hasReminder = !hasReminder }
                ) {
                    Checkbox(checked = hasReminder, onCheckedChange = { hasReminder = it })
                    Text(strings.reminderLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank() && note.isNotBlank()) {
                        onSave(number, name, note, hasReminder)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_note_dialog_button")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun BlockNumberDialog(
    initialNumber: String,
    initialLabel: String,
    strings: NovaStrings,
    onDismiss: () -> Unit,
    onConfirm: (number: String, label: String, reason: String) -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    var label by remember { mutableStateOf(initialLabel) }
    var reason by remember { mutableStateOf("Spam") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.blockNumber) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(strings.numberPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(strings.namePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(strings.reasonPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank()) {
                        onConfirm(number, label, reason)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_block_dialog_button")
            ) { Text(strings.blockNumber) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun SpeedDialAssignDialog(
    initialDigit: Int,
    existing: SpeedDialEntity?,
    initialNumber: String,
    initialName: String,
    strings: NovaStrings,
    onDismiss: () -> Unit,
    onSave: (digit: Int, name: String, number: String, route: CallRoute) -> Unit,
    onRemove: (digit: Int) -> Unit
) {
    var selectedDigit by remember { mutableIntStateOf(initialDigit.coerceIn(1, 9)) }
    var name by remember { mutableStateOf(existing?.name ?: initialName) }
    var number by remember { mutableStateOf(existing?.phoneNumber ?: initialNumber) }
    var route by remember {
        mutableStateOf(
            existing?.preferredRoute?.let { runCatching { CallRoute.valueOf(it) }.getOrNull() }
                ?: CallRoute.SIM_1
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.assignSpeedDial} (#$selectedDigit)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..9).forEach { d ->
                        val active = d == selectedDigit
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { selectedDigit = d },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$d",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.namePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(strings.numberPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = route == CallRoute.SIM_1,
                        onClick = { route = CallRoute.SIM_1 },
                        label = { Text("SIM 1") }
                    )
                    FilterChip(
                        selected = route == CallRoute.SIM_2,
                        onClick = { route = CallRoute.SIM_2 },
                        label = { Text("SIM 2") }
                    )
                    FilterChip(
                        selected = route == CallRoute.BRILLIANT,
                        onClick = { route = CallRoute.BRILLIANT },
                        label = { Text(strings.brilliantShort) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank()) {
                        onSave(selectedDigit, name, number, route)
                        onDismiss()
                    }
                }
            ) { Text(strings.save) }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = {
                        onRemove(selectedDigit)
                        onDismiss()
                    }) { Text(strings.delete) }
                }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        }
    )
}

@Composable
fun BrilliantNotInstalledDialog(
    packageName: String,
    language: AppLanguage,
    strings: NovaStrings,
    onDismiss: () -> Unit,
    onInstallClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val nova = LocalNovaColors.current
    val body = if (language == AppLanguage.BENGALI) {
        "এই ফোনে Brilliant Connect ($packageName) অ্যাপটি পাওয়া যায়নি।\n\nব্রিলিয়ান্ট থেকে সরাসরি কল করার জন্য প্লে স্টোর থেকে Brilliant Connect ইনস্টল করুন, অথবা আপনার ফোনে ভিন্ন প্যাকেজ থাকলে ব্রিলিয়ান্ট সেটিংস থেকে প্যাকেজ নাম আপডেট করুন।"
    } else {
        "Brilliant Connect ($packageName) was not detected on this device.\n\nInstall Brilliant Connect from the Play Store to route VoIP calls directly from Shajamal, or adjust the package name in Brilliant Bridge Settings."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = nova.brilliant) },
        title = { Text(strings.brilliantNotInstalled) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onInstallClick()
                },
                colors = ButtonDefaults.buttonColors(containerColor = nova.brilliant, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.installBrilliant)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onOpenSettingsClick()
            }) {
                Text(if (language == AppLanguage.BENGALI) "ব্রিজ সেটিংস" else "Bridge Settings")
            }
        }
    )
}
