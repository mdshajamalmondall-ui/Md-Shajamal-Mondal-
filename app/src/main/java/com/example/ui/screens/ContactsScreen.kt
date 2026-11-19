package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SpeedDialEntity
import com.example.data.model.CallRoute
import com.example.data.model.DeviceContact
import com.example.ui.components.ContactAvatar
import com.example.ui.components.PermissionAccessCard
import com.example.ui.components.RouteBadge
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState

@Composable
fun ContactsScreen(
    state: DialerUiState,
    strings: NovaStrings,
    onSearchChange: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onAddNewContact: () -> Unit,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onOpenCallerDetail: (String, String?) -> Unit,
    onEditSpeedDialSlot: (Int, SpeedDialEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val speedDialMap = state.speedDials.associateBy { it.digit }

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
                        text = strings.tabContacts,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.speedDialTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(44.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

            // VIP Speed Dial Slots (1..9)
            Text(
                text = strings.speedDial,
                style = MaterialTheme.typography.labelLarge,
                color = nova.gold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items((1..9).toList()) { digit ->
                    val entry = speedDialMap[digit]
                    SpeedDialSlotCard(
                        digit = digit,
                        entry = entry,
                        sim1Label = state.sim1.displayName,
                        sim2Label = state.sim2.displayName,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
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
                trailingIcon = if (state.contactSearchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.clearAll)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = nova.glassSurface,
                    unfocusedContainerColor = nova.glassSurface,
                    focusedBorderColor = nova.gold,
                    unfocusedBorderColor = nova.glassBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!state.permissions.hasReadContacts) {
                PermissionAccessCard(
                    strings = strings,
                    onRequestPermissions = onRequestPermissions,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (state.filteredContacts.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(state.filteredContacts, key = { it.id }) { contact ->
                        ContactRowCard(
                            contact = contact,
                            onOpenDetail = { onOpenCallerDetail(contact.phoneNumber, contact.name) },
                            onCallSim1 = { onCallRoute(contact.phoneNumber, CallRoute.SIM_1, contact.name) },
                            onCallBrilliant = { onCallRoute(contact.phoneNumber, CallRoute.BRILLIANT, contact.name) }
                        )
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
    sim1Label: String,
    sim2Label: String,
    strings: NovaStrings,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier
            .width(106.dp)
            .height(94.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .testTag("speed_dial_slot_$digit"),
        shape = RoundedCornerShape(18.dp),
        color = nova.glassSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (entry != null) nova.gold.copy(alpha = 0.5f) else nova.glassBorder.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
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
                        sim2Label = "S2",
                        brilliantLabel = "Br"
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = strings.assignSpeedDial,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (entry != null) {
                Column {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = strings.assignSpeedDial,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
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
    onCallBrilliant: () -> Unit
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("contact_card_${contact.id}"),
        shape = RoundedCornerShape(18.dp),
        color = nova.glassSurface,
        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleSmall,
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
            // Instant Brilliant Connect call button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(nova.brilliantContainer)
                    .clickable(onClick = onCallBrilliant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = "Brilliant Call",
                    tint = nova.brilliant,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Instant SIM 1 call button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(nova.sim1Container)
                    .clickable(onClick = onCallSim1),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Call SIM 1",
                    tint = nova.sim1,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
