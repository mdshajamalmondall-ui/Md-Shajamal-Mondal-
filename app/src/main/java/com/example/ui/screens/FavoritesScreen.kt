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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.CallRoute
import com.example.data.model.DeviceContact
import com.example.data.model.FavoriteGroup
import com.example.data.model.formatDisplayNumber
import com.example.ui.components.ContactAvatar
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    state: DialerUiState,
    strings: NovaStrings,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onOpenContactProfile: (String, String?) -> Unit,
    onLongPressChooseLine: (String, String?) -> Unit,
    onAddFavorite: (DeviceContact) -> Unit,
    onRemoveFavorite: (DeviceContact) -> Unit,
    onSelectGroup: (FavoriteGroup) -> Unit,
    onAssignGroup: (String, FavoriteGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val haptic = LocalHapticFeedback.current
    val isBn = state.preferences.language == AppLanguage.BENGALI

    var editMode by remember { mutableStateOf(false) }
    var showAddPicker by remember { mutableStateOf(false) }

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
            // Top Bar: Edit Pill (left) • Favorites Title (center) • + Add Circle (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { editMode = !editMode }
                        .testTag("favorites_edit_button"),
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }

                Text(
                    text = if (isBn) "প্রিয়জন" else "Favorites",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { showAddPicker = true }
                        .testTag("favorites_add_button"),
                    shape = CircleShape,
                    color = nova.glassSurface,
                    border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.75f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = if (isBn) "প্রিয়জন যোগ করুন" else "Add Favorite",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Favorite Groups Filter Strip: All | Family | Friends | Office
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FavoriteGroup.entries, key = { it.name }) { grp ->
                    val active = state.selectedFavoriteGroup == grp
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectGroup(grp) }
                            .testTag("fav_group_${grp.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) nova.goldContainer else nova.glassSurface,
                        border = BorderStroke(
                            1.dp,
                            if (active) nova.gold else nova.glassBorder.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = if (isBn) grp.labelBn else grp.labelEn,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (active) nova.gold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val displayedFavorites = remember(
                state.favoriteContacts,
                state.selectedFavoriteGroup,
                state.preferences.favoriteGroups
            ) {
                if (state.selectedFavoriteGroup == FavoriteGroup.ALL) {
                    state.favoriteContacts
                } else {
                    state.favoriteContacts.filter { c ->
                        val tail = c.normalizedNumber.takeLast(10)
                        val assigned = state.preferences.favoriteGroups.entries.firstOrNull { (k, _) ->
                            k == c.normalizedNumber || (tail.length >= 8 && k.endsWith(tail))
                        }?.value
                        assigned == state.selectedFavoriteGroup
                    }
                }
            }

            if (displayedFavorites.isEmpty()) {
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
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(nova.goldContainer.copy(alpha = 0.75f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = nova.gold,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (state.selectedFavoriteGroup == FavoriteGroup.ALL) {
                                if (isBn) "আপনার প্রিয় মানুষেরা সবসময় সামনে" else "Your people, always first."
                            } else {
                                val grpName = if (isBn) state.selectedFavoriteGroup.labelBn else state.selectedFavoriteGroup.labelEn
                                if (isBn) "$grpName গ্রুপে এখনো কেউ নেই" else "No favorites in $grpName yet"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBn) "উপরের (+) বাটনে ট্যাপ করে প্রিয় কন্টাক্ট যোগ করুন—অথবা প্রোফাইল থেকে গ্রুপ সেট করুন।"
                            else "Tap (+) above to pin favorite contacts or assign groups from Contact Profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(displayedFavorites, key = { it.id }) { contact ->
                        val tail = contact.normalizedNumber.takeLast(10)
                        val assignedGroup = remember(contact.normalizedNumber, state.preferences.favoriteGroups) {
                            state.preferences.favoriteGroups.entries.firstOrNull { (k, _) ->
                                k == contact.normalizedNumber || (tail.length >= 8 && k.endsWith(tail))
                            }?.value
                        }
                        var groupMenuOpen by remember { mutableStateOf(false) }

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (!editMode) {
                                                onCallRoute(contact.phoneNumber, CallRoute.SIM_1, contact.name)
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLongPressChooseLine(contact.phoneNumber, contact.name)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 10.dp)
                                    .testTag("favorite_row_${contact.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedVisibility(visible = editMode) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onRemoveFavorite(contact) },
                                            modifier = Modifier.size(36.dp)
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
                                }

                                ContactAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = nova.gold,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        if (assignedGroup != null && assignedGroup != FavoriteGroup.ALL) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = nova.goldContainer
                                            ) {
                                                Text(
                                                    text = if (isBn) assignedGroup.labelBn else assignedGroup.labelEn,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = nova.gold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${if (isBn) "মোবাইল" else "Mobile"} • ${formatDisplayNumber(contact.phoneNumber)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box {
                                    IconButton(
                                        onClick = { groupMenuOpen = true },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Group,
                                            contentDescription = if (isBn) "গ্রুপ বাছুন" else "Assign Group",
                                            tint = if (assignedGroup != null) nova.gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = groupMenuOpen,
                                        onDismissRequest = { groupMenuOpen = false }
                                    ) {
                                        listOf(
                                            FavoriteGroup.FAMILY,
                                            FavoriteGroup.FRIENDS,
                                            FavoriteGroup.OFFICE,
                                            FavoriteGroup.ALL
                                        ).forEach { grp ->
                                            val label = if (grp == FavoriteGroup.ALL) {
                                                if (isBn) "গ্রুপ সরান" else "No Group"
                                            } else {
                                                if (isBn) grp.labelBn else grp.labelEn
                                            }
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    groupMenuOpen = false
                                                    onAssignGroup(contact.phoneNumber, grp)
                                                }
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { onOpenContactProfile(contact.phoneNumber, contact.name) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = if (isBn) "কন্টাক্ট প্রোফাইল" else "Contact Profile",
                                        tint = nova.sim2,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = nova.glassBorder.copy(alpha = 0.35f),
                                modifier = Modifier.padding(start = 62.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var pickerQuery by remember { mutableStateOf("") }
        val existingSet = remember(state.favoriteContacts) {
            state.favoriteContacts.map { it.normalizedNumber }.toSet()
        }
        val candidates = remember(state.contacts, pickerQuery, existingSet) {
            val q = pickerQuery.trim()
            state.contacts.filter { c ->
                !existingSet.contains(c.normalizedNumber) && (
                    q.isEmpty() ||
                        c.name.contains(q, ignoreCase = true) ||
                        c.phoneNumber.contains(q, ignoreCase = true)
                    )
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 26.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "প্রিয় কন্টাক্ট বাছাই করুন" else "Add to Favorites",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showAddPicker = false }) {
                        Icon(Icons.Filled.Close, contentDescription = strings.cancel)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pickerQuery,
                    onValueChange = { pickerQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.searchContactsHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = nova.gold) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = nova.glassSurface,
                        unfocusedContainerColor = nova.glassSurface,
                        focusedBorderColor = nova.gold,
                        unfocusedBorderColor = nova.glassBorder
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (candidates.isEmpty()) {
                    Text(
                        text = if (isBn) "যোগ করার মতো নতুন কন্টাক্ট পাওয়া যায়নি।" else "No more contacts available to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates.take(60), key = { it.id }) { contact ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onAddFavorite(contact)
                                        showAddPicker = false
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = nova.glassSurface,
                                border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 40.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(nova.goldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Add Favorite",
                                            tint = nova.gold,
                                            modifier = Modifier.size(18.dp)
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
}
