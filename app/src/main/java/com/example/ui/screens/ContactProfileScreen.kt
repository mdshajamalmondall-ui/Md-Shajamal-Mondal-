package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CallerCategory
import com.example.data.model.AppLanguage
import com.example.data.model.CallDirection
import com.example.data.model.CallRoute
import com.example.data.model.FavoriteGroup
import com.example.data.model.formatDisplayNumber
import com.example.ui.components.ContactAvatar
import com.example.ui.components.formatCallDuration
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.CallerDetailTarget
import com.example.ui.viewmodel.DialerUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactProfileScreen(
    target: CallerDetailTarget,
    state: DialerUiState,
    strings: NovaStrings,
    onBack: () -> Unit,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onWhatsApp: (String) -> Unit,
    onQuickSms: (String, String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onSaveContact: (String, String) -> Unit,
    onToggleFavorite: (String, String?) -> Unit,
    onAssignFavoriteGroup: (String, FavoriteGroup) -> Unit,
    onSaveNote: (number: String, name: String, note: String, hasReminder: Boolean) -> Unit,
    onScheduleReminder: (number: String, name: String, note: String, delayMinutes: Long) -> Unit,
    onClearReminder: (String) -> Unit,
    onOpenTagCaller: (String, String?) -> Unit,
    onReportSpam: (number: String, label: String, alsoBlock: Boolean) -> Unit,
    onToggleBlock: (Boolean) -> Unit,
    onOpenSpeedDial: (number: String, name: String) -> Unit,
    onDeleteSingleHistoryEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val isBn = state.preferences.language == AppLanguage.BENGALI
    val targetTail = target.normalizedNumber.takeLast(10)

    val matchedContact = remember(target.normalizedNumber, state.contacts) {
        state.contacts.firstOrNull {
            it.normalizedNumber == target.normalizedNumber ||
                (targetTail.length >= 8 && it.normalizedNumber.endsWith(targetTail))
        }
    }
    val isFavorite = remember(target.normalizedNumber, state.favoriteContacts, state.preferences.favoriteNumbers) {
        state.favoriteContacts.any {
            it.normalizedNumber == target.normalizedNumber ||
                (targetTail.length >= 8 && it.normalizedNumber.endsWith(targetTail))
        } || state.preferences.favoriteNumbers.any {
            it == target.normalizedNumber || (targetTail.length >= 8 && it.endsWith(targetTail))
        }
    }
    val currentGroup = remember(target.normalizedNumber, state.preferences.favoriteGroups) {
        state.preferences.favoriteGroups.entries.firstOrNull { (k, _) ->
            k == target.normalizedNumber || (targetTail.length >= 8 && k.endsWith(targetTail))
        }?.value ?: FavoriteGroup.ALL
    }
    val isBlocked = remember(target.normalizedNumber, state.blockedNumbers) {
        state.blockedNumbers.any {
            it.normalizedNumber == target.normalizedNumber ||
                (targetTail.length >= 8 && it.normalizedNumber.endsWith(targetTail))
        }
    }
    val existingNote = remember(target.normalizedNumber, state.callNotes) {
        state.callNotes.firstOrNull {
            it.normalizedNumber == target.normalizedNumber ||
                (targetTail.length >= 8 && it.normalizedNumber.endsWith(targetTail))
        }
    }
    val reminderAtMs = remember(target.normalizedNumber, state.preferences.callReminderTimestamps) {
        state.preferences.callReminderTimestamps.entries.firstOrNull { (k, _) ->
            k == target.normalizedNumber || (targetTail.length >= 8 && k.endsWith(targetTail))
        }?.value
    }
    val historyEntries = remember(state.callLogs, target.normalizedNumber, target.number) {
        state.callLogs.filter { entry ->
            when {
                targetTail.length >= 8 && entry.normalizedNumber.length >= 8 ->
                    entry.normalizedNumber.takeLast(10) == targetTail
                target.normalizedNumber.isNotBlank() ->
                    entry.normalizedNumber == target.normalizedNumber
                else -> entry.number.trim() == target.number.trim()
            }
        }
    }

    val lookup = state.profileLookupResult
    val displayTitle = matchedContact?.name
        ?: target.name?.takeIf { it.isNotBlank() }
        ?: lookup?.displayName?.takeIf { it.isNotBlank() }
        ?: formatDisplayNumber(target.number)

    var noteInput by remember(target.normalizedNumber, existingNote?.note) {
        mutableStateOf(existingNote?.note.orEmpty())
    }
    var showQuickReplies by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault()) }

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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isBn) "ফিরে যান" else "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = if (isBn) "কন্টাক্ট প্রোফাইল" else "Contact Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleFavorite(target.number, displayTitle) },
                        modifier = Modifier.testTag("profile_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (isBn) "প্রিয়জন" else "Favorite",
                            tint = if (isFavorite) nova.gold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onSaveContact(target.number, displayTitle) },
                        modifier = Modifier.testTag("profile_edit_contact_button")
                    ) {
                        Icon(
                            imageVector = if (matchedContact != null) Icons.Filled.Edit else Icons.Filled.PersonAdd,
                            contentDescription = strings.addContact,
                            tint = nova.sim1
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
            ) {
                // Hero Profile Card
                item(key = "hero") {
                    val isSpamAlert = lookup?.isSpam == true || isBlocked
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = nova.glassSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSpamAlert) nova.missed.copy(alpha = 0.65f) else nova.glassBorder.copy(alpha = 0.8f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ContactAvatar(
                                name = displayTitle,
                                photoUri = matchedContact?.photoUri,
                                size = 84.dp,
                                accentOverride = if (isSpamAlert) nova.missed else null
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatDisplayNumber(target.number),
                                style = MaterialTheme.typography.titleSmall,
                                color = nova.sim1
                            )
                            if (!lookup?.carrierOrRegion.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lookup?.carrierOrRegion.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Smart Caller ID & Business / Spam Badges
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (lookup != null) {
                                    val isSpam = lookup.isSpam
                                    val isBiz = lookup.category == CallerCategory.BUSINESS || lookup.category == CallerCategory.DELIVERY
                                    val pillBg = when {
                                        isSpam -> nova.missedContainer
                                        isBiz -> nova.goldContainer
                                        else -> nova.sim2Container
                                    }
                                    val pillFg = when {
                                        isSpam -> nova.missed
                                        isBiz -> nova.gold
                                        else -> nova.sim2
                                    }
                                    val pillIcon = when {
                                        isSpam -> Icons.Filled.Warning
                                        isBiz -> Icons.Filled.Business
                                        else -> Icons.Filled.VerifiedUser
                                    }
                                    val catText = if (isBn) lookup.category.labelBn else lookup.category.labelEn
                                    val spamSuffix = if (lookup.spamReports > 0L) " (${lookup.spamReports})" else ""
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = pillBg
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(pillIcon, contentDescription = null, tint = pillFg, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = "$catText$spamSuffix",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = pillFg
                                            )
                                        }
                                    }
                                }
                                if (currentGroup != FavoriteGroup.ALL) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = nova.goldContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(Icons.Filled.Group, contentDescription = null, tint = nova.gold, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = if (isBn) currentGroup.labelBn else currentGroup.labelEn,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = nova.gold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 5 Quick Actions: SIM 1 | SIM 2 | WhatsApp | Quick Reply SMS | Copy
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileQuickAction(
                                    icon = Icons.Filled.Call,
                                    label = "SIM 1",
                                    tint = nova.sim1,
                                    bg = nova.sim1Container,
                                    onClick = { onCallRoute(target.number, CallRoute.SIM_1, displayTitle) }
                                )
                                ProfileQuickAction(
                                    icon = Icons.Filled.SimCard,
                                    label = "SIM 2",
                                    tint = nova.sim2,
                                    bg = nova.sim2Container,
                                    onClick = { onCallRoute(target.number, CallRoute.SIM_2, displayTitle) }
                                )
                                ProfileQuickAction(
                                    icon = Icons.AutoMirrored.Filled.Message,
                                    label = "WA",
                                    tint = nova.whatsApp,
                                    bg = nova.whatsApp.copy(alpha = 0.16f),
                                    onClick = { onWhatsApp(target.number) }
                                )
                                ProfileQuickAction(
                                    icon = Icons.AutoMirrored.Filled.Message,
                                    label = if (isBn) "রিপ্লাই" else "SMS",
                                    tint = nova.gold,
                                    bg = nova.goldContainer,
                                    onClick = { showQuickReplies = !showQuickReplies }
                                )
                                ProfileQuickAction(
                                    icon = Icons.Filled.ContentCopy,
                                    label = if (isBn) "কপি" else "Copy",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    bg = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    onClick = { onCopyNumber(target.number) }
                                )
                            }

                            // Expandable Quick Reply SMS picker
                            AnimatedVisibility(visible = showQuickReplies) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HorizontalDivider(color = nova.glassBorder.copy(alpha = 0.45f))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBn) "কুইক রিপ্লাই SMS (ট্যাপ করে পাঠান)" else "Quick Reply SMS (Tap to send)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = nova.gold
                                    )
                                    state.preferences.quickReplies.forEach { tpl ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onQuickSms(target.number, tpl) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Text(
                                                text = tpl,
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

                // Favorite Group Assignment Card
                item(key = "fav_group") {
                    ProfileSectionCard(
                        title = if (isBn) "প্রিয়জন গ্রুপ (Favorite Group)" else "Favorite Group",
                        icon = Icons.Filled.Group,
                        tint = nova.gold
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                FavoriteGroup.FAMILY,
                                FavoriteGroup.FRIENDS,
                                FavoriteGroup.OFFICE,
                                FavoriteGroup.ALL
                            ).forEach { grp ->
                                val selected = currentGroup == grp
                                val label = if (grp == FavoriteGroup.ALL) {
                                    if (isBn) "কোনোটি না" else "None"
                                } else {
                                    if (isBn) grp.labelBn else grp.labelEn
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { onAssignFavoriteGroup(target.number, grp) },
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

                // Personal Number Notes & Call Reminder Card
                item(key = "notes_and_reminder") {
                    ProfileSectionCard(
                        title = if (isBn) "ব্যক্তিগত নোট ও কল রিমাইন্ডার" else "Number Note & Call Reminder",
                        icon = Icons.Filled.StickyNote2,
                        tint = nova.sim2
                    ) {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = {
                                Text(if (isBn) "এই নম্বরের জন্য ব্যক্তিগত নোট লিখুন..." else "Write a personal note for this number...")
                            },
                            minLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_note_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (noteInput.isNotBlank()) {
                                        onSaveNote(target.number, displayTitle, noteInput, reminderAtMs != null)
                                    }
                                },
                                enabled = noteInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("profile_save_note_button")
                            ) {
                                Text(if (isBn) "নোট সেভ করুন" else "Save Note")
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = nova.glassBorder.copy(alpha = 0.45f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Alarm, contentDescription = null, tint = nova.gold, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isBn) "কল-ব্যাক রিমাইন্ডার সেট করুন" else "Schedule Call Reminder",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (reminderAtMs != null && reminderAtMs > System.currentTimeMillis() - 60_000L) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = nova.goldContainer.copy(alpha = 0.6f),
                                border = BorderStroke(0.8.dp, nova.gold.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBn) {
                                            "সক্রিয়: ${dateFmt.format(Date(reminderAtMs))}"
                                        } else {
                                            "Active: ${dateFmt.format(Date(reminderAtMs))}"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = nova.gold
                                    )
                                    IconButton(
                                        onClick = { onClearReminder(target.number) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = nova.missed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val minutesUntilTomorrow9Am = remember {
                            val now = Calendar.getInstance()
                            val tomorrow = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            ((tomorrow.timeInMillis - now.timeInMillis) / 60_000L).coerceAtLeast(60L)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                15L to (if (isBn) "১৫ মি." else "15 min"),
                                30L to (if (isBn) "৩০ মি." else "30 min"),
                                60L to (if (isBn) "১ ঘণ্টা" else "1 hour"),
                                minutesUntilTomorrow9Am to (if (isBn) "আগামীকাল ৯টা" else "Tomorrow 9 AM")
                            )
                            presets.forEach { (mins, label) ->
                                OutlinedButton(
                                    onClick = {
                                        onScheduleReminder(target.number, displayTitle, noteInput, mins)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Caller ID, Spam Report, Block & Speed Dial Card
                item(key = "protection_tools") {
                    ProfileSectionCard(
                        title = if (isBn) "স্মার্ট কলার আইডি ও সুরক্ষা" else "Smart Caller ID & Controls",
                        icon = Icons.Filled.VerifiedUser,
                        tint = Color(0xFF0087FF)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenTagCaller(target.number, displayTitle) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "নাম/বিজনেস ট্যাগ" else "Tag Name / Business", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { onOpenSpeedDial(target.number, displayTitle) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "স্পিড ডায়াল" else "Speed Dial", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { onReportSpam(target.number, displayTitle, true) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, nova.missed.copy(alpha = 0.65f))
                            ) {
                                Icon(Icons.Filled.Flag, contentDescription = null, tint = nova.missed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "স্প্যাম রিপোর্ট" else "Report Spam",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = nova.missed
                                )
                            }
                            Button(
                                onClick = { onToggleBlock(isBlocked) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBlocked) MaterialTheme.colorScheme.surfaceContainerHigh else nova.missed,
                                    contentColor = if (isBlocked) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            ) {
                                Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBlocked) strings.unblockNumber else strings.blockNumber,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // Full Call History Section
                item(key = "history_header") {
                    Text(
                        text = if (isBn) "কল হিস্ট্রি (${historyEntries.size})" else "Call History (${historyEntries.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (historyEntries.isEmpty()) {
                    item(key = "history_empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = nova.glassSurface,
                            border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = if (isBn) "এই নম্বরের কোনো কল হিস্ট্রি নেই।" else "No call history recorded for this number.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(historyEntries, key = { it.id }) { entry ->
                        val (dirIcon, dirTint, dirLabel) = when (entry.direction) {
                            CallDirection.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, nova.sim1, strings.outgoing)
                            CallDirection.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, nova.sim2, strings.incoming)
                            CallDirection.MISSED -> Triple(Icons.AutoMirrored.Filled.CallMissed, nova.missed, strings.missed)
                            CallDirection.REJECTED -> Triple(Icons.Filled.Block, nova.missed, strings.rejected)
                        }
                        val simLabel = if (entry.route == CallRoute.SIM_2) "SIM 2" else "SIM 1"
                        val durStr = formatCallDuration(entry.durationSeconds, state.preferences.language)
                            .ifEmpty { if (isBn) "০সে" else "0s" }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = nova.glassSurface,
                            border = BorderStroke(0.8.dp, nova.glassBorder.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(dirTint.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(dirIcon, contentDescription = dirLabel, tint = dirTint, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$dirLabel • $simLabel",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dateFmt.format(Date(entry.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = durStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { onDeleteSingleHistoryEntry(entry.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileQuickAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    content: @Composable () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = nova.glassSurface,
        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
