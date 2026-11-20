package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.cloud.CallerIdSource
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppLanguage
import com.example.data.model.CallRoute
import com.example.data.model.DeviceContact
import com.example.data.model.formatDisplayNumber
import com.example.ui.components.BrilliantStatusPill
import com.example.ui.components.ContactAvatar
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState

private data class DialKeySpec(
    val primary: String,
    val letters: String,
    val digitNum: Int? = null
)

private val DIAL_ROWS = listOf(
    listOf(
        DialKeySpec("1", "∞", 1),
        DialKeySpec("2", "ABC", 2),
        DialKeySpec("3", "DEF", 3)
    ),
    listOf(
        DialKeySpec("4", "GHI", 4),
        DialKeySpec("5", "JKL", 5),
        DialKeySpec("6", "MNO", 6)
    ),
    listOf(
        DialKeySpec("7", "PQRS", 7),
        DialKeySpec("8", "TUV", 8),
        DialKeySpec("9", "WXYZ", 9)
    ),
    listOf(
        DialKeySpec("*", "P"),
        DialKeySpec("0", "+"),
        DialKeySpec("#", "W")
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(
    state: DialerUiState,
    strings: NovaStrings,
    onDigitPress: (String) -> Unit,
    onDigitLongPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onWhatsApp: (String) -> Unit,
    onAddContact: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onOpenNoteDialog: (String, String?) -> Unit,
    onOpenBridgeTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onOpenTagCaller: (String, String?) -> Unit,
    onSelectT9Contact: (DeviceContact) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val haptic = LocalHapticFeedback.current
    val speedDialMap = state.speedDials.associateBy { it.digit }
    val isBn = state.preferences.language == AppLanguage.BENGALI

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (nova.isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                nova.brilliant.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.08f),
                            radius = size.width * 0.7f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                nova.gold.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.25f),
                            radius = size.width * 0.55f
                        )
                    )
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Luxury Brand + Bridge Status + Settings Gear
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimMiniDot(label = state.sim1.displayName, color = nova.sim1)
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            SimMiniDot(label = state.sim2.displayName, color = nova.sim2)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BrilliantStatusPill(
                            status = state.brilliantStatus,
                            strings = strings,
                            onClick = onOpenBridgeTab
                        )
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenSettings)
                                .testTag("open_settings_button"),
                            shape = CircleShape,
                            color = nova.glassSurface,
                            border = BorderStroke(1.dp, nova.glassBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.cd_settings),
                                    tint = nova.gold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Compact 1-tap banner to make NovaCall the Default Phone App for in-app outgoing & incoming calls
                AnimatedVisibility(visible = !state.isDefaultDialer && state.dialedDigits.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onRequestDefaultDialer)
                            .testTag("keypad_default_dialer_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = nova.goldContainer.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, nova.gold.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = nova.gold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBn) "ইনকামিং ও আউটগোয়িং কল এই অ্যাপে রাখতে ট্যাপ করে ডিফল্ট ফোন করুন"
                                else "Tap to set Shajamal as Default Phone for in-app calls",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Intelligence & T9 Smart Match Zone
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Blocked Warning Banner
                AnimatedVisibility(visible = state.isDialedBlocked, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = nova.missedContainer,
                        border = BorderStroke(1.dp, nova.missed.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, tint = nova.missed, modifier = Modifier.size(16.dp))
                            Text(
                                text = strings.blockedWarningBanner,
                                style = MaterialTheme.typography.labelMedium,
                                color = nova.missed
                            )
                        }
                    }
                }

                // Active Caller Note Context Banner
                AnimatedVisibility(visible = state.activeDialedNote != null, enter = fadeIn(), exit = fadeOut()) {
                    state.activeDialedNote?.let { note ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = nova.goldContainer.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, nova.gold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.StickyNote2, contentDescription = null, tint = nova.gold, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${strings.callerNotePrefix}: ${note.note}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Smart Brilliant Rate-Saver Tip
                AnimatedVisibility(visible = state.suggestBrilliantForDialed, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable {
                                onCallRoute(state.dialedDigits, CallRoute.BRILLIANT, null)
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = nova.brilliantContainer,
                        border = BorderStroke(1.dp, nova.brilliant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = nova.brilliant, modifier = Modifier.size(16.dp))
                            Text(
                                text = strings.smartSuggestedBrilliant,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Live Truecaller-style Unknown Number Caller ID Card
                val lookup = state.cloudCallerId.dialedLookup
                AnimatedVisibility(
                    visible = state.dialedDigits.length >= 3 && (lookup != null || state.cloudCallerId.isLookingUp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val isSpam = lookup?.isSpam == true
                    val hasName = !lookup?.displayName.isNullOrBlank()
                    val accent = when {
                        isSpam -> nova.missed
                        hasName -> nova.sim1
                        else -> nova.gold
                    }
                    val container = when {
                        isSpam -> nova.missedContainer.copy(alpha = 0.85f)
                        hasName -> nova.sim1Container.copy(alpha = 0.8f)
                        else -> nova.glassSurface
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenTagCaller(state.dialedDigits, lookup?.displayName) }
                            .testTag("dialpad_caller_id_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = container,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.cloudCallerId.isLookingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = accent
                                )
                            } else {
                                Icon(
                                    imageVector = if (isSpam) Icons.Filled.ReportProblem else Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        hasName -> lookup!!.displayName!!
                                        state.cloudCallerId.isLookingUp -> if (isBn) "কলার আইডি খোঁজা হচ্ছে..." else "Searching Caller ID..."
                                        else -> if (isBn) "অচেনা নম্বর" else "Unknown Number"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSpam) nova.missed else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitleText = buildString {
                                    lookup?.carrierOrRegion?.takeIf { it.isNotBlank() }?.let { append(it) }
                                    if (isSpam) {
                                        if (isNotEmpty()) append(" • ")
                                        append(if (isBn) "স্প্যাম রিপোর্ট (${lookup?.spamReports ?: 1})" else "Spam (${lookup?.spamReports ?: 1})")
                                    } else if (lookup?.source == CallerIdSource.COMMUNITY_CLOUD) {
                                        if (isNotEmpty()) append(" • ")
                                        append("Shajamal Caller ID")
                                    } else if (lookup?.source == CallerIdSource.SERVICE_DIRECTORY) {
                                        if (isNotEmpty()) append(" • ")
                                        append(if (isBn) "ভেরিফায়েড সার্ভিস" else "Verified Service")
                                    }
                                }
                                if (subtitleText.isNotBlank()) {
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accent.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (hasName) (if (isBn) "আপডেট" else "Edit") else (if (isBn) "নাম দিন" else "Tag"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = accent
                                    )
                                }
                            }
                        }
                    }
                }

                // T9 Live Contact Matches
                AnimatedVisibility(visible = state.t9Matches.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(state.t9Matches, key = { it.id }) { contact ->
                            T9MatchChip(
                                contact = contact,
                                onSelect = { onSelectT9Contact(contact) },
                                onBrilliantCall = {
                                    onCallRoute(contact.phoneNumber, CallRoute.BRILLIANT, contact.name)
                                }
                            )
                        }
                    }
                }

                // Number Display Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.dialedDigits.isEmpty()) {
                            Text(
                                text = strings.enterNumberHint,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val formatted = formatDisplayNumber(state.dialedDigits)
                            val fontSize = when {
                                formatted.length <= 12 -> 34.sp
                                formatted.length <= 16 -> 27.sp
                                else -> 22.sp
                            }
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = fontSize,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("dialed_number_display")
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.dialedDigits.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        if (state.preferences.hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onBackspace()
                                    },
                                    onLongClick = {
                                        if (state.preferences.hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        onClearAll()
                                    }
                                )
                                .testTag("keypad_backspace_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.cd_backspace),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Quick Utility Strip when number entered
                AnimatedVisibility(visible = state.dialedDigits.length >= 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickUtilityChip(
                            icon = Icons.Filled.PersonAdd,
                            label = strings.addContact,
                            tint = nova.gold,
                            tag = "quick_add_contact_chip",
                            onClick = { onAddContact(state.dialedDigits) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        QuickUtilityChip(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = strings.whatsapp,
                            tint = nova.whatsApp,
                            tag = "quick_whatsapp_chip",
                            onClick = { onWhatsApp(state.dialedDigits) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        QuickUtilityChip(
                            icon = Icons.Filled.StickyNote2,
                            label = strings.addNote,
                            tint = nova.brilliant,
                            tag = "quick_note_chip",
                            onClick = { onOpenNoteDialog(state.dialedDigits, state.t9Matches.firstOrNull()?.name) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        QuickUtilityChip(
                            icon = Icons.Filled.ContentCopy,
                            label = "",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            tag = "quick_copy_chip",
                            onClick = { onCopyNumber(state.dialedDigits) }
                        )
                    }
                }
            }

            // Compact Glassmorphic T9 Keypad Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                DIAL_ROWS.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        row.forEach { spec ->
                            val speedDial = spec.digitNum?.let { speedDialMap[it] }
                            DialKeyButton(
                                spec = spec,
                                speedDial = speedDial,
                                hapticEnabled = state.preferences.hapticEnabled,
                                onPress = { onDigitPress(spec.primary) },
                                onLongPress = { onDigitLongPress(spec.primary) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signature Triple-Line Call Dock: SIM 1 | Brilliant Connect | SIM 2
            TripleLineCallDock(
                sim1Name = state.sim1.displayName,
                sim2Name = state.sim2.displayName,
                brilliantLabel = strings.brilliantShort,
                brilliantInstalled = state.brilliantStatus.isInstalled,
                highlightBrilliant = state.suggestBrilliantForDialed,
                onCallSim1 = { onCallRoute(state.dialedDigits, CallRoute.SIM_1, null) },
                onCallBrilliant = { onCallRoute(state.dialedDigits, CallRoute.BRILLIANT, null) },
                onCallSim2 = { onCallRoute(state.dialedDigits, CallRoute.SIM_2, null) }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SimMiniDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label.take(12),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun T9MatchChip(
    contact: DeviceContact,
    onSelect: () -> Unit,
    onBrilliantCall: () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = nova.glassSurface,
        border = BorderStroke(1.dp, nova.glassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 32.dp)
            Column(modifier = Modifier.widthIn(max = 135.dp)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(nova.brilliantContainer)
                    .clickable(onClick = onBrilliantCall),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = "Brilliant Call",
                    tint = nova.brilliant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickUtilityChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(50),
        color = nova.glassSurface,
        border = BorderStroke(0.75.dp, nova.glassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label.ifEmpty { "Copy" },
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKeyButton(
    spec: DialKeySpec,
    speedDial: SpeedDialEntity?,
    hapticEnabled: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onPress()
                },
                onLongClick = {
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onLongPress()
                }
            )
            .testTag("dial_key_${spec.primary}"),
        shape = RoundedCornerShape(16.dp),
        color = nova.glassSurface.copy(alpha = if (nova.isDark) 0.88f else 0.96f),
        border = BorderStroke(
            width = 1.dp,
            color = if (speedDial != null) nova.gold.copy(alpha = 0.45f) else nova.glassBorder.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = spec.primary,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 23.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (speedDial != null) {
                Text(
                    text = speedDial.name.take(8),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 10.sp),
                    color = nova.gold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = spec.letters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 1.sp,
                        lineHeight = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun TripleLineCallDock(
    sim1Name: String,
    sim2Name: String,
    brilliantLabel: String,
    brilliantInstalled: Boolean,
    highlightBrilliant: Boolean,
    onCallSim1: () -> Unit,
    onCallBrilliant: () -> Unit,
    onCallSim2: () -> Unit
) {
    val nova = LocalNovaColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SIM 1 Button
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onCallSim1)
                .testTag("call_sim1_button"),
            shape = RoundedCornerShape(18.dp),
            color = nova.sim1Container,
            border = BorderStroke(1.2.dp, nova.sim1.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = stringResource(R.string.cd_call_sim1),
                        tint = nova.sim1,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SIM 1",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = nova.sim1
                    )
                }
                Text(
                    text = sim1Name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Center Hero: Brilliant Connect VoIP Bridge Button
        Box(
            modifier = Modifier
                .weight(1.25f)
                .height(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            nova.brilliantGlow,
                            nova.brilliant,
                            if (highlightBrilliant) nova.gold else nova.brilliant
                        )
                    )
                )
                .border(
                    width = if (highlightBrilliant) 2.dp else 1.2.dp,
                    color = if (highlightBrilliant) nova.gold else Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onCallBrilliant)
                .testTag("call_brilliant_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = stringResource(R.string.cd_call_brilliant),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = brilliantLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (brilliantInstalled) Color(0xFF69F0AE) else Color(0xFFFFD54F))
                    )
                    Text(
                        text = if (brilliantInstalled) "VoIP Direct" else "Connect App",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // SIM 2 Button
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onCallSim2)
                .testTag("call_sim2_button"),
            shape = RoundedCornerShape(18.dp),
            color = nova.sim2Container,
            border = BorderStroke(1.2.dp, nova.sim2.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SimCard,
                        contentDescription = stringResource(R.string.cd_call_sim2),
                        tint = nova.sim2,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SIM 2",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = nova.sim2
                    )
                }
                Text(
                    text = sim2Name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
