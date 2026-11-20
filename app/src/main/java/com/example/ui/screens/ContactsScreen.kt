package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppLanguage
import com.example.data.model.CallRoute
import com.example.data.model.DeviceContact
import com.example.ui.components.ContactAvatar
import com.example.ui.components.PermissionAccessCard
import com.example.ui.components.RouteBadge
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState
import kotlinx.coroutines.launch

private sealed interface ContactListEntry {
    data class SectionHeader(val letter: String) : ContactListEntry
    data class ContactItem(val contact: DeviceContact) : ContactListEntry
}

@Composable
fun ContactsScreen(
    state: DialerUiState,
    strings: NovaStrings,
    onSearchChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onAddNewContact: () -> Unit,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onOpenCallerDetail: (String, String?) -> Unit,
    onEditSpeedDialSlot: (Int, SpeedDialEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val isBn = state.preferences.language == AppLanguage.BENGALI
    val speedDialMap = remember(state.speedDials) { state.speedDials.associateBy { it.digit } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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

    // Group contacts alphabetically (A..Z, #) for section headers & right-side A-Z Fast-Scroll Index
    val groupedData = remember(state.filteredContacts) {
        val entries = ArrayList<ContactListEntry>(state.filteredContacts.size + 28)
        val letterToIndex = LinkedHashMap<String, Int>()
        var lastLetter: String? = null
        val sorted = state.filteredContacts.sortedBy { it.name.trim().uppercase() }
        for (contact in sorted) {
            val firstChar = contact.name.trim().firstOrNull()?.uppercaseChar()
            val letter = if (firstChar != null && firstChar in 'A'..'Z') firstChar.toString() else "#"
            if (letter != lastLetter) {
                letterToIndex[letter] = entries.size
                entries.add(ContactListEntry.SectionHeader(letter))
                lastLetter = letter
            }
            entries.add(ContactListEntry.ContactItem(contact))
        }
        entries to letterToIndex
    }
    val contactEntries = groupedData.first
    val letterIndices = groupedData.second
    val indexLetters = remember(letterIndices) { letterIndices.keys.toList() }

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
            // Top Bar matching Screenshot 7: Settings (left) • Contacts (center) • + Add Contact (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSettings)
                        .testTag("contacts_settings_button"),
                    shape = CircleShape,
                    color = nova.glassSurface,
                    border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.75f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = if (isBn) "সেটিংস" else "Settings",
                            tint = nova.gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.tabContacts,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isBn) "${state.contacts.size} টি কন্টাক্ট" else "${state.contacts.size} Contacts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAddNewContact)
                        .testTag("add_new_contact_button"),
                    shape = CircleShape,
                    color = nova.goldContainer,
                    border = BorderStroke(1.dp, nova.gold.copy(alpha = 0.55f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = stringResource(R.string.cd_add_contact),
                            tint = nova.gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rounded Pill Search Bar with Voice Mic (matching Screenshot 7)
            OutlinedTextField(
                value = state.contactSearchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_search_input"),
                placeholder = {
                    Text(
                        text = strings.searchContactsHint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = nova.gold
                    )
                },
                trailingIcon = {
                    if (state.contactSearchQuery.isNotEmpty()) {
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
                    focusedBorderColor = nova.gold,
                    unfocusedBorderColor = nova.glassBorder.copy(alpha = 0.65f)
                )
            )

            if (state.contactSearchQuery.isBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                // Compact VIP Speed Dial Strip (1..9)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 6.dp)
                ) {
                    items((1..9).toList()) { digit ->
                        val entry = speedDialMap[digit]
                        SpeedDialSlotCard(
                            digit = digit,
                            entry = entry,
                            strings = strings,
                            onTap = {
                                if (entry != null) {
                                    val route = runCatching { CallRoute.valueOf(entry.preferredRoute) }
                                        .getOrDefault(CallRoute.SIM_1)
                                    onCallRoute(entry.phoneNumber, route, entry.name)
                                } else {
                                    onEditSpeedDialSlot(digit, null)
                                }
                            },
                            onLongPress = { onEditSpeedDialSlot(digit, entry) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (contactEntries.isEmpty()) {
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
                                imageVector = Icons.Filled.Contacts,
                                contentDescription = null,
                                tint = nova.gold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.noContactsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.noContactsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Alphabetical Contacts List + Right-Side A–Z Fast-Scroll Alphabet Index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (indexLetters.size > 3) 22.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(
                            items = contactEntries,
                            key = { entry ->
                                when (entry) {
                                    is ContactListEntry.SectionHeader -> "hdr_${entry.letter}"
                                    is ContactListEntry.ContactItem -> entry.contact.id
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is ContactListEntry.SectionHeader -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                                    ) {
                                        Text(
                                            text = entry.letter,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = nova.sim2
                                        )
                                        HorizontalDivider(
                                            color = nova.glassBorder.copy(alpha = 0.45f),
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                }
                                is ContactListEntry.ContactItem -> {
                                    val contact = entry.contact
                                    ContactRowCard(
                                        contact = contact,
                                        onOpenDetail = { onOpenCallerDetail(contact.phoneNumber, contact.name) },
                                        onCallSim1 = { onCallRoute(contact.phoneNumber, CallRoute.SIM_1, contact.name) },
                                        onCallSim2 = { onCallRoute(contact.phoneNumber, CallRoute.SIM_2, contact.name) }
                                    )
                                }
                            }
                        }
                    }

                    // A–Z Fast-Scroll Alphabet Rail (matching Screenshot 7)
                    if (indexLetters.size > 3) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(bottom = 86.dp, top = 8.dp)
                                .width(20.dp)
                                .pointerInput(indexLetters) {
                                    detectVerticalDragGestures { change, _ ->
                                        val frac = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 0.999f)
                                        val idx = (frac * indexLetters.size).toInt().coerceIn(0, indexLetters.lastIndex)
                                        val target = letterIndices[indexLetters[idx]]
                                        if (target != null) {
                                            coroutineScope.launch { listState.scrollToItem(target) }
                                        }
                                    }
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            indexLetters.forEach { letter ->
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 13.sp
                                    ),
                                    color = nova.sim2,
                                    modifier = Modifier
                                        .clickable {
                                            letterIndices[letter]?.let { idx ->
                                                coroutineScope.launch { listState.scrollToItem(idx) }
                                            }
                                        }
                                        .padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialSlotCard(
    digit: Int,
    entry: SpeedDialEntity?,
    strings: NovaStrings,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier
            .width(96.dp)
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .testTag("speed_dial_slot_$digit"),
        shape = RoundedCornerShape(16.dp),
        color = nova.glassSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (entry != null) nova.gold.copy(alpha = 0.5f) else nova.glassBorder.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (entry != null) nova.goldContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$digit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (entry != null) nova.gold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry != null) {
                    val route = runCatching { CallRoute.valueOf(entry.preferredRoute) }.getOrDefault(CallRoute.SIM_1)
                    RouteBadge(
                        route = route,
                        sim1Label = "S1",
                        sim2Label = "S2"
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = strings.assignSpeedDial,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (entry != null) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = strings.assignSpeedDial,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContactRowCard(
    contact: DeviceContact,
    onOpenDetail: () -> Unit,
    onCallSim1: () -> Unit,
    onCallSim2: () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("contact_card_${contact.id}"),
        shape = RoundedCornerShape(18.dp),
        color = nova.glassSurface.copy(alpha = if (nova.liquidGlassEnabled) 0.90f else 1f),
        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.60f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.isStarred) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = nova.gold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Instant SIM 1 call button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(nova.sim1Container)
                    .clickable(onClick = onCallSim1),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Call SIM 1",
                    tint = nova.sim1,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Instant SIM 2 call button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(nova.sim2Container)
                    .clickable(onClick = onCallSim2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SimCard,
                    contentDescription = "Call SIM 2",
                    tint = nova.sim2,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
