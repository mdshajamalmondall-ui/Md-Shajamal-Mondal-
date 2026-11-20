package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    onRefresh: () -> Unit,
    onRequestPermissions: () -> Unit,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onOpenCallerDetail: (String, String?) -> Unit,
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

    val blockedSet = remember(state.blockedNumbers) {
        state.blockedNumbers.map { it.normalizedNumber }.toSet()
    }
    val notesMap = remember(state.callNotes) {
        state.callNotes.associateBy { it.normalizedNumber }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.tabRecents,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${strings.todayCalls}: ${state.todayCallCount}  •  ${strings.totalBlocked}: ${state.blockedNumbers.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_recents_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = nova.gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills
            val filters = listOf(
                CallFilterType.ALL to strings.filterAll,
                CallFilterType.MISSED to strings.filterMissed,
                CallFilterType.BRILLIANT to strings.filterBrilliant,
                CallFilterType.OUTGOING to strings.filterOutgoing,
                CallFilterType.INCOMING to strings.filterIncoming
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(filters) { (filter, label) ->
                    val selected = state.recentsFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(filter) },
                        modifier = Modifier.testTag("recents_filter_${filter.name.lowercase()}"),
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = if (filter == CallFilterType.BRILLIANT) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (filter == CallFilterType.BRILLIANT) nova.brilliantContainer else MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = if (filter == CallFilterType.BRILLIANT) nova.brilliant else MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = nova.brilliant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isBn) "ডানে টানলে কল • বামে টানলে ডিলিট • চেপে ধরলে কল অপশন"
                else "Swipe right to call • Swipe left to delete • Long press for options",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (!state.permissions.hasReadCallLog) {
                PermissionAccessCard(
                    strings = strings,
                    onRequestPermissions = onRequestPermissions,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(state.filteredCallLogs, key = { it.id }) { item ->
                        val isBlocked = blockedSet.contains(item.normalizedNumber)
                        val note = notesMap[item.normalizedNumber]
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { targetValue ->
                                when (targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCallRoute(item.number, item.route, item.contactName)
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
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val bgColor = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> nova.sim1
                                    SwipeToDismissBoxValue.EndToStart -> nova.missed
                                    else -> Color.Transparent
                                }
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
                                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
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
                        ) {
                            CallLogCard(
                                item = item,
                                expanded = expandedId == item.id,
                                isBlocked = isBlocked,
                                noteText = note?.note,
                                sim1Label = state.sim1.displayName,
                                sim2Label = state.sim2.displayName,
                                strings = strings,
                                language = state.preferences.language,
                                onToggleExpand = {
                                    expandedId = if (expandedId == item.id) null else item.id
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongPressCallOptions(item)
                                },
                                onCallRoute = { route ->
                                    onCallRoute(item.number, route, item.contactName)
                                },
                                onWhatsApp = { onWhatsApp(item.number) },
                                onAddNote = { onOpenNoteDialog(item.number, item.contactName) },
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
    isBlocked: Boolean,
    noteText: String?,
    sim1Label: String,
    sim2Label: String,
    strings: NovaStrings,
    language: AppLanguage,
    onToggleExpand: () -> Unit,
    onLongPress: () -> Unit,
    onCallRoute: (CallRoute) -> Unit,
    onWhatsApp: () -> Unit,
    onAddNote: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val nova = LocalNovaColors.current
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
        color = nova.glassSurface,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isBlocked -> nova.missed.copy(alpha = 0.55f)
                expanded -> nova.gold.copy(alpha = 0.5f)
                else -> nova.glassBorder.copy(alpha = 0.7f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (item.direction == CallDirection.MISSED) nova.missed else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isBlocked) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = "Blocked",
                                tint = nova.missed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = dirIcon,
                            contentDescription = dirText,
                            tint = dirColor,
                            modifier = Modifier.size(14.dp)
                        )
                        val dur = formatCallDuration(item.durationSeconds, language)
                        val timeStr = formatRelativeTime(item.timestamp, language)
                        Text(
                            text = listOf(timeStr, dur).filter { it.isNotEmpty() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RouteBadge(
                            route = item.route,
                            sim1Label = sim1Label,
                            sim2Label = sim2Label,
                            brilliantLabel = strings.brilliantShort
                        )
                    }
                    if (!noteText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${strings.callerNotePrefix}: $noteText",
                            style = MaterialTheme.typography.labelSmall,
                            color = nova.gold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Quick Direct Call button using the same route used in the log!
                val quickBg = when (item.route) {
                    CallRoute.SIM_1 -> nova.sim1Container
                    CallRoute.SIM_2 -> nova.sim2Container
                    CallRoute.BRILLIANT -> nova.brilliantContainer
                }
                val quickTint = when (item.route) {
                    CallRoute.SIM_1 -> nova.sim1
                    CallRoute.SIM_2 -> nova.sim2
                    CallRoute.BRILLIANT -> nova.brilliant
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(quickBg)
                        .clickable { onCallRoute(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.route == CallRoute.BRILLIANT) Icons.Filled.Bolt else Icons.Filled.Call,
                        contentDescription = strings.call,
                        tint = quickTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = nova.glassBorder.copy(alpha = 0.55f))
                    Spacer(modifier = Modifier.height(10.dp))
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
                            icon = Icons.Filled.Bolt,
                            label = strings.brilliantShort,
                            tint = nova.brilliant,
                            onClick = { onCallRoute(CallRoute.BRILLIANT) }
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
                        ExpandedActionPill(
                            icon = Icons.Filled.StickyNote2,
                            label = strings.callerNotePrefix,
                            tint = nova.gold,
                            onClick = onAddNote
                        )
                        ExpandedActionPill(
                            icon = Icons.Filled.Info,
                            label = "More",
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
