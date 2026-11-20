package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CallerIdIntelligence
import com.example.data.model.AppLanguage
import com.example.data.model.CallDirection
import com.example.data.model.CallFilterType
import com.example.data.model.CallLogItem
import com.example.data.model.CallRoute
import com.example.data.model.formatDisplayNumber
import com.example.ui.components.ContactAvatar
import com.example.ui.components.PermissionAccessCard
import com.example.ui.components.RouteBadge
import com.example.ui.components.formatCallDuration
import com.example.ui.components.formatRelativeTime
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    state: DialerUiState,
    strings: NovaStrings,
    onSelectFilter: (CallFilterType) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onOpenCallerDetail: (String, String?) -> Unit,
    onOpenTagCaller: (String, String?) -> Unit,
    onWhatsApp: (String) -> Unit,
    onOpenNoteDialog: (String, String?) -> Unit,
    onDeleteCallLog: (CallLogItem) -> Unit,
    onLongPressCallOptions: (CallLogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val haptic = LocalHapticFeedback.current
    val isBn = state.preferences.language == AppLanguage.BENGALI
    var expandedId by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onSearchChange(spoken)
            }
        }
    }

    val blockedSet = remember(state.blockedNumbers) {
        state.blockedNumbers.map { it.normalizedNumber }.toSet()
    }
    val notesMap = remember(state.callNotes) {
        state.callNotes.associateBy { it.normalizedNumber }
    }
    val savedNumbersSet = remember(state.contacts) {
        state.contacts.map { it.normalizedNumber.takeLast(10) }.toSet()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Bar matching Liquid Glass Recents: Edit | [All | Missed] | Filter + Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { editMode = !editMode }
                        .testTag("recents_edit_button"),
                    shape = RoundedCornerShape(50),
                    color = if (editMode) MaterialTheme.colorScheme.primaryContainer else nova.glassSurface,
                    border = BorderStroke(
                        1.dp,
                        if (editMode) MaterialTheme.colorScheme.primary else nova.glassBorder.copy(alpha = 0.75f)
                    )
                ) {
                    Text(
                        text = if (editMode) {
                            if (isBn) "সম্পন্ন" else "Done"
                        } else {
                            if (isBn) "এডিট" else "Edit"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                // Segmented Liquid Glass Pill: All | Missed
                Surface(
                    shape = RoundedCornerShape(50),
                    color = nova.glassSurface.copy(alpha = if (nova.liquidGlassEnabled) 0.85f else 1f),
                    border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAll = state.recentsFilter == CallFilterType.ALL
                        val isMissed = state.recentsFilter == CallFilterType.MISSED
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isAll) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onSelectFilter(CallFilterType.ALL) }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("recents_filter_all"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.filterAll,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isAll) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isMissed) nova.missedContainer else Color.Transparent)
                                .clickable { onSelectFilter(CallFilterType.MISSED) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("recents_filter_missed"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.filterMissed,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isMissed) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isMissed) nova.missed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { showFilterMenu = true }
                                .testTag("recents_filter_menu_button"),
                            shape = CircleShape,
                            color = nova.glassSurface,
                            border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.75f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = if (isBn) "ফিল্টার" else "Filter",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.filterAll) },
                                onClick = {
                                    onSelectFilter(CallFilterType.ALL)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.filterMissed) },
                                onClick = {
                                    onSelectFilter(CallFilterType.MISSED)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.filterOutgoing) },
                                onClick = {
                                    onSelectFilter(CallFilterType.OUTGOING)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.filterIncoming) },
                                onClick = {
                                    onSelectFilter(CallFilterType.INCOMING)
                                    showFilterMenu = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isBn) "রিফ্রেশ করুন" else "Refresh") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                onClick = {
                                    onRefresh()
                                    showFilterMenu = false
                                }
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSettings)
                            .testTag("recents_settings_button"),
                        shape = CircleShape,
                        color = nova.glassSurface,
                        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.75f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = if (isBn) "সেটিংস" else "Settings",
                                tint = nova.gold,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rounded Pill Search Bar with Voice Mic (matching Screenshot 8)
            OutlinedTextField(
                value = state.recentsSearchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recents_search_input"),
                placeholder = {
                    Text(
                        text = if (isBn) "নাম বা নম্বর খুঁজুন..." else "Search recents...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.recentsSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.clearAll)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                    }
                                    voiceSearchLauncher.launch(intent)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = if (isBn) "ভয়েস সার্চ" else "Voice Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = nova.glassSurface,
                    unfocusedContainerColor = nova.glassSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = nova.glassBorder.copy(alpha = 0.65f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Truecaller-style Caller ID Status Strip + Gesture Tip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF0087FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isBn) "Truecaller আইডি সক্রিয় • আজকের কল: ${state.todayCallCount}"
                        else "Caller ID Active • Today: ${state.todayCallCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isBn) "ডানে কল • বামে ডিলিট" else "Swipe → Call • ← Del",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (state.filteredCallLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(nova.glassSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = nova.gold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.noRecentsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.noRecentsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = state.filteredCallLogs,
                        key = { it.id },
                        contentType = { "call_log_item" }
                    ) { item ->
                        val isBlocked = blockedSet.contains(item.normalizedNumber)
                        val note = notesMap[item.normalizedNumber]
                        val isSavedContact = savedNumbersSet.contains(item.normalizedNumber.takeLast(10))
                        val truecallerInfo = remember(item.number, isSavedContact) {
                            if (!isSavedContact) CallerIdIntelligence.detectOffline(item.number) else null
                        }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { targetValue ->
                                when (targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCallRoute(
                                            item.number,
                                            if (item.route == CallRoute.SIM_2) CallRoute.SIM_2 else CallRoute.SIM_1,
                                            item.contactName
                                        )
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDeleteCallLog(item)
                                        true
                                    }
                                    SwipeToDismissBoxValue.Settled -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = !editMode,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                if (direction == SwipeToDismissBoxValue.Settled) {
                                    Box(modifier = Modifier.fillMaxSize())
                                } else {
                                    val bgColor = if (direction == SwipeToDismissBoxValue.StartToEnd) nova.sim1 else nova.missed
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(bgColor)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                            Alignment.CenterStart
                                        } else {
                                            Alignment.CenterEnd
                                        }
                                    ) {
                                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Call,
                                                    contentDescription = strings.call,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    text = if (isBn) "কল করুন" else "Call",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isBn) "ডিলিট" else "Delete",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = strings.delete,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            CallLogCard(
                                item = item,
                                expanded = expandedId == item.id,
                                editMode = editMode,
                                isBlocked = isBlocked,
                                isUnknownCaller = !isSavedContact,
                                callerCarrierHint = truecallerInfo?.carrierOrRegion,
                                noteText = note?.note,
                                sim1Label = state.sim1.displayName,
                                sim2Label = state.sim2.displayName,
                                strings = strings,
                                language = state.preferences.language,
                                onToggleExpand = {
                                    expandedId = if (expandedId == item.id) null else item.id
                                },
                                onDeleteClick = { onDeleteCallLog(item) },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongPressCallOptions(item)
                                },
                                onCallRoute = { route ->
                                    onCallRoute(item.number, route, item.contactName)
                                },
                                onWhatsApp = { onWhatsApp(item.number) },
                                onAddNote = { onOpenNoteDialog(item.number, item.contactName) },
                                onTagCaller = { onOpenTagCaller(item.number, item.contactName) },
                                onOpenDetail = { onOpenCallerDetail(item.number, item.contactName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogCard(
    item: CallLogItem,
    expanded: Boolean,
    editMode: Boolean,
    isBlocked: Boolean,
    isUnknownCaller: Boolean,
    callerCarrierHint: String?,
    noteText: String?,
    sim1Label: String,
    sim2Label: String,
    strings: NovaStrings,
    language: AppLanguage,
    onToggleExpand: () -> Unit,
    onDeleteClick: () -> Unit,
    onLongPress: () -> Unit,
    onCallRoute: (CallRoute) -> Unit,
    onWhatsApp: () -> Unit,
    onAddNote: () -> Unit,
    onTagCaller: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val nova = LocalNovaColors.current
    val isBn = language == AppLanguage.BENGALI
    val (dirIcon, dirColor, dirText) = when (item.direction) {
        CallDirection.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, nova.sim1, strings.incoming)
        CallDirection.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, nova.sim2, strings.outgoing)
        CallDirection.MISSED -> Triple(Icons.AutoMirrored.Filled.CallMissed, nova.missed, strings.missed)
        CallDirection.REJECTED -> Triple(Icons.Filled.Block, nova.missed, strings.rejected)
    }

    val title = item.contactName?.takeIf { it.isNotBlank() }
        ?: formatDisplayNumber(item.number).ifBlank { strings.unknownCaller }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onToggleExpand,
                onLongClick = onLongPress
            )
            .testTag("call_log_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        color = nova.glassSurface.copy(alpha = if (nova.liquidGlassEnabled) 0.90f else 1f),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isBlocked -> nova.missed.copy(alpha = 0.55f)
                expanded -> nova.gold.copy(alpha = 0.5f)
                else -> nova.glassBorder.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editMode) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RemoveCircle,
                            contentDescription = strings.delete,
                            tint = nova.missed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                ContactAvatar(
                    name = item.contactName ?: item.number,
                    photoUri = item.photoUri,
                    size = 44.dp,
                    accentOverride = if (item.direction == CallDirection.MISSED) nova.missed else null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (item.direction == CallDirection.MISSED) nova.missed else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.callCount > 1) {
                            val badgeBg = if (item.direction == CallDirection.MISSED) {
                                nova.missedContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                            val badgeFg = if (item.direction == CallDirection.MISSED) {
                                nova.missed
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = badgeBg,
                                border = BorderStroke(0.75.dp, badgeFg.copy(alpha = 0.45f)),
                                modifier = Modifier.testTag("call_count_badge_${item.id}")
                            ) {
                                Text(
                                    text = "(${item.callCount})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (isUnknownCaller && !item.contactName.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "Truecaller ID",
                                tint = Color(0xFF0087FF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (isBlocked) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = "Blocked",
                                tint = nova.missed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = dirIcon,
                            contentDescription = dirText,
                            tint = dirColor,
                            modifier = Modifier.size(13.dp)
                        )
                        val subtitleText = remember(item.timestamp, item.durationSeconds, item.callCount, callerCarrierHint, language) {
                            val dur = formatCallDuration(item.durationSeconds, language)
                            val timeStr = formatRelativeTime(item.timestamp, language)
                            val countStr = if (item.callCount > 1) {
                                if (language == AppLanguage.BENGALI) "${item.callCount} বার" else "${item.callCount} calls"
                            } else ""
                            buildString {
                                if (countStr.isNotEmpty()) {
                                    append(countStr)
                                    append(" • ")
                                }
                                append(timeStr)
                                when {
                                    dur.isNotEmpty() -> {
                                        append(" • ")
                                        append(dur)
                                    }
                                    !callerCarrierHint.isNullOrBlank() -> {
                                        append(" • ")
                                        append(callerCarrierHint)
                                    }
                                }
                            }
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        RouteBadge(
                            route = item.route,
                            sim1Label = sim1Label,
                            sim2Label = sim2Label
                        )
                    }
                    if (!noteText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${strings.callerNotePrefix}: $noteText",
                            style = MaterialTheme.typography.labelSmall,
                            color = nova.gold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                // One-tap Call circle (matching Screenshot 8)
                val safeRoute = if (item.route == CallRoute.SIM_2) CallRoute.SIM_2 else CallRoute.SIM_1
                val quickBg = if (safeRoute == CallRoute.SIM_2) nova.sim2Container else nova.sim1Container
                val quickTint = if (safeRoute == CallRoute.SIM_2) nova.sim2 else nova.sim1
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(quickBg)
                        .clickable { onCallRoute(safeRoute) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = strings.call,
                        tint = quickTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = nova.glassBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExpandedActionPill(
                            icon = Icons.Filled.Call,
                            label = "SIM 1",
                            tint = nova.sim1,
                            onClick = { onCallRoute(CallRoute.SIM_1) }
                        )
                        ExpandedActionPill(
                            icon = Icons.Filled.SimCard,
                            label = "SIM 2",
                            tint = nova.sim2,
                            onClick = { onCallRoute(CallRoute.SIM_2) }
                        )
                        ExpandedActionPill(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = "WA",
                            tint = nova.whatsApp,
                            onClick = onWhatsApp
                        )
                        if (isUnknownCaller) {
                            ExpandedActionPill(
                                icon = Icons.Filled.VerifiedUser,
                                label = if (isBn) "নাম দিন" else "ID Tag",
                                tint = Color(0xFF0087FF),
                                onClick = onTagCaller
                            )
                        } else {
                            ExpandedActionPill(
                                icon = Icons.Filled.StickyNote2,
                                label = strings.callerNotePrefix,
                                tint = nova.gold,
                                onClick = onAddNote
                            )
                        }
                        ExpandedActionPill(
                            icon = Icons.Filled.Info,
                            label = if (isBn) "বিস্তারিত" else "Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onOpenDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
