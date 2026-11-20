package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.InCallActivity
import com.example.data.cloud.CallerCategory
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppLanguage
import com.example.data.model.normalizePhoneNumber
import com.example.data.telephony.CallStateManager
import com.example.data.telephony.NewContactPayload
import com.example.ui.components.BlockNumberDialog
import com.example.ui.components.CallNoteDialog
import com.example.ui.components.CallerDetailBottomSheet
import com.example.ui.components.ChooseLineBottomSheet
import com.example.ui.components.SaveContactBottomSheet
import com.example.ui.components.SpeedDialAssignDialog
import com.example.ui.components.TagCallerDialog
import com.example.ui.i18n.novaStrings
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.KeypadScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaCallTheme
import com.example.ui.viewmodel.DialerUiEvent
import com.example.ui.viewmodel.DialerViewModel
import com.example.ui.viewmodel.NovaTab

private data class NavDestinationSpec(
    val tab: NovaTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private data class NoteDialogRequest(
    val number: String,
    val name: String,
    val note: String,
    val hasReminder: Boolean
)

private data class BlockDialogRequest(
    val number: String,
    val label: String
)

private data class SpeedDialDialogRequest(
    val digit: Int,
    val existing: SpeedDialEntity?,
    val initialNumber: String,
    val initialName: String
)

private data class TagCallerDialogRequest(
    val number: String,
    val name: String,
    val category: CallerCategory = CallerCategory.PERSONAL
)

private data class SaveContactDialogRequest(
    val number: String,
    val name: String
)

@Composable
fun NovaCallApp(
    viewModel: DialerViewModel,
    signedInEmail: String?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeCall by CallStateManager.activeCall.collectAsStateWithLifecycle()
    val strings = remember(state.preferences.language) { novaStrings(state.preferences.language) }
    val snackbarHostState = remember { SnackbarHostState() }

    var noteDialogRequest by remember { mutableStateOf<NoteDialogRequest?>(null) }
    var blockDialogRequest by remember { mutableStateOf<BlockDialogRequest?>(null) }
    var speedDialDialogRequest by remember { mutableStateOf<SpeedDialDialogRequest?>(null) }
    var tagCallerRequest by remember { mutableStateOf<TagCallerDialogRequest?>(null) }
    var saveContactRequest by remember { mutableStateOf<SaveContactDialogRequest?>(null) }
    var pendingContactPayload by remember { mutableStateOf<NewContactPayload?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSystemData(force = true)
    }

    val writeContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingContactPayload
        pendingContactPayload = null
        if (pending != null) {
            if (granted) {
                viewModel.saveContactWithPhoto(
                    payload = pending,
                    onSaved = { saveContactRequest = null },
                    onNeedPermission = {}
                )
            } else {
                saveContactRequest = null
                viewModel.openAddContact(pending.phoneNumber, pending.fullName)
            }
        }
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemData(force = true)
    }

    val requestAllPermissions: () -> Unit = {
        val perms = buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.WRITE_CONTACTS)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.WRITE_CALL_LOG)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    val requestDefaultDialer: () -> Unit = {
        viewModel.createRequestDefaultDialerIntent()?.let { intent ->
            runCatching { defaultDialerLauncher.launch(intent) }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemData(force = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DialerUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    BackHandler(enabled = state.isSettingsOpen || state.activeTab != NovaTab.RECENTS) {
        if (state.isSettingsOpen) {
            viewModel.closeSettings()
        } else {
            viewModel.selectTab(NovaTab.RECENTS)
        }
    }

    val isBn = state.preferences.language == AppLanguage.BENGALI
    val navItems = remember(strings, isBn) {
        listOf(
            NavDestinationSpec(
                tab = NovaTab.FAVORITES,
                label = if (isBn) "প্রিয়জন" else "Favorites",
                selectedIcon = Icons.Filled.Star,
                unselectedIcon = Icons.Outlined.StarOutline,
                testTag = "nav_favorites"
            ),
            NavDestinationSpec(
                tab = NovaTab.RECENTS,
                label = strings.tabRecents,
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                testTag = "nav_recents"
            ),
            NavDestinationSpec(
                tab = NovaTab.CONTACTS,
                label = strings.tabContacts,
                selectedIcon = Icons.Filled.Contacts,
                unselectedIcon = Icons.Outlined.Contacts,
                testTag = "nav_contacts"
            ),
            NavDestinationSpec(
                tab = NovaTab.KEYPAD,
                label = strings.tabKeypad,
                selectedIcon = Icons.Filled.Dialpad,
                unselectedIcon = Icons.Outlined.Dialpad,
                testTag = "nav_keypad"
            )
        )
    }

    NovaCallTheme(
        darkTheme = state.preferences.darkThemeEnabled,
        themePalette = state.preferences.themePalette,
        displayThemeMode = state.preferences.displayThemeMode,
        fontScale = state.preferences.fontScale,
        liquidGlassEnabled = state.preferences.liquidGlassEnabled
    ) {
        val nova = LocalNovaColors.current

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp
            val showFloatingPill = !useRail && !state.isSettingsOpen

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.statusBars,
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(bottom = if (showFloatingPill) 82.dp else 12.dp)
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .let { mod -> if (state.isSettingsOpen) mod.navigationBarsPadding() else mod }
                    ) {
                        // Tap-to-return Active Call banner when user is on a live call inside Shajamal
                        activeCall?.let { live ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(nova.sim1)
                                    .clickable {
                                        context.startActivity(
                                            Intent(context, InCallActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            }
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isBn) "কল চলছে: ${live.displayName} • ফিরে যেতে ট্যাপ করুন"
                                    else "Ongoing Call: ${live.displayName} • Tap to return",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }

                        Row(modifier = Modifier.weight(1f)) {
                            if (useRail && !state.isSettingsOpen) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    navItems.forEach { item ->
                                        val selected = state.activeTab == item.tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { viewModel.selectTab(item.tab) },
                                            modifier = Modifier.testTag(item.testTag),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = item.label,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                if (state.isSettingsOpen) {
                                    SettingsScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = viewModel::closeSettings,
                                        onRequestDefaultDialer = requestDefaultDialer,
                                        onOpenAppInfoSettings = viewModel::openAppInfoSettings,
                                        onOpenSystemDefaultApps = viewModel::openSystemDefaultAppsSettings,
                                        onRequestPermissions = requestAllPermissions,
                                        onOpenSystemNotificationSettings = viewModel::openAppNotificationSettings,
                                        onToggleNotifyIncoming = viewModel::updateNotifyIncoming,
                                        onToggleNotifyMissed = viewModel::updateNotifyMissed,
                                        onToggleNotifyOngoing = viewModel::updateNotifyOngoing,
                                        onToggleVibrateOnRing = viewModel::updateVibrateOnRing,
                                        onToggleAutoRejectBlocked = viewModel::updateAutoRejectBlocked,
                                        onOpenAddBlockDialog = { blockDialogRequest = BlockDialogRequest("", "") },
                                        onUnblockNumber = viewModel::unblockNumber,
                                        onToggleDtmfSound = viewModel::updateDtmfSound,
                                        onSelectSoundStyle = viewModel::updateKeypadSoundStyle,
                                        onChangeSoundVolume = viewModel::updateKeypadSoundVolume,
                                        onPreviewSound = { viewModel.previewKeypadSound() },
                                        onToggleHaptic = viewModel::updateHaptic,
                                        onToggleAutoSpeaker = viewModel::updateAutoSpeaker,
                                        onUpdateDefaultRoute = viewModel::updateDefaultRoute,
                                        onUpdateSimAlias = viewModel::updateSimAlias,
                                        onAddQuickReply = viewModel::addQuickReply,
                                        onRemoveQuickReply = viewModel::removeQuickReply,
                                        onToggleDarkTheme = viewModel::updateDarkTheme,
                                        onSelectThemePalette = viewModel::updateThemePalette,
                                        onToggleLiquidGlass = viewModel::updateLiquidGlass,
                                        onSelectDisplayThemeMode = viewModel::updateDisplayThemeMode,
                                        onSelectFontScale = viewModel::updateFontScale,
                                        onSelectIncomingCallStyle = viewModel::updateIncomingCallDisplayStyle,
                                        onSelectSlideToAnswerMode = viewModel::updateSlideToAnswerMode,
                                        onToggleLanguage = viewModel::updateLanguage,
                                        signedInEmail = signedInEmail,
                                        onToggleCloudCallerId = viewModel::toggleCloudCallerIdEnabled,
                                        onSyncCommunityContacts = viewModel::syncContactsToCommunityCallerId,
                                        onOpenTagCallerDialog = {
                                            tagCallerRequest = TagCallerDialogRequest(
                                                number = state.dialedDigits,
                                                name = ""
                                            )
                                        },
                                        onSignOut = onSignOut
                                    )
                                } else {
                                    when (state.activeTab) {
                                        NovaTab.FAVORITES -> FavoritesScreen(
                                            state = state,
                                            strings = strings,
                                            onCallRoute = viewModel::placeCall,
                                            onOpenCallerDetail = viewModel::openCallerDetail,
                                            onLongPressChooseLine = viewModel::openRoutePicker,
                                            onAddFavorite = viewModel::addFavorite,
                                            onRemoveFavorite = viewModel::removeFavorite
                                        )

                                        NovaTab.RECENTS -> RecentsScreen(
                                            state = state,
                                            strings = strings,
                                            onSelectFilter = viewModel::updateRecentsFilter,
                                            onSearchChange = viewModel::updateRecentsSearch,
                                            onRefresh = { viewModel.refreshSystemData(force = true) },
                                            onOpenSettings = viewModel::openSettings,
                                            onRequestPermissions = requestAllPermissions,
                                            onCallRoute = viewModel::placeCall,
                                            onOpenCallerDetail = viewModel::openCallerDetail,
                                            onOpenTagCaller = { num, existingName ->
                                                tagCallerRequest = TagCallerDialogRequest(
                                                    number = num,
                                                    name = existingName.orEmpty()
                                                )
                                            },
                                            onWhatsApp = viewModel::openWhatsApp,
                                            onOpenNoteDialog = { num, name ->
                                                val existing = state.callNotes.firstOrNull { it.normalizedNumber == normalizePhoneNumber(num) }
                                                noteDialogRequest = NoteDialogRequest(
                                                    number = num,
                                                    name = existing?.contactName ?: name.orEmpty(),
                                                    note = existing?.note.orEmpty(),
                                                    hasReminder = existing?.hasReminder ?: false
                                                )
                                            },
                                            onDeleteCallLog = viewModel::deleteCallLogItem,
                                            onLongPressCallOptions = { item ->
                                                viewModel.openRoutePicker(item.number, item.contactName)
                                            }
                                        )

                                        NovaTab.CONTACTS -> ContactsScreen(
                                            state = state,
                                            strings = strings,
                                            onSearchChange = viewModel::updateContactSearch,
                                            onOpenSettings = viewModel::openSettings,
                                            onRequestPermissions = requestAllPermissions,
                                            onAddNewContact = {
                                                saveContactRequest = SaveContactDialogRequest("", "")
                                            },
                                            onCallRoute = viewModel::placeCall,
                                            onOpenCallerDetail = viewModel::openCallerDetail,
                                            onEditSpeedDialSlot = { digit, existing ->
                                                speedDialDialogRequest = SpeedDialDialogRequest(
                                                    digit = digit,
                                                    existing = existing,
                                                    initialNumber = existing?.phoneNumber.orEmpty(),
                                                    initialName = existing?.name.orEmpty()
                                                )
                                            }
                                        )

                                        NovaTab.KEYPAD -> KeypadScreen(
                                            state = state,
                                            strings = strings,
                                            onDigitPress = viewModel::onDialDigitPress,
                                            onDigitLongPress = viewModel::onDialDigitLongPress,
                                            onBackspace = viewModel::onDialBackspace,
                                            onClearAll = viewModel::onDialClear,
                                            onCallRoute = viewModel::placeCall,
                                            onWhatsApp = viewModel::openWhatsApp,
                                            onAddContact = { num ->
                                                saveContactRequest = SaveContactDialogRequest(
                                                    number = num,
                                                    name = state.cloudCallerId.dialedLookup?.displayName
                                                        ?: state.t9Matches.firstOrNull()?.name.orEmpty()
                                                )
                                            },
                                            onCopyNumber = viewModel::copyNumber,
                                            onOpenNoteDialog = { num, name ->
                                                val existing = state.callNotes.firstOrNull { it.normalizedNumber == normalizePhoneNumber(num) }
                                                noteDialogRequest = NoteDialogRequest(
                                                    number = num,
                                                    name = existing?.contactName ?: name.orEmpty(),
                                                    note = existing?.note.orEmpty(),
                                                    hasReminder = existing?.hasReminder ?: false
                                                )
                                            },
                                            onOpenSettings = viewModel::openSettings,
                                            onRequestDefaultDialer = requestDefaultDialer,
                                            onOpenTagCaller = { num, existingName ->
                                                tagCallerRequest = TagCallerDialogRequest(
                                                    number = num,
                                                    name = existingName.orEmpty(),
                                                    category = state.cloudCallerId.dialedLookup?.category ?: CallerCategory.PERSONAL
                                                )
                                            },
                                            onSelectT9Contact = { contact ->
                                                viewModel.setDialedNumber(contact.phoneNumber)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Liquid Glass Pill Bottom Navigation Bar (Favorites | Recents | Contacts | Keypad)
                if (showFloatingPill) {
                    FloatingLiquidGlassNavBar(
                        items = navItems,
                        activeTab = state.activeTab,
                        liquidGlassEnabled = state.preferences.liquidGlassEnabled,
                        onSelectTab = viewModel::selectTab,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .widthIn(max = 460.dp)
                    )
                }
            }

            // Bottom Sheet: Caller Details
            state.selectedCallerDetail?.let { target ->
                CallerDetailBottomSheet(
                    target = target,
                    state = state,
                    strings = strings,
                    onDismiss = viewModel::closeCallerDetail,
                    onCallRoute = { route -> viewModel.placeCall(target.number, route, target.name) },
                    onWhatsApp = { viewModel.openWhatsApp(target.number) },
                    onQuickSms = { template -> viewModel.openSms(target.number, template) },
                    onCopy = { viewModel.copyNumber(target.number) },
                    onSaveContact = {
                        saveContactRequest = SaveContactDialogRequest(
                            number = target.number,
                            name = target.name.orEmpty()
                        )
                    },
                    onOpenNoteDialog = {
                        val existing = state.callNotes.firstOrNull { it.normalizedNumber == target.normalizedNumber }
                        noteDialogRequest = NoteDialogRequest(
                            number = target.number,
                            name = existing?.contactName ?: target.name.orEmpty(),
                            note = existing?.note.orEmpty(),
                            hasReminder = existing?.hasReminder ?: false
                        )
                    },
                    onOpenSpeedDialDialog = {
                        val usedDigits = state.speedDials.map { it.digit }.toSet()
                        val nextFree = (2..9).firstOrNull { it !in usedDigits } ?: 2
                        speedDialDialogRequest = SpeedDialDialogRequest(
                            digit = nextFree,
                            existing = state.speedDials.firstOrNull { it.digit == nextFree },
                            initialNumber = target.number,
                            initialName = target.name.orEmpty()
                        )
                    },
                    onToggleBlock = { currentlyBlocked ->
                        if (currentlyBlocked) {
                            viewModel.unblockNumber(target.normalizedNumber)
                        } else {
                            blockDialogRequest = BlockDialogRequest(
                                number = target.number,
                                label = target.name.orEmpty()
                            )
                        }
                    }
                )
            }

            // Bottom Sheet: Choose Calling Line
            state.routePickerTarget?.let { target ->
                ChooseLineBottomSheet(
                    target = target,
                    state = state,
                    strings = strings,
                    onDismiss = viewModel::closeRoutePicker,
                    onSelectRoute = { route -> viewModel.placeCall(target.number, route, target.name) }
                )
            }

            // Dialog: Add / Edit Call Note
            noteDialogRequest?.let { req ->
                CallNoteDialog(
                    initialNumber = req.number,
                    initialName = req.name,
                    initialNote = req.note,
                    initialReminder = req.hasReminder,
                    strings = strings,
                    onDismiss = { noteDialogRequest = null },
                    onSave = viewModel::saveCallNote
                )
            }

            // Dialog: Block Number
            blockDialogRequest?.let { req ->
                BlockNumberDialog(
                    initialNumber = req.number,
                    initialLabel = req.label,
                    strings = strings,
                    onDismiss = { blockDialogRequest = null },
                    onConfirm = viewModel::blockNumber
                )
            }

            // Dialog: Assign Speed Dial
            speedDialDialogRequest?.let { req ->
                SpeedDialAssignDialog(
                    initialDigit = req.digit,
                    existing = req.existing,
                    initialNumber = req.initialNumber,
                    initialName = req.initialName,
                    strings = strings,
                    onDismiss = { speedDialDialogRequest = null },
                    onSave = viewModel::saveSpeedDial,
                    onRemove = viewModel::removeSpeedDial
                )
            }

            // Dialog: Tag Caller ID (Community Directory)
            tagCallerRequest?.let { req ->
                TagCallerDialog(
                    initialNumber = req.number,
                    initialName = req.name,
                    initialCategory = req.category,
                    language = state.preferences.language,
                    onDismiss = { tagCallerRequest = null },
                    onSubmit = viewModel::publishCallerTag
                )
            }

            // Bottom Sheet: Save Contact with Photo & Full Details
            saveContactRequest?.let { req ->
                SaveContactBottomSheet(
                    initialNumber = req.number,
                    initialName = req.name,
                    language = state.preferences.language,
                    strings = strings,
                    onDismiss = { saveContactRequest = null },
                    onSave = { payload ->
                        viewModel.saveContactWithPhoto(
                            payload = payload,
                            onSaved = { saveContactRequest = null },
                            onNeedPermission = {
                                pendingContactPayload = payload
                                writeContactsLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                            }
                        )
                    },
                    onOpenSystemContactsFallback = { num, name ->
                        viewModel.openAddContact(num, name)
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatingLiquidGlassNavBar(
    items: List<NavDestinationSpec>,
    activeTab: NovaTab,
    liquidGlassEnabled: Boolean,
    onSelectTab: (NovaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val pillColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (liquidGlassEnabled) {
        nova.glassBorder.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        color = pillColor,
        shadowElevation = if (liquidGlassEnabled) 4.dp else 2.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = activeTab == item.tab
                val activeTint = if (item.tab == NovaTab.FAVORITES) Color(0xFF0A84FF) else MaterialTheme.colorScheme.primary
                val activePillBg = activeTint.copy(alpha = 0.16f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (selected) activePillBg else Color.Transparent)
                        .clickable { onSelectTab(item.tab) }
                        .testTag(item.testTag)
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (selected) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
