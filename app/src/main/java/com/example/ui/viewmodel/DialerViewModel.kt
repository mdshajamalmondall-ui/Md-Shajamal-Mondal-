package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.DialerRepository
import com.example.data.cloud.CallerCategory
import com.example.data.cloud.CallerDirectoryEntry
import com.example.data.cloud.CallerIdIntelligence
import com.example.data.cloud.CallerIdLookupResult
import com.example.data.cloud.CallerIdRepository
import com.example.data.cloud.UserCloudProfile
import com.example.data.local.AppCallRecordEntity
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallNoteEntity
import com.example.data.local.NovaCallDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemePalette
import com.example.data.model.BrilliantBridgeStatus
import com.example.data.model.BrilliantLaunchMode
import com.example.data.model.CallDirection
import com.example.data.model.CallDispatchResult
import com.example.data.model.CallFilterType
import com.example.data.model.CallLogItem
import com.example.data.model.CallRoute
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.DeviceContact
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.SimCardInfo
import com.example.data.model.UserPreferences
import com.example.data.model.normalizePhoneNumber
import com.example.data.telephony.CallNotificationHelper
import com.example.data.telephony.TelephonyBridgeManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NovaTab {
    KEYPAD,
    RECENTS,
    CONTACTS,
    BRIDGE
}

data class PermissionSnapshot(
    val hasCallPhone: Boolean = false,
    val hasReadPhoneState: Boolean = false,
    val hasReadContacts: Boolean = false,
    val hasReadCallLog: Boolean = false,
    val hasPostNotifications: Boolean = true
) {
    val allGranted: Boolean
        get() = hasCallPhone && hasReadPhoneState && hasReadContacts && hasReadCallLog
}

data class CallerDetailTarget(
    val number: String,
    val normalizedNumber: String,
    val name: String?
)

data class CloudCallerIdSnapshot(
    val profile: UserCloudProfile? = null,
    val myTags: List<CallerDirectoryEntry> = emptyList(),
    val dialedLookup: CallerIdLookupResult? = null,
    val isLookingUp: Boolean = false,
    val isSyncingContacts: Boolean = false,
    val errorMessage: String? = null
)

data class DialerUiState(
    val activeTab: NovaTab = NovaTab.RECENTS,
    val isSettingsOpen: Boolean = false,
    val isDefaultDialer: Boolean = false,
    val dialedDigits: String = "",
    val contactSearchQuery: String = "",
    val recentsFilter: CallFilterType = CallFilterType.ALL,
    val preferences: UserPreferences = UserPreferences(),
    val permissions: PermissionSnapshot = PermissionSnapshot(),
    val simCards: List<SimCardInfo> = emptyList(),
    val brilliantStatus: BrilliantBridgeStatus = BrilliantBridgeStatus(),
    val contacts: List<DeviceContact> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val speedDials: List<SpeedDialEntity> = emptyList(),
    val blockedNumbers: List<BlockedNumberEntity> = emptyList(),
    val callNotes: List<CallNoteEntity> = emptyList(),
    val t9Matches: List<DeviceContact> = emptyList(),
    val filteredContacts: List<DeviceContact> = emptyList(),
    val filteredCallLogs: List<CallLogItem> = emptyList(),
    val activeDialedNote: CallNoteEntity? = null,
    val isDialedBlocked: Boolean = false,
    val suggestBrilliantForDialed: Boolean = false,
    val todayCallCount: Int = 0,
    val selectedCallerDetail: CallerDetailTarget? = null,
    val routePickerTarget: CallerDetailTarget? = null,
    val isLoadingSystemData: Boolean = false,
    val cloudCallerId: CloudCallerIdSnapshot = CloudCallerIdSnapshot()
) {
    val sim1: SimCardInfo
        get() = simCards.firstOrNull { it.slotIndex == 0 }
            ?: SimCardInfo(0, -1, preferences.sim1Alias.ifBlank { "SIM 1" }, "SIM 1", "", false)

    val sim2: SimCardInfo
        get() = simCards.firstOrNull { it.slotIndex == 1 }
            ?: SimCardInfo(1, -1, preferences.sim2Alias.ifBlank { "SIM 2" }, "SIM 2", "", false)
}

sealed interface DialerUiEvent {
    data class ShowMessage(val text: String) : DialerUiEvent
    data class PromptInstallBrilliant(val packageName: String) : DialerUiEvent
}

private data class LocalRoomSnapshot(
    val speedDials: List<SpeedDialEntity>,
    val blockedNumbers: List<BlockedNumberEntity>,
    val callNotes: List<CallNoteEntity>,
    val appCallRecords: List<AppCallRecordEntity>,
    val preferences: UserPreferences
)

private data class SystemDataSnapshot(
    val permissions: PermissionSnapshot,
    val isDefaultDialer: Boolean,
    val simCards: List<SimCardInfo>,
    val brilliantStatus: BrilliantBridgeStatus,
    val contacts: List<DeviceContact>,
    val systemCallLogs: List<CallLogItem>,
    val isLoading: Boolean
)

private data class UiInputSnapshot(
    val activeTab: NovaTab,
    val isSettingsOpen: Boolean,
    val dialedDigits: String,
    val contactSearchQuery: String,
    val recentsFilter: CallFilterType,
    val selectedCallerDetail: CallerDetailTarget?,
    val routePickerTarget: CallerDetailTarget?
)

class DialerViewModel(
    private val repository: DialerRepository,
    private val telephony: TelephonyBridgeManager,
    private val callerIdRepository: CallerIdRepository?,
    private val currentUserId: String,
    private val userDisplayNameHint: String
) : ViewModel() {

    private val activeTab = MutableStateFlow(NovaTab.RECENTS)
    private val isSettingsOpen = MutableStateFlow(false)
    private val dialedDigits = MutableStateFlow("")
    private val contactSearchQuery = MutableStateFlow("")
    private val recentsFilter = MutableStateFlow(CallFilterType.ALL)
    private val selectedCallerDetail = MutableStateFlow<CallerDetailTarget?>(null)
    private val routePickerTarget = MutableStateFlow<CallerDetailTarget?>(null)

    private val dialedLookupResult = MutableStateFlow<CallerIdLookupResult?>(null)
    private val isLookingUpCallerId = MutableStateFlow(false)
    private val isSyncingCommunityContacts = MutableStateFlow(false)
    private val cloudErrorMessage = MutableStateFlow<String?>(null)
    private val hiddenCallLogIds = MutableStateFlow<Set<String>>(emptySet())
    private var lookupJob: Job? = null
    private var refreshJob: Job? = null
    private var lastSystemRefreshMs: Long = 0L
    @Volatile
    private var cachedT9Index: List<Pair<DeviceContact, String>> = emptyList()
    @Volatile
    private var cachedT9ContactsRef: List<DeviceContact>? = null

    private val systemSnapshot = MutableStateFlow(
        SystemDataSnapshot(
            permissions = currentPermissions(),
            isDefaultDialer = telephony.isDefaultDialer(),
            simCards = emptyList(),
            brilliantStatus = BrilliantBridgeStatus(),
            contacts = emptyList(),
            systemCallLogs = emptyList(),
            isLoading = false
        )
    )

    private val _events = MutableSharedFlow<DialerUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DialerUiEvent> = _events.asSharedFlow()

    private val cloudProfileFlow: StateFlow<UserCloudProfile?> = (
        if (callerIdRepository != null && currentUserId.isNotBlank()) {
            callerIdRepository.observeUserProfile(currentUserId)
        } else {
            flowOf(null)
        }
        )
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user cloud profile", firestoreError ?: error)
            cloudErrorMessage.value = (firestoreError ?: error).localizedMessage
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val myTagsFlow: StateFlow<List<CallerDirectoryEntry>> = (
        if (callerIdRepository != null && currentUserId.isNotBlank()) {
            callerIdRepository.observeMyContributedTags(currentUserId)
        } else {
            flowOf(emptyList())
        }
        )
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing contributed caller tags", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val cloudSnapshotFlow = combine(
        cloudProfileFlow,
        myTagsFlow,
        dialedLookupResult,
        combine(isLookingUpCallerId, isSyncingCommunityContacts, cloudErrorMessage) { l, s, e -> Triple(l, s, e) }
    ) { profile, tags, lookup, flags ->
        CloudCallerIdSnapshot(
            profile = profile,
            myTags = tags,
            dialedLookup = lookup,
            isLookingUp = flags.first,
            isSyncingContacts = flags.second,
            errorMessage = flags.third
        )
    }

    private val roomSnapshotFlow = combine(
        repository.speedDials,
        repository.blockedNumbers,
        repository.callNotes,
        repository.appCallRecords,
        repository.preferences
    ) { speedDials, blocked, notes, records, prefs ->
        LocalRoomSnapshot(speedDials, blocked, notes, records, prefs)
    }

    private val uiInputFlow = combine(
        combine(activeTab, isSettingsOpen) { t, s -> t to s },
        dialedDigits,
        contactSearchQuery,
        recentsFilter,
        combine(selectedCallerDetail, routePickerTarget) { d, r -> d to r }
    ) { nav, digits, query, filter, overlays ->
        UiInputSnapshot(nav.first, nav.second, digits, query, filter, overlays.first, overlays.second)
    }

    val uiState: StateFlow<DialerUiState> = combine(
        roomSnapshotFlow,
        combine(systemSnapshot, hiddenCallLogIds) { sys, hidden -> sys to hidden },
        uiInputFlow,
        cloudSnapshotFlow
    ) { room, sysPair, input, cloud ->
        val sys = sysPair.first
        val hiddenIds = sysPair.second
        val mergedLogs = mergeCallLogs(
            systemLogs = sys.systemCallLogs,
            localRecords = room.appCallRecords,
            contacts = sys.contacts,
            callNotes = room.callNotes,
            hiddenIds = hiddenIds
        )
        val normalizedDial = normalizePhoneNumber(input.dialedDigits)
        val t9Matches = computeT9Matches(input.dialedDigits, sys.contacts)
        val filteredContacts = filterContacts(input.contactSearchQuery, sys.contacts)
        val filteredLogs = filterCallLogs(input.recentsFilter, mergedLogs)
        val matchedNote = if (normalizedDial.length >= 4) {
            room.callNotes.firstOrNull {
                it.normalizedNumber == normalizedDial ||
                    it.normalizedNumber.endsWith(normalizedDial.takeLast(10))
            }
        } else null
        val isBlocked = normalizedDial.length >= 4 && room.blockedNumbers.any {
            it.normalizedNumber == normalizedDial ||
                (normalizedDial.length >= 8 && it.normalizedNumber.endsWith(normalizedDial.takeLast(10)))
        }
        val prefixes = room.preferences.brilliantSmartPrefixes
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val suggestBrilliant = input.dialedDigits.isNotEmpty() &&
            prefixes.any { prefix -> input.dialedDigits.startsWith(prefix) }

        val startOfDay = System.currentTimeMillis() - (24L * 60L * 60L * 1000L)
        val todayCount = mergedLogs.count { it.timestamp >= startOfDay }

        DialerUiState(
            activeTab = input.activeTab,
            isSettingsOpen = input.isSettingsOpen,
            isDefaultDialer = sys.isDefaultDialer,
            dialedDigits = input.dialedDigits,
            contactSearchQuery = input.contactSearchQuery,
            recentsFilter = input.recentsFilter,
            preferences = room.preferences,
            permissions = sys.permissions,
            simCards = sys.simCards,
            brilliantStatus = sys.brilliantStatus,
            contacts = sys.contacts,
            callLogs = mergedLogs,
            speedDials = room.speedDials,
            blockedNumbers = room.blockedNumbers,
            callNotes = room.callNotes,
            t9Matches = t9Matches,
            filteredContacts = filteredContacts,
            filteredCallLogs = filteredLogs,
            activeDialedNote = matchedNote,
            isDialedBlocked = isBlocked,
            suggestBrilliantForDialed = suggestBrilliant,
            todayCallCount = todayCount,
            selectedCallerDetail = input.selectedCallerDetail,
            routePickerTarget = input.routePickerTarget,
            isLoadingSystemData = sys.isLoading,
            cloudCallerId = cloud
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DialerUiState()
        )

    init {
        refreshSystemData(force = true)
        if (callerIdRepository != null && currentUserId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                callerIdRepository.ensureUserProfile(userDisplayNameHint)
            }
        }
        viewModelScope.launch {
            repository.preferences
                .map { Triple(it.sim1Alias, it.sim2Alias, it.brilliantPackageName) }
                .distinctUntilChanged()
                .collect {
                    val prefs = uiState.value.preferences
                    refreshBridgeAndSims(prefs)
                }
        }
    }

    override fun onCleared() {
        lookupJob?.cancel()
        refreshJob?.cancel()
        telephony.releaseToneGenerator()
        super.onCleared()
    }

    private fun currentPermissions(): PermissionSnapshot = PermissionSnapshot(
        hasCallPhone = telephony.hasPermission(Manifest.permission.CALL_PHONE),
        hasReadPhoneState = telephony.hasPermission(Manifest.permission.READ_PHONE_STATE),
        hasReadContacts = telephony.hasPermission(Manifest.permission.READ_CONTACTS),
        hasReadCallLog = telephony.hasPermission(Manifest.permission.READ_CALL_LOG),
        hasPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            telephony.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else true
    )

    fun refreshSystemData(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (refreshJob?.isActive == true || now - lastSystemRefreshMs < 1_500L)) {
            // Still quickly update lightweight flags (permissions / default dialer) without re-querying DB
            val perms = currentPermissions()
            val isDef = telephony.isDefaultDialer()
            val cur = systemSnapshot.value
            if (cur.permissions != perms || cur.isDefaultDialer != isDef) {
                systemSnapshot.value = cur.copy(permissions = perms, isDefaultDialer = isDef)
            }
            return
        }
        lastSystemRefreshMs = now
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            val perms = currentPermissions()
            val isDef = telephony.isDefaultDialer()
            systemSnapshot.value = systemSnapshot.value.copy(
                permissions = perms,
                isDefaultDialer = isDef,
                isLoading = systemSnapshot.value.systemCallLogs.isEmpty()
            )
            val prefs = uiState.value.preferences
            coroutineScope {
                val logsDeferred = async { telephony.loadSystemCallLogs() }
                val simsDeferred = async { telephony.loadSimCards(prefs) }
                val brilliantDeferred = async { telephony.inspectBrilliantBridge(prefs.brilliantPackageName) }
                val contactsDeferred = async { telephony.loadDeviceContacts() }

                // Emit call logs immediately so the Recents screen opens instantaneously!
                val logs = logsDeferred.await()
                systemSnapshot.value = systemSnapshot.value.copy(
                    systemCallLogs = logs,
                    isLoading = false
                )

                val sims = simsDeferred.await()
                val brilliant = brilliantDeferred.await()
                val contacts = contactsDeferred.await()
                systemSnapshot.value = SystemDataSnapshot(
                    permissions = perms,
                    isDefaultDialer = isDef,
                    simCards = sims,
                    brilliantStatus = brilliant,
                    contacts = contacts,
                    systemCallLogs = logs,
                    isLoading = false
                )
            }
        }
    }

    private fun refreshBridgeAndSims(prefs: UserPreferences) {
        viewModelScope.launch(Dispatchers.IO) {
            coroutineScope {
                val simsDeferred = async { telephony.loadSimCards(prefs) }
                val brilliantDeferred = async { telephony.inspectBrilliantBridge(prefs.brilliantPackageName) }
                systemSnapshot.value = systemSnapshot.value.copy(
                    permissions = currentPermissions(),
                    isDefaultDialer = telephony.isDefaultDialer(),
                    simCards = simsDeferred.await(),
                    brilliantStatus = brilliantDeferred.await()
                )
            }
        }
    }

    private fun triggerLiveCallerIdLookup(rawInput: String) {
        lookupJob?.cancel()
        val digitsOnly = rawInput.filter { it.isDigit() }
        if (digitsOnly.length < 3) {
            dialedLookupResult.value = null
            isLookingUpCallerId.value = false
            return
        }

        // If already saved in device contacts, no need for unknown caller cloud lookup
        val localMatch = resolveContactName(rawInput, systemSnapshot.value.contacts)
        if (!localMatch.isNullOrBlank()) {
            dialedLookupResult.value = null
            isLookingUpCallerId.value = false
            return
        }

        // Show instant offline carrier / verified BD service info + local saved tag immediately
        val offlineBase = CallerIdIntelligence.detectOffline(rawInput)
        val normInput = normalizePhoneNumber(rawInput)
        val tailInput = normInput.takeLast(10)
        val localTaggedNote = uiState.value.callNotes.firstOrNull { note ->
            note.contactName.isNotBlank() && (
                note.normalizedNumber == normInput ||
                    (tailInput.length >= 8 && note.normalizedNumber.endsWith(tailInput))
                )
        }
        val myCloudTag = myTagsFlow.value.firstOrNull { tag ->
            tag.displayName.isNotBlank() && (
                tag.phoneKey == normInput ||
                    (tailInput.length >= 8 && tag.phoneKey.endsWith(tailInput))
                )
        }
        val instantLocalName = myCloudTag?.displayName?.takeIf { it.isNotBlank() }
            ?: localTaggedNote?.contactName?.takeIf { it.isNotBlank() }

        val resolvedTagCategory = myCloudTag?.category
            ?.let { raw -> runCatching { CallerCategory.valueOf(raw.uppercase()) }.getOrNull() }
            ?: CallerCategory.PERSONAL

        dialedLookupResult.value = if (!instantLocalName.isNullOrBlank()) {
            (offlineBase ?: CallerIdLookupResult(
                phoneKey = normInput,
                displayName = instantLocalName,
                carrierOrRegion = ""
            )).copy(
                displayName = instantLocalName,
                category = resolvedTagCategory,
                source = com.example.data.cloud.CallerIdSource.COMMUNITY_CLOUD
            )
        } else {
            offlineBase
        }

        val callerIdEnabled = cloudProfileFlow.value?.callerIdEnabled ?: true
        val repo = callerIdRepository
        if (!callerIdEnabled || digitsOnly.length < 4 || repo == null) {
            isLookingUpCallerId.value = false
            return
        }

        lookupJob = viewModelScope.launch {
            delay(320L)
            isLookingUpCallerId.value = true
            val result = repo.lookupNumber(rawInput)
            if (dialedDigits.value.filter { it.isDigit() } == digitsOnly) {
                if (!result?.displayName.isNullOrBlank()) {
                    dialedLookupResult.value = result
                } else if (instantLocalName.isNullOrBlank()) {
                    dialedLookupResult.value = result
                }
            }
            isLookingUpCallerId.value = false
        }
    }

    fun publishCallerTag(number: String, name: String, category: CallerCategory) {
        viewModelScope.launch {
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            val repo = callerIdRepository
            if (repo == null) {
                val cleanName = name.trim()
                val cleanNum = number.trim()
                if (cleanName.isBlank() || cleanNum.isBlank()) return@launch
                repository.saveCallNote(
                    number = cleanNum,
                    contactName = cleanName,
                    note = cleanName,
                    hasReminder = false
                )
                if (category == CallerCategory.SPAM) {
                    repository.blockNumber(cleanNum, cleanName, "Spam")
                }
                val offline = CallerIdIntelligence.detectOffline(cleanNum)
                dialedLookupResult.value = (offline ?: CallerIdLookupResult(
                    phoneKey = cleanNum.filter { it.isDigit() },
                    displayName = cleanName,
                    carrierOrRegion = ""
                )).copy(
                    displayName = cleanName,
                    category = category,
                    spamReports = if (category == CallerCategory.SPAM) 1L else 0L
                )
                emitMessage(
                    if (isBn) "লোকাল কলার আইডি সেভ হয়েছে: $cleanName"
                    else "Saved local Caller ID: $cleanName"
                )
                return@launch
            }
            val res = repo.publishCallerTag(number, name, category)
            res.onSuccess { published ->
                if (dialedDigits.value.isNotBlank()) {
                    dialedLookupResult.value = published
                }
                emitMessage(
                    if (isBn) "কলার আইডি সেভ হয়েছে: ${published.displayName}"
                    else "Caller ID published: ${published.displayName}"
                )
            }.onFailure { err ->
                emitMessage(
                    if (isBn) "কলার আইডি সেভ করা যায়নি: ${err.localizedMessage.orEmpty()}"
                    else "Failed to save Caller ID: ${err.localizedMessage.orEmpty()}"
                )
            }
        }
    }

    fun toggleCloudCallerIdEnabled(enabled: Boolean) {
        viewModelScope.launch {
            callerIdRepository?.updateCloudCallerIdSettings(callerIdEnabled = enabled)
            if (!enabled) {
                dialedLookupResult.value = null
            } else if (dialedDigits.value.length >= 4) {
                triggerLiveCallerIdLookup(dialedDigits.value)
            }
        }
    }

    fun syncContactsToCommunityCallerId() {
        viewModelScope.launch {
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            val repo = callerIdRepository
            if (repo == null) {
                emitMessage(
                    if (isBn) "এই বিল্ডে অফলাইন কলার আইডি চালু আছে (ক্লাউড সিঙ্ক অনুপলব্ধ)"
                    else "Offline Caller ID mode active (cloud sync unavailable in this build)"
                )
                return@launch
            }
            val contacts = systemSnapshot.value.contacts
            if (contacts.isEmpty()) {
                emitMessage(
                    if (isBn) "কন্টাক্ট পারমিশন দিন অথবা আগে ফোনে কন্টাক্ট সেভ করুন"
                    else "Grant Contacts permission first to sync names"
                )
                return@launch
            }
            isSyncingCommunityContacts.value = true
            val res = repo.syncOptInContacts(contacts)
            isSyncingCommunityContacts.value = false
            res.onSuccess { added ->
                emitMessage(
                    if (isBn) "কমিউনিটি কলার আইডিতে $added টি নতুন নম্বর যুক্ত হয়েছে!"
                    else "Synced $added new contacts to Community Caller ID!"
                )
            }.onFailure { err ->
                emitMessage(
                    if (isBn) "সিঙ্ক ব্যর্থ হয়েছে: ${err.localizedMessage.orEmpty()}"
                    else "Sync failed: ${err.localizedMessage.orEmpty()}"
                )
            }
        }
    }

    fun createRequestDefaultDialerIntent(): Intent? = telephony.createRequestDefaultDialerIntent()

    fun openAppNotificationSettings() = telephony.openAppNotificationSettings()

    fun openAppInfoSettings() = telephony.openAppInfoSettings()

    fun openSystemDefaultAppsSettings() = telephony.openSystemDefaultAppsSettings()

    fun openCallingAccountsSettings() = telephony.openCallingAccountsSettings()

    fun openSettings() {
        isSettingsOpen.value = true
    }

    fun closeSettings() {
        isSettingsOpen.value = false
    }

    fun selectTab(tab: NovaTab) {
        isSettingsOpen.value = false
        activeTab.value = tab
    }

    fun prefillNumberFromIntent(rawNumber: String) {
        val clean = normalizePhoneNumber(rawNumber)
        if (clean.isNotEmpty()) {
            dialedDigits.value = clean
            isSettingsOpen.value = false
            activeTab.value = NovaTab.KEYPAD
            triggerLiveCallerIdLookup(clean)
        }
    }

    fun onDialDigitPress(symbol: String) {
        if (dialedDigits.value.length < 24) {
            dialedDigits.value += symbol
            triggerLiveCallerIdLookup(dialedDigits.value)
        }
        val prefs = uiState.value.preferences
        if (prefs.dtmfSoundEnabled) {
            telephony.playDialpadSound(symbol, prefs.keypadSoundStyle, prefs.keypadSoundVolume)
        }
    }

    fun previewKeypadSound(style: KeypadSoundStyle = uiState.value.preferences.keypadSoundStyle) {
        telephony.playDialpadSound("5", style, uiState.value.preferences.keypadSoundVolume)
    }

    fun onDialBackspace() {
        if (dialedDigits.value.isNotEmpty()) {
            dialedDigits.value = dialedDigits.value.dropLast(1)
            triggerLiveCallerIdLookup(dialedDigits.value)
        }
    }

    fun onDialClear() {
        dialedDigits.value = ""
        dialedLookupResult.value = null
        isLookingUpCallerId.value = false
        lookupJob?.cancel()
    }

    fun setDialedNumber(number: String) {
        dialedDigits.value = number.trim()
        isSettingsOpen.value = false
        activeTab.value = NovaTab.KEYPAD
        triggerLiveCallerIdLookup(dialedDigits.value)
    }

    fun onDialDigitLongPress(symbol: String) {
        if (symbol == "0") {
            onDialDigitPress("+")
            return
        }
        val digit = symbol.toIntOrNull() ?: return
        val speedDial = uiState.value.speedDials.firstOrNull { it.digit == digit }
        if (speedDial != null) {
            val route = runCatching { CallRoute.valueOf(speedDial.preferredRoute) }
                .getOrDefault(CallRoute.SIM_1)
            placeCall(speedDial.phoneNumber, route, speedDial.name)
        } else {
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(
                if (isBn) "কী $digit এ কোনো স্পিড ডায়াল সেট করা নেই (কন্টাক্ট ট্যাব থেকে সেট করুন)"
                else "No Speed Dial set for key $digit (configure in Contacts tab)"
            )
        }
    }

    fun updateContactSearch(query: String) {
        contactSearchQuery.value = query
    }

    fun updateRecentsFilter(filter: CallFilterType) {
        recentsFilter.value = filter
    }

    fun openCallerDetail(number: String, name: String?) {
        selectedCallerDetail.value = CallerDetailTarget(
            number = number,
            normalizedNumber = normalizePhoneNumber(number),
            name = name
        )
    }

    fun closeCallerDetail() {
        selectedCallerDetail.value = null
    }

    fun openRoutePicker(number: String, name: String?) {
        routePickerTarget.value = CallerDetailTarget(
            number = number,
            normalizedNumber = normalizePhoneNumber(number),
            name = name
        )
    }

    fun closeRoutePicker() {
        routePickerTarget.value = null
    }

    fun placeCall(rawNumber: String, route: CallRoute, contactName: String? = null) {
        val state = uiState.value
        val numberToCall = rawNumber.trim().ifEmpty {
            val last = state.callLogs.firstOrNull()?.number.orEmpty()
            if (last.isNotEmpty()) {
                dialedDigits.value = last
                triggerLiveCallerIdLookup(last)
            }
            return
        }

        val resolvedName = contactName
            ?: resolveContactName(numberToCall, state.contacts)
            ?: state.cloudCallerId.dialedLookup?.displayName

        val result = when (route) {
            CallRoute.SIM_1 -> telephony.placeSimCall(numberToCall, state.sim1)
            CallRoute.SIM_2 -> telephony.placeSimCall(numberToCall, state.sim2)
            CallRoute.BRILLIANT -> telephony.placeBrilliantCall(
                rawNumber = numberToCall,
                bridgeStatus = state.brilliantStatus,
                prefs = state.preferences,
                fallbackSim = state.sim1
            )
        }

        val isBn = state.preferences.language == AppLanguage.BENGALI
        when (result) {
            is CallDispatchResult.Dispatched -> {
                viewModelScope.launch {
                    repository.recordInAppCall(result.formattedNumber, resolvedName, route)
                }
                emitMessage(if (isBn) result.messageBn else result.messageEn)
            }
            is CallDispatchResult.BrilliantNotInstalled -> {
                _events.tryEmit(DialerUiEvent.PromptInstallBrilliant(result.packageName))
            }
            is CallDispatchResult.InvalidNumber -> {
                emitMessage(if (isBn) result.reasonBn else result.reasonEn)
            }
            is CallDispatchResult.Error -> {
                emitMessage(if (isBn) result.messageBn else result.messageEn)
            }
        }
    }

    fun testBrilliantBridge() {
        val state = uiState.value
        val isBn = state.preferences.language == AppLanguage.BENGALI
        if (state.brilliantStatus.isInstalled) {
            val opened = telephony.openBrilliantApp(state.brilliantStatus.packageName)
            emitMessage(
                if (opened) {
                    if (isBn) "ব্রিলিয়ান্ট কানেক্ট সফলভাবে সংযুক্ত ও চালু হয়েছে!"
                    else "Brilliant Connect is connected and launched!"
                } else {
                    if (isBn) "ব্রিলিয়ান্ট অ্যাপ চালু করা যায়নি"
                    else "Unable to launch Brilliant Connect"
                }
            )
        } else {
            _events.tryEmit(DialerUiEvent.PromptInstallBrilliant(state.brilliantStatus.packageName))
        }
    }

    fun openPlayStoreForBrilliant(pkg: String = uiState.value.brilliantStatus.packageName) {
        telephony.openPlayStoreForBrilliant(pkg)
    }

    fun openWhatsApp(number: String) {
        val ok = telephony.openWhatsAppChat(number)
        if (!ok) {
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(if (isBn) "সঠিক ফোন নম্বর দিন" else "Enter a valid phone number")
        }
    }

    fun openSms(number: String, body: String = "") {
        telephony.openSmsComposer(number, body)
    }

    fun openAddContact(number: String, name: String = "") {
        telephony.openAddContact(number, name)
    }

    fun copyNumber(number: String) {
        telephony.copyToClipboard("Phone Number", number)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(if (isBn) "নম্বর কপি হয়েছে: $number" else "Copied: $number")
    }

    fun saveSpeedDial(digit: Int, name: String, number: String, route: CallRoute) {
        viewModelScope.launch {
            repository.saveSpeedDial(digit, name, number, route)
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(if (isBn) "স্পিড ডায়াল #$digit সেভ হয়েছে" else "Speed Dial #$digit saved")
        }
    }

    fun removeSpeedDial(digit: Int) {
        viewModelScope.launch { repository.removeSpeedDial(digit) }
    }

    fun blockNumber(number: String, label: String, reason: String) {
        viewModelScope.launch {
            repository.blockNumber(number, label, reason)
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(if (isBn) "$number ব্লক লিস্টে যোগ হয়েছে" else "Blocked $number")
        }
    }

    fun unblockNumber(normalizedNumber: String) {
        viewModelScope.launch {
            repository.unblockNumber(normalizedNumber)
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(if (isBn) "নম্বর আনব্লক করা হয়েছে" else "Number unblocked")
        }
    }

    fun saveCallNote(number: String, contactName: String, note: String, hasReminder: Boolean) {
        viewModelScope.launch {
            repository.saveCallNote(number, contactName, note, hasReminder)
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(if (isBn) "কল নোট সেভ হয়েছে" else "Call note saved")
        }
    }

    fun deleteCallNote(normalizedNumber: String) {
        viewModelScope.launch { repository.deleteCallNote(normalizedNumber) }
    }

    fun deleteCallLogItem(item: CallLogItem) {
        hiddenCallLogIds.value = hiddenCallLogIds.value + item.id
        if (item.id.startsWith("sys_")) {
            systemSnapshot.value = systemSnapshot.value.copy(
                systemCallLogs = systemSnapshot.value.systemCallLogs.filterNot { it.id == item.id }
            )
        }
        viewModelScope.launch {
            if (item.id.startsWith("local_")) {
                item.id.removePrefix("local_").toLongOrNull()?.let { localId ->
                    repository.deleteLocalCallRecord(localId)
                }
            } else if (item.id.startsWith("sys_")) {
                telephony.deleteSystemCallLog(item.id.removePrefix("sys_"))
            }
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(if (isBn) "কল হিস্ট্রি থেকে ডিলিট হয়েছে" else "Deleted from call history")
        }
    }

    fun updateBrilliantPackage(pkg: String) = viewModelScope.launch {
        repository.updateBrilliantPackage(pkg.ifBlank { BrilliantBridgeStatus.DEFAULT_BRILLIANT_PACKAGE })
    }

    fun updateBrilliantLaunchMode(mode: BrilliantLaunchMode) = viewModelScope.launch {
        repository.updateBrilliantLaunchMode(mode)
    }

    fun updateBrilliantAutoFormatBd(enabled: Boolean) = viewModelScope.launch {
        repository.updateBrilliantAutoFormatBd(enabled)
    }

    fun updateBrilliantAutoCopy(enabled: Boolean) = viewModelScope.launch {
        repository.updateBrilliantAutoCopy(enabled)
    }

    fun updateBrilliantSmartPrefixes(prefixes: String) = viewModelScope.launch {
        repository.updateBrilliantSmartPrefixes(prefixes)
    }

    fun updateBrilliantExternalHandoff(enabled: Boolean) = viewModelScope.launch {
        repository.updateBrilliantExternalHandoff(enabled)
    }

    fun updateBrilliantAccount(name: String, number: String) = viewModelScope.launch {
        repository.updateBrilliantAccount(name, number)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(
            if (isBn) "ব্রিলিয়ান্ট অ্যাকাউন্ট সেভ হয়েছে"
            else "Brilliant account saved"
        )
    }

    fun updateBrilliantLinkedAccountId(accountId: String) = viewModelScope.launch {
        repository.updateBrilliantLinkedAccountId(accountId)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(
            if (isBn) "ব্রিলিয়ান্ট কলিং অ্যাকাউন্ট লিংক হয়েছে"
            else "Brilliant calling account linked"
        )
    }

    fun updateDefaultRoute(route: DefaultRoutePreference) = viewModelScope.launch {
        repository.updateDefaultRoute(route)
    }

    fun updateSimAlias(slotIndex: Int, alias: String) = viewModelScope.launch {
        repository.updateSimAlias(slotIndex, alias)
    }

    fun updateDtmfSound(enabled: Boolean) = viewModelScope.launch {
        repository.updateDtmfSound(enabled)
        if (enabled) previewKeypadSound()
    }

    fun updateKeypadSoundStyle(style: KeypadSoundStyle) = viewModelScope.launch {
        repository.updateKeypadSoundStyle(style)
        previewKeypadSound(style)
    }

    fun updateKeypadSoundVolume(volume: Float) = viewModelScope.launch {
        repository.updateKeypadSoundVolume(volume)
    }

    fun updateHaptic(enabled: Boolean) = viewModelScope.launch {
        repository.updateHaptic(enabled)
    }

    fun updateDarkTheme(enabled: Boolean) = viewModelScope.launch {
        repository.updateDarkTheme(enabled)
    }

    fun updateThemePalette(palette: AppThemePalette) = viewModelScope.launch {
        repository.updateThemePalette(palette)
    }

    fun updateLanguage(language: AppLanguage) = viewModelScope.launch {
        repository.updateLanguage(language)
    }

    fun updateNotifyIncoming(enabled: Boolean) = viewModelScope.launch {
        repository.updateNotifyIncoming(enabled)
    }

    fun updateNotifyMissed(enabled: Boolean) = viewModelScope.launch {
        repository.updateNotifyMissed(enabled)
    }

    fun updateNotifyOngoing(enabled: Boolean) = viewModelScope.launch {
        repository.updateNotifyOngoing(enabled)
    }

    fun updateVibrateOnRing(enabled: Boolean) = viewModelScope.launch {
        repository.updateVibrateOnRing(enabled)
    }

    fun updateAutoRejectBlocked(enabled: Boolean) = viewModelScope.launch {
        repository.updateAutoRejectBlocked(enabled)
    }

    fun updateAutoSpeaker(enabled: Boolean) = viewModelScope.launch {
        repository.updateAutoSpeaker(enabled)
    }

    fun addQuickReply(template: String) = viewModelScope.launch {
        val trimmed = template.trim()
        if (trimmed.isEmpty()) return@launch
        val updated = (uiState.value.preferences.quickReplies + trimmed).distinct()
        repository.updateQuickReplies(updated)
    }

    fun removeQuickReply(template: String) = viewModelScope.launch {
        val updated = uiState.value.preferences.quickReplies.filterNot { it == template }
        repository.updateQuickReplies(updated.ifEmpty { UserPreferences.DEFAULT_QUICK_REPLIES })
    }

    private fun emitMessage(text: String) {
        _events.tryEmit(DialerUiEvent.ShowMessage(text))
    }

    private fun resolveContact(number: String, contacts: List<DeviceContact>): DeviceContact? {
        val norm = normalizePhoneNumber(number)
        if (norm.length < 4) return null
        val tail = norm.takeLast(10)
        return contacts.firstOrNull {
            it.normalizedNumber == norm ||
                (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
        }
    }

    private fun resolveContactName(number: String, contacts: List<DeviceContact>): String? {
        return resolveContact(number, contacts)?.name
    }

    private fun resolveLocalTagName(number: String, callNotes: List<CallNoteEntity>): String? {
        val norm = normalizePhoneNumber(number)
        if (norm.length < 4) return null
        val tail = norm.takeLast(10)
        return callNotes.firstOrNull {
            it.contactName.isNotBlank() && (
                it.normalizedNumber == norm ||
                    (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
                )
        }?.contactName
    }

    private fun mergeCallLogs(
        systemLogs: List<CallLogItem>,
        localRecords: List<AppCallRecordEntity>,
        contacts: List<DeviceContact>,
        callNotes: List<CallNoteEntity>,
        hiddenIds: Set<String>
    ): List<CallLogItem> {
        // O(1) indexed lookup maps for instant, lag-free merging even with 5,000+ contacts
        val contactsByExact = HashMap<String, DeviceContact>(contacts.size * 2)
        val contactsByTail = HashMap<String, DeviceContact>(contacts.size * 2)
        for (c in contacts) {
            if (c.normalizedNumber.isNotEmpty()) {
                contactsByExact.putIfAbsent(c.normalizedNumber, c)
                if (c.normalizedNumber.length >= 8) {
                    contactsByTail.putIfAbsent(c.normalizedNumber.takeLast(10), c)
                }
            }
        }
        val notesByExact = HashMap<String, String>(callNotes.size * 2)
        val notesByTail = HashMap<String, String>(callNotes.size * 2)
        for (n in callNotes) {
            if (n.contactName.isNotBlank() && n.normalizedNumber.isNotEmpty()) {
                notesByExact.putIfAbsent(n.normalizedNumber, n.contactName)
                if (n.normalizedNumber.length >= 8) {
                    notesByTail.putIfAbsent(n.normalizedNumber.takeLast(10), n.contactName)
                }
            }
        }

        fun lookupContactFast(norm: String): DeviceContact? {
            if (norm.length < 4) return null
            return contactsByExact[norm]
                ?: if (norm.length >= 8) contactsByTail[norm.takeLast(10)] else null
        }

        fun lookupNoteFast(norm: String): String? {
            if (norm.length < 4) return null
            return notesByExact[norm]
                ?: if (norm.length >= 8) notesByTail[norm.takeLast(10)] else null
        }

        val enrichedSystemLogs = systemLogs
            .filterNot { it.id in hiddenIds }
            .map { sys ->
                val matchedContact = lookupContactFast(sys.normalizedNumber)
                val resolvedName = sys.contactName?.takeIf { it.isNotBlank() }
                    ?: matchedContact?.name
                    ?: lookupNoteFast(sys.normalizedNumber)
                    ?: CallerIdIntelligence.detectOffline(sys.number)?.displayName
                val resolvedPhoto = sys.photoUri?.takeIf { it.isNotBlank() }
                    ?: matchedContact?.photoUri
                if (resolvedName == sys.contactName && resolvedPhoto == sys.photoUri) sys
                else sys.copy(contactName = resolvedName, photoUri = resolvedPhoto)
            }

        val localMapped = localRecords
            .map { rec ->
                val route = runCatching { CallRoute.valueOf(rec.route) }.getOrDefault(CallRoute.SIM_1)
                val matchedContact = lookupContactFast(rec.normalizedNumber)
                CallLogItem(
                    id = "local_${rec.id}",
                    number = rec.number,
                    normalizedNumber = rec.normalizedNumber,
                    contactName = rec.contactName?.takeIf { it.isNotBlank() }
                        ?: matchedContact?.name
                        ?: lookupNoteFast(rec.normalizedNumber)
                        ?: CallerIdIntelligence.detectOffline(rec.number)?.displayName,
                    photoUri = matchedContact?.photoUri,
                    direction = CallDirection.OUTGOING,
                    timestamp = rec.timestamp,
                    durationSeconds = 0L,
                    route = route,
                    isLocalNovaRecord = true
                )
            }
            .filterNot { it.id in hiddenIds }

        val filteredLocal = localMapped.filter { local ->
            if (local.route == CallRoute.BRILLIANT) return@filter true
            enrichedSystemLogs.none { sys ->
                sys.normalizedNumber.takeLast(10) == local.normalizedNumber.takeLast(10) &&
                    kotlin.math.abs(sys.timestamp - local.timestamp) < 15_000L
            }
        }
        return (filteredLocal + enrichedSystemLogs)
            .sortedByDescending { it.timestamp }
            .take(120)
    }

    private fun filterCallLogs(filter: CallFilterType, logs: List<CallLogItem>): List<CallLogItem> {
        return when (filter) {
            CallFilterType.ALL -> logs
            CallFilterType.MISSED -> logs.filter { it.direction == CallDirection.MISSED || it.direction == CallDirection.REJECTED }
            CallFilterType.BRILLIANT -> logs.filter { it.route == CallRoute.BRILLIANT }
            CallFilterType.OUTGOING -> logs.filter { it.direction == CallDirection.OUTGOING }
            CallFilterType.INCOMING -> logs.filter { it.direction == CallDirection.INCOMING }
        }
    }

    private fun filterContacts(query: String, contacts: List<DeviceContact>): List<DeviceContact> {
        val q = query.trim()
        if (q.isEmpty()) return contacts
        val normQ = normalizePhoneNumber(q)
        return contacts.filter { c ->
            c.name.contains(q, ignoreCase = true) ||
                c.phoneNumber.contains(q, ignoreCase = true) ||
                (normQ.isNotEmpty() && c.normalizedNumber.contains(normQ))
        }
    }

    private fun computeT9Matches(dialed: String, contacts: List<DeviceContact>): List<DeviceContact> {
        val digits = dialed.filter { it.isDigit() }
        if (digits.length < 2) return emptyList()
        if (cachedT9ContactsRef !== contacts) {
            cachedT9Index = contacts.map { it to nameToT9(it.name) }
            cachedT9ContactsRef = contacts
        }
        val matches = ArrayList<DeviceContact>(6)
        for ((contact, t9) in cachedT9Index) {
            if (contact.normalizedNumber.contains(digits) || t9.contains(digits)) {
                matches.add(contact)
                if (matches.size >= 6) break
            }
        }
        return matches
    }

    private fun nameToT9(name: String): String {
        val sb = StringBuilder(name.length)
        for (ch in name.lowercase()) {
            when (ch) {
                'a', 'b', 'c' -> sb.append('2')
                'd', 'e', 'f' -> sb.append('3')
                'g', 'h', 'i' -> sb.append('4')
                'j', 'k', 'l' -> sb.append('5')
                'm', 'n', 'o' -> sb.append('6')
                'p', 'q', 'r', 's' -> sb.append('7')
                't', 'u', 'v' -> sb.append('8')
                'w', 'x', 'y', 'z' -> sb.append('9')
                in '0'..'9' -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "DialerVM"

        fun factory(
            application: Application,
            currentUserId: String,
            userDisplayNameHint: String,
            enableCloudCallerId: Boolean = true
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val callerIdRepo = if (enableCloudCallerId && FirebaseApp.getApps(application).isNotEmpty()) {
                    runCatching {
                        val databaseId = application.getString(R.string.firestore_database_id)
                        val firestore = FirebaseFirestore.getInstance(databaseId)
                        CallerIdRepository(db = firestore)
                    }.getOrNull()
                } else {
                    null
                }
                val roomDb = NovaCallDatabase.getInstance(application)
                val prefsRepo = PreferencesRepository(application)
                val dialerRepo = DialerRepository(roomDb.novaCallDao(), prefsRepo)
                val telephony = TelephonyBridgeManager(application)
                DialerViewModel(
                    repository = dialerRepo,
                    telephony = telephony,
                    callerIdRepository = callerIdRepo,
                    currentUserId = currentUserId,
                    userDisplayNameHint = userDisplayNameHint
                ).also { vm ->
                    vm.viewModelScope.launch(Dispatchers.IO) {
                        CallNotificationHelper.ensureChannels(application)
                    }
                }
            }
        }
    }
}
