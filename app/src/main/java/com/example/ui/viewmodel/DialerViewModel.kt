package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.DialerRepository
import com.example.data.backup.CallBackupManager
import com.example.data.cloud.CallerCategory
import com.example.data.cloud.CallerDirectoryEntry
import com.example.data.cloud.CallerIdIntelligence
import com.example.data.cloud.CallerIdLookupResult
import com.example.data.cloud.CallerIdRepository
import com.example.data.cloud.CallerIdSource
import com.example.data.cloud.UserCloudProfile
import com.example.data.local.AppCallRecordEntity
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallNoteEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.SpeedDialEntity
import com.example.data.local.NovaCallDatabase
import com.example.data.model.AnalyticsChartBucket
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.AppFontScale
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemePalette
import com.example.data.model.BackgroundDoodleStyle
import com.example.data.model.CallAnalyticsSummary
import com.example.data.model.CallDirection
import com.example.data.model.CallDispatchResult
import com.example.data.model.CallFilterType
import com.example.data.model.CallLogItem
import com.example.data.model.CallRoute
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.DeviceContact
import com.example.data.model.DisplayThemeMode
import com.example.data.model.FavoriteGroup
import com.example.data.model.IncomingCallDisplayStyle
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.SimCardInfo
import com.example.data.model.SlideToAnswerMode
import com.example.data.model.TopContactStat
import com.example.data.model.UserPreferences
import com.example.data.model.normalizePhoneNumber
import com.example.data.telephony.CallNotificationHelper
import com.example.data.telephony.IspNetworkStatus
import com.example.data.telephony.NewContactPayload
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class NovaTab {
    FAVORITES,
    RECENTS,
    CONTACTS,
    KEYPAD
}

@Immutable
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

@Immutable
data class CallerDetailTarget(
    val number: String,
    val normalizedNumber: String,
    val name: String?
)

@Immutable
data class CloudCallerIdSnapshot(
    val profile: UserCloudProfile? = null,
    val myTags: List<CallerDirectoryEntry> = emptyList(),
    val dialedLookup: CallerIdLookupResult? = null,
    val isLookingUp: Boolean = false,
    val isSyncingContacts: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class DialerUiState(
    val activeTab: NovaTab = NovaTab.RECENTS,
    val isSettingsOpen: Boolean = false,
    val isDashboardOpen: Boolean = false,
    val isDefaultDialer: Boolean = false,
    val dialedDigits: String = "",
    val contactSearchQuery: String = "",
    val recentsSearchQuery: String = "",
    val recentsFilter: CallFilterType = CallFilterType.ALL,
    val selectedFavoriteGroup: FavoriteGroup = FavoriteGroup.ALL,
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEKLY,
    val analyticsSummary: CallAnalyticsSummary = CallAnalyticsSummary(),
    val numberSearchQuery: String = "",
    val numberSearchResult: CallerIdLookupResult? = null,
    val isSearchingNumber: Boolean = false,
    val profileLookupResult: CallerIdLookupResult? = null,
    val preferences: UserPreferences = UserPreferences(),
    val permissions: PermissionSnapshot = PermissionSnapshot(),
    val simCards: List<SimCardInfo> = emptyList(),
    val contacts: List<DeviceContact> = emptyList(),
    val favoriteContacts: List<DeviceContact> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val speedDials: List<SpeedDialEntity> = emptyList(),
    val blockedNumbers: List<BlockedNumberEntity> = emptyList(),
    val callNotes: List<CallNoteEntity> = emptyList(),
    val t9Matches: List<DeviceContact> = emptyList(),
    val filteredContacts: List<DeviceContact> = emptyList(),
    val filteredCallLogs: List<CallLogItem> = emptyList(),
    val activeDialedNote: CallNoteEntity? = null,
    val isDialedBlocked: Boolean = false,
    val todayCallCount: Int = 0,
    val selectedCallerDetail: CallerDetailTarget? = null,
    val selectedContactProfile: CallerDetailTarget? = null,
    val callHistoryTarget: CallerDetailTarget? = null,
    val routePickerTarget: CallerDetailTarget? = null,
    val isLoadingSystemData: Boolean = false,
    val cloudCallerId: CloudCallerIdSnapshot = CloudCallerIdSnapshot(),
    val sim1BalanceText: String? = null,
    val sim2BalanceText: String? = null,
    val checkingBalanceSlot: Int? = null,
    val iccSipStatusText: String? = null,
    val isTestingIccSip: Boolean = false,
    val ispNetworkStatus: IspNetworkStatus? = null,
    val isCheckingIsp: Boolean = false
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
    val contacts: List<DeviceContact>,
    val systemCallLogs: List<CallLogItem>,
    val isLoading: Boolean
)

private data class UiInputSnapshot(
    val activeTab: NovaTab,
    val isSettingsOpen: Boolean,
    val isDashboardOpen: Boolean,
    val dialedDigits: String,
    val contactSearchQuery: String,
    val recentsSearchQuery: String,
    val recentsFilter: CallFilterType,
    val selectedFavoriteGroup: FavoriteGroup,
    val analyticsPeriod: AnalyticsPeriod,
    val selectedCallerDetail: CallerDetailTarget?,
    val selectedContactProfile: CallerDetailTarget?,
    val callHistoryTarget: CallerDetailTarget?,
    val routePickerTarget: CallerDetailTarget?
)

private data class DiagnosticsSnapshot(
    val sim1BalanceText: String? = null,
    val sim2BalanceText: String? = null,
    val checkingBalanceSlot: Int? = null,
    val iccSipStatusText: String? = null,
    val isTestingIccSip: Boolean = false,
    val ispNetworkStatus: IspNetworkStatus? = null,
    val isCheckingIsp: Boolean = false,
    val numberSearchQuery: String = "",
    val numberSearchResult: CallerIdLookupResult? = null,
    val isSearchingNumber: Boolean = false,
    val profileLookupResult: CallerIdLookupResult? = null
)

class DialerViewModel(
    private val appContext: Application,
    private val repository: DialerRepository,
    private val telephony: TelephonyBridgeManager,
    private val callerIdRepoFactory: () -> CallerIdRepository?,
    private val currentUserId: String,
    private val userDisplayNameHint: String
) : ViewModel() {

    private val activeTab = MutableStateFlow(NovaTab.RECENTS)
    private val isSettingsOpen = MutableStateFlow(false)
    private val isDashboardOpen = MutableStateFlow(false)
    private val dialedDigits = MutableStateFlow("")
    private val contactSearchQuery = MutableStateFlow("")
    private val recentsSearchQuery = MutableStateFlow("")
    private val recentsFilter = MutableStateFlow(CallFilterType.ALL)
    private val selectedFavoriteGroup = MutableStateFlow(FavoriteGroup.ALL)
    private val analyticsPeriod = MutableStateFlow(AnalyticsPeriod.WEEKLY)
    private val selectedCallerDetail = MutableStateFlow<CallerDetailTarget?>(null)
    private val selectedContactProfile = MutableStateFlow<CallerDetailTarget?>(null)
    private val callHistoryTarget = MutableStateFlow<CallerDetailTarget?>(null)
    private val routePickerTarget = MutableStateFlow<CallerDetailTarget?>(null)
    private val sim1BalanceText = MutableStateFlow<String?>(null)
    private val sim2BalanceText = MutableStateFlow<String?>(null)
    private val checkingBalanceSlot = MutableStateFlow<Int?>(null)
    private val iccSipStatusText = MutableStateFlow<String?>(null)
    private val isTestingIccSip = MutableStateFlow(false)
    private val ispNetworkStatus = MutableStateFlow<IspNetworkStatus?>(null)
    private val isCheckingIsp = MutableStateFlow(false)
    private val numberSearchQuery = MutableStateFlow("")
    private val numberSearchResult = MutableStateFlow<CallerIdLookupResult?>(null)
    private val isSearchingNumber = MutableStateFlow(false)
    private val profileLookupResult = MutableStateFlow<CallerIdLookupResult?>(null)

    @Volatile
    private var callerIdRepository: CallerIdRepository? = null
    private val cloudProfileFlow = MutableStateFlow<UserCloudProfile?>(null)
    private val myTagsFlow = MutableStateFlow<List<CallerDirectoryEntry>>(emptyList())
    private val dialedLookupResult = MutableStateFlow<CallerIdLookupResult?>(null)
    private val isLookingUpCallerId = MutableStateFlow(false)
    private val isSyncingCommunityContacts = MutableStateFlow(false)
    private val cloudErrorMessage = MutableStateFlow<String?>(null)
    private val hiddenCallLogIds = MutableStateFlow<Set<String>>(emptySet())
    private var lookupJob: Job? = null
    private var numberSearchJob: Job? = null
    private var refreshJob: Job? = null
    private var lastSystemRefreshMs: Long = 0L

    // Reference-based memoization caches so tab switches / typing / sheet toggles do ZERO list recomputation
    @Volatile private var cachedT9Index: List<Pair<DeviceContact, String>> = emptyList()
    @Volatile private var cachedT9ContactsRef: List<DeviceContact>? = null
    @Volatile private var lastMergedKey: Any? = null
    @Volatile private var lastMergedLogs: List<CallLogItem> = emptyList()
    @Volatile private var lastTodayCount: Int = 0
    @Volatile private var lastFavKey: Any? = null
    @Volatile private var lastFavorites: List<DeviceContact> = emptyList()
    @Volatile private var lastFilteredContactsKey: Any? = null
    @Volatile private var lastFilteredContacts: List<DeviceContact> = emptyList()
    @Volatile private var lastFilteredLogsKey: Any? = null
    @Volatile private var lastFilteredLogs: List<CallLogItem> = emptyList()
    @Volatile private var lastAnalyticsKey: Any? = null
    @Volatile private var lastAnalyticsSummary: CallAnalyticsSummary = CallAnalyticsSummary()

    private val initialPerms: PermissionSnapshot = currentPermissions()

    private val systemSnapshot = MutableStateFlow(
        SystemDataSnapshot(
            permissions = initialPerms,
            isDefaultDialer = false,
            simCards = emptyList(),
            contacts = emptyList(),
            systemCallLogs = emptyList(),
            isLoading = false
        )
    )

    private val _events = MutableSharedFlow<DialerUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DialerUiEvent> = _events.asSharedFlow()

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

    private val numberLookupSnapshotFlow = combine(
        numberSearchQuery,
        numberSearchResult,
        isSearchingNumber,
        profileLookupResult
    ) { q, res, busy, prof ->
        arrayOf<Any?>(q, res, busy, prof)
    }

    private val diagnosticsFlow = combine(
        combine(sim1BalanceText, sim2BalanceText, checkingBalanceSlot) { b1, b2, slot -> Triple(b1, b2, slot) },
        combine(iccSipStatusText, isTestingIccSip, ispNetworkStatus, isCheckingIsp) { sipTxt, testing, isp, checking ->
            DiagnosticsSnapshot(
                iccSipStatusText = sipTxt,
                isTestingIccSip = testing,
                ispNetworkStatus = isp,
                isCheckingIsp = checking
            )
        },
        numberLookupSnapshotFlow
    ) { bal, net, lookupArr ->
        net.copy(
            sim1BalanceText = bal.first,
            sim2BalanceText = bal.second,
            checkingBalanceSlot = bal.third,
            numberSearchQuery = lookupArr[0] as String,
            numberSearchResult = lookupArr[1] as CallerIdLookupResult?,
            isSearchingNumber = lookupArr[2] as Boolean,
            profileLookupResult = lookupArr[3] as CallerIdLookupResult?
        )
    }

    private val uiInputFlow = combine(
        combine(activeTab, isSettingsOpen, isDashboardOpen) { t, s, d -> Triple(t, s, d) },
        dialedDigits,
        combine(contactSearchQuery, recentsSearchQuery) { cq, rq -> cq to rq },
        combine(recentsFilter, selectedFavoriteGroup, analyticsPeriod) { f, grp, per -> Triple(f, grp, per) },
        combine(selectedCallerDetail, selectedContactProfile, callHistoryTarget, routePickerTarget) { d, p, h, r ->
            listOf(d, p, h, r)
        }
    ) { nav, digits, queries, filters, overlays ->
        UiInputSnapshot(
            activeTab = nav.first,
            isSettingsOpen = nav.second,
            isDashboardOpen = nav.third,
            dialedDigits = digits,
            contactSearchQuery = queries.first,
            recentsSearchQuery = queries.second,
            recentsFilter = filters.first,
            selectedFavoriteGroup = filters.second,
            analyticsPeriod = filters.third,
            selectedCallerDetail = overlays[0],
            selectedContactProfile = overlays[1],
            callHistoryTarget = overlays[2],
            routePickerTarget = overlays[3]
        )
    }

    val uiState: StateFlow<DialerUiState> = combine(
        roomSnapshotFlow,
        combine(systemSnapshot, hiddenCallLogIds) { sys, hidden -> sys to hidden },
        uiInputFlow,
        cloudSnapshotFlow,
        diagnosticsFlow
    ) { room, sysPair, input, cloud, diag ->
        val sys = sysPair.first
        val hiddenIds = sysPair.second

        // Memoized mergeCallLogs — only runs when logs/records/contacts/notes/hiddenIds actually change
        val mergedKey = listOf(sys.systemCallLogs, room.appCallRecords, sys.contacts, room.callNotes, hiddenIds)
        val mergedLogs = if (mergedKey == lastMergedKey) {
            lastMergedLogs
        } else {
            val computed = mergeCallLogs(
                systemLogs = sys.systemCallLogs,
                localRecords = room.appCallRecords,
                contacts = sys.contacts,
                callNotes = room.callNotes,
                hiddenIds = hiddenIds
            )
            val startOfDay = System.currentTimeMillis() - (24L * 60L * 60L * 1000L)
            lastTodayCount = computed.count { it.timestamp >= startOfDay }
            lastMergedLogs = computed
            lastMergedKey = mergedKey
            computed
        }

        val analyticsKey = listOf(input.analyticsPeriod, mergedLogs, room.blockedNumbers.size, room.preferences.language)
        val analytics = if (analyticsKey == lastAnalyticsKey) {
            lastAnalyticsSummary
        } else {
            computeAnalyticsSummary(
                period = input.analyticsPeriod,
                logs = mergedLogs,
                blockedCount = room.blockedNumbers.size,
                isBn = room.preferences.language == AppLanguage.BENGALI
            ).also {
                lastAnalyticsSummary = it
                lastAnalyticsKey = analyticsKey
            }
        }

        val normalizedDial = normalizePhoneNumber(input.dialedDigits)
        val t9Matches = if (input.dialedDigits.length >= 2) {
            computeT9Matches(input.dialedDigits, sys.contacts)
        } else {
            emptyList()
        }

        val filteredContactsKey = Pair(input.contactSearchQuery, sys.contacts)
        val filteredContacts = if (filteredContactsKey == lastFilteredContactsKey) {
            lastFilteredContacts
        } else {
            filterContacts(input.contactSearchQuery, sys.contacts).also {
                lastFilteredContacts = it
                lastFilteredContactsKey = filteredContactsKey
            }
        }

        val filteredLogsKey = Triple(input.recentsFilter, input.recentsSearchQuery, mergedLogs)
        val filteredLogs = if (filteredLogsKey == lastFilteredLogsKey) {
            lastFilteredLogs
        } else {
            filterCallLogs(input.recentsFilter, input.recentsSearchQuery, mergedLogs).also {
                lastFilteredLogs = it
                lastFilteredLogsKey = filteredLogsKey
            }
        }

        val favKey = Triple(sys.contacts, room.speedDials, room.preferences.favoriteNumbers)
        val favorites = if (favKey == lastFavKey) {
            lastFavorites
        } else {
            buildFavoritesList(sys.contacts, room.speedDials, room.preferences.favoriteNumbers).also {
                lastFavorites = it
                lastFavKey = favKey
            }
        }

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

        DialerUiState(
            activeTab = input.activeTab,
            isSettingsOpen = input.isSettingsOpen,
            isDashboardOpen = input.isDashboardOpen,
            isDefaultDialer = sys.isDefaultDialer,
            dialedDigits = input.dialedDigits,
            contactSearchQuery = input.contactSearchQuery,
            recentsSearchQuery = input.recentsSearchQuery,
            recentsFilter = input.recentsFilter,
            selectedFavoriteGroup = input.selectedFavoriteGroup,
            analyticsPeriod = input.analyticsPeriod,
            analyticsSummary = analytics,
            numberSearchQuery = diag.numberSearchQuery,
            numberSearchResult = diag.numberSearchResult,
            isSearchingNumber = diag.isSearchingNumber,
            profileLookupResult = diag.profileLookupResult,
            preferences = room.preferences,
            permissions = sys.permissions,
            simCards = sys.simCards,
            contacts = sys.contacts,
            favoriteContacts = favorites,
            callLogs = mergedLogs,
            speedDials = room.speedDials,
            blockedNumbers = room.blockedNumbers,
            callNotes = room.callNotes,
            t9Matches = t9Matches,
            filteredContacts = filteredContacts,
            filteredCallLogs = filteredLogs,
            activeDialedNote = matchedNote,
            isDialedBlocked = isBlocked,
            todayCallCount = lastTodayCount,
            selectedCallerDetail = input.selectedCallerDetail,
            selectedContactProfile = input.selectedContactProfile,
            callHistoryTarget = input.callHistoryTarget,
            routePickerTarget = input.routePickerTarget,
            isLoadingSystemData = sys.isLoading,
            cloudCallerId = cloud,
            sim1BalanceText = diag.sim1BalanceText,
            sim2BalanceText = diag.sim2BalanceText,
            checkingBalanceSlot = diag.checkingBalanceSlot,
            iccSipStatusText = diag.iccSipStatusText,
            isTestingIccSip = diag.isTestingIccSip,
            ispNetworkStatus = diag.ispNetworkStatus,
            isCheckingIsp = diag.isCheckingIsp
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DialerUiState(permissions = initialPerms)
        )

    init {
        refreshSystemData(force = true)
        // Defer Firestore initialization off the main thread & after first frame so cold start is instantaneous
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                delay(450L)
                val repo = runCatching { callerIdRepoFactory() }.getOrNull() ?: return@launch
                callerIdRepository = repo
                launch {
                    runCatching { repo.ensureUserProfile(userDisplayNameHint) }
                }
                launch {
                    repo.observeUserProfile(currentUserId)
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            Log.w(TAG, "Error observing user cloud profile", firestoreError ?: error)
                            cloudErrorMessage.value = (firestoreError ?: error).localizedMessage
                        }
                        .collect { cloudProfileFlow.value = it }
                }
                launch {
                    repo.observeMyContributedTags(currentUserId)
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            Log.w(TAG, "Error observing contributed caller tags", firestoreError ?: error)
                        }
                        .collect { myTagsFlow.value = it }
                }
            }
        }
        viewModelScope.launch {
            repository.preferences
                .map { Pair(it.sim1Alias, it.sim2Alias) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    refreshSimsOnly(uiState.value.preferences)
                }
        }
        // Auto Call Backup watcher: writes a background JSON snapshot when enabled and local records update
        viewModelScope.launch(Dispatchers.IO) {
            roomSnapshotFlow
                .drop(1)
                .collect { snapshot ->
                    if (snapshot.preferences.autoCallBackupEnabled) {
                        val now = System.currentTimeMillis()
                        if (now - snapshot.preferences.lastBackupTimestamp >= 60_000L) {
                            runCatching {
                                val json = CallBackupManager.buildBackupJson(
                                    repository = repository,
                                    systemCallLogs = systemSnapshot.value.systemCallLogs,
                                    preferences = snapshot.preferences
                                )
                                CallBackupManager.writeAutoLocalSnapshot(appContext, json)
                                repository.updateLastBackupTimestamp(now)
                            }
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        lookupJob?.cancel()
        numberSearchJob?.cancel()
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
        if (!force && (refreshJob?.isActive == true || now - lastSystemRefreshMs < 2_000L)) {
            viewModelScope.launch(Dispatchers.IO) {
                val perms = currentPermissions()
                val isDef = telephony.isDefaultDialer()
                val cur = systemSnapshot.value
                if (cur.permissions != perms || cur.isDefaultDialer != isDef) {
                    systemSnapshot.value = cur.copy(permissions = perms, isDefaultDialer = isDef)
                }
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

            // Phase 1: Load Recents FIRST without Binder contention so first screen appears instantly!
            val logs = telephony.loadSystemCallLogs()
            systemSnapshot.value = systemSnapshot.value.copy(
                permissions = perms,
                isDefaultDialer = isDef,
                systemCallLogs = logs,
                isLoading = false
            )

            // Phase 2: Load Dual-SIM info & Contacts in parallel right after Recents is already on screen
            val prefs = uiState.value.preferences
            coroutineScope {
                val simsDeferred = async { telephony.loadSimCards(prefs) }
                val contactsDeferred = async { telephony.loadDeviceContacts() }
                val sims = simsDeferred.await()
                val contacts = contactsDeferred.await()
                systemSnapshot.value = SystemDataSnapshot(
                    permissions = perms,
                    isDefaultDialer = isDef,
                    simCards = sims,
                    contacts = contacts,
                    systemCallLogs = logs,
                    isLoading = false
                )
            }
        }
    }

    private fun refreshSimsOnly(prefs: UserPreferences) {
        viewModelScope.launch(Dispatchers.IO) {
            val sims = telephony.loadSimCards(prefs)
            systemSnapshot.value = systemSnapshot.value.copy(
                permissions = currentPermissions(),
                isDefaultDialer = telephony.isDefaultDialer(),
                simCards = sims
            )
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

    fun openSystemSoundSettings() = telephony.openSystemSoundSettings()

    fun openSipCallingAccountsSettings() = telephony.openSipCallingAccountsSettings()

    fun openWebUrl(url: String) {
        telephony.openWebUrl(url)
    }

    fun defaultBalanceUssdForSlot(slotIndex: Int): String {
        val state = uiState.value
        val custom = if (slotIndex == 0) state.preferences.sim1BalanceUssd else state.preferences.sim2BalanceUssd
        if (custom.isNotBlank()) return custom
        val sim = if (slotIndex == 0) state.sim1 else state.sim2
        return telephony.defaultBalanceUssdForCarrier(sim.carrierName)
    }

    fun checkSimBalance(slotIndex: Int) {
        val state = uiState.value
        val isBn = state.preferences.language == AppLanguage.BENGALI
        val sim = if (slotIndex == 1) state.sim2 else state.sim1
        val customUssd = if (slotIndex == 1) state.preferences.sim2BalanceUssd else state.preferences.sim1BalanceUssd
        checkingBalanceSlot.value = slotIndex
        telephony.checkSimBalance(sim, customUssd) { result ->
            checkingBalanceSlot.value = null
            result.onSuccess { message ->
                if (slotIndex == 1) sim2BalanceText.value = message else sim1BalanceText.value = message
            }.onFailure { err ->
                val text = if (err is SecurityException) {
                    if (isBn) "ব্যালেন্স চেক করতে আগে ফোন পারমিশন দিন" else "Grant Phone permission first to check balance"
                } else {
                    if (isBn) "ব্যালেন্স চেক ব্যর্থ হয়েছে" else "Balance check failed"
                }
                if (slotIndex == 1) sim2BalanceText.value = text else sim1BalanceText.value = text
                emitMessage(text)
            }
        }
    }

    fun updateSimBalanceUssd(slotIndex: Int, code: String) = viewModelScope.launch {
        repository.updateSimBalanceUssd(slotIndex, code)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(if (isBn) "SIM ${slotIndex + 1} ব্যালেন্স কোড সেভ হয়েছে" else "SIM ${slotIndex + 1} balance code saved")
    }

    fun updateCustomRingtone(uri: String, title: String) = viewModelScope.launch {
        repository.updateCustomRingtone(uri, title)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(if (isBn) "কল টোন সেট হয়েছে: $title" else "Ringtone set: $title")
    }

    fun saveIccSipConfig(
        enabled: Boolean,
        server: String,
        port: Int,
        username: String,
        password: String,
        ispCustomerId: String
    ) = viewModelScope.launch {
        repository.updateIccSipConfig(enabled, server, port, username, password, ispCustomerId)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(
            if (isBn) "ICC Communication IP ও ISP তথ্য সেভ হয়েছে"
            else "ICC Communication IP & ISP settings saved"
        )
    }

    fun testIccSipServer(server: String, port: Int) {
        viewModelScope.launch {
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            isTestingIccSip.value = true
            val res = telephony.testIccSipServer(server, port)
            isTestingIccSip.value = false
            res.onSuccess { ms ->
                iccSipStatusText.value = if (isBn) {
                    "সার্ভার সক্রিয় ($server:$port • ${ms}ms)"
                } else {
                    "Server reachable ($server:$port • ${ms}ms)"
                }
            }.onFailure { err ->
                iccSipStatusText.value = if (isBn) {
                    "সার্ভার পাওয়া যায়নি: ${err.localizedMessage ?: "নেটওয়ার্ক চেক করুন"}"
                } else {
                    "Server unreachable: ${err.localizedMessage ?: "Check internet"}"
                }
            }
        }
    }

    fun refreshIspNetworkStatus() {
        viewModelScope.launch {
            isCheckingIsp.value = true
            val res = telephony.fetchIspNetworkStatus()
            isCheckingIsp.value = false
            res.onSuccess { status ->
                ispNetworkStatus.value = status
            }.onFailure {
                val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
                emitMessage(if (isBn) "ISP নেটওয়ার্ক তথ্য পাওয়া যায়নি" else "Could not fetch ISP status")
            }
        }
    }

    fun openSettings() {
        isSettingsOpen.value = true
    }

    fun closeSettings() {
        isSettingsOpen.value = false
    }

    fun openDashboard() {
        isSettingsOpen.value = false
        selectedContactProfile.value = null
        isDashboardOpen.value = true
    }

    fun closeDashboard() {
        isDashboardOpen.value = false
    }

    fun selectAnalyticsPeriod(period: AnalyticsPeriod) {
        analyticsPeriod.value = period
    }

    fun openContactProfile(number: String, name: String?) {
        val clean = number.trim()
        if (clean.isEmpty()) return
        val norm = normalizePhoneNumber(clean)
        val resolvedName = name?.takeIf { it.isNotBlank() }
            ?: resolveContactName(clean, systemSnapshot.value.contacts)
            ?: resolveLocalTagName(clean, uiState.value.callNotes)
        selectedContactProfile.value = CallerDetailTarget(
            number = clean,
            normalizedNumber = norm,
            name = resolvedName
        )
        profileLookupResult.value = CallerIdIntelligence.detectOffline(clean)
        val repo = callerIdRepository
        if (repo != null && norm.filter { it.isDigit() }.length >= 4) {
            viewModelScope.launch {
                val res = repo.lookupNumber(clean)
                if (selectedContactProfile.value?.normalizedNumber == norm && res != null) {
                    profileLookupResult.value = res
                }
            }
        }
    }

    fun closeContactProfile() {
        selectedContactProfile.value = null
        profileLookupResult.value = null
    }

    fun selectFavoriteGroup(group: FavoriteGroup) {
        selectedFavoriteGroup.value = group
    }

    fun assignFavoriteGroup(number: String, group: FavoriteGroup) = viewModelScope.launch {
        val norm = normalizePhoneNumber(number)
        if (norm.isBlank()) return@launch
        val tail = norm.takeLast(10)
        val updated = uiState.value.preferences.favoriteGroups.toMutableMap()
        val matchingKeys = updated.keys.filter { it == norm || (tail.length >= 8 && it.endsWith(tail)) }
        matchingKeys.forEach { updated.remove(it) }
        if (group != FavoriteGroup.ALL) {
            updated[norm] = group
        }
        repository.updateFavoriteGroups(updated)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        val grpLabel = if (isBn) group.labelBn else group.labelEn
        emitMessage(
            if (group == FavoriteGroup.ALL) {
                if (isBn) "গ্রুপ সরানো হয়েছে" else "Removed from group"
            } else {
                if (isBn) "$grpLabel গ্রুপে যুক্ত হয়েছে" else "Assigned to $grpLabel group"
            }
        )
    }

    fun toggleFavoriteByNumber(number: String, name: String?) = viewModelScope.launch {
        val norm = normalizePhoneNumber(number)
        if (norm.isBlank()) return@launch
        val tail = norm.takeLast(10)
        val currentFavs = uiState.value.preferences.favoriteNumbers
        val isPinned = currentFavs.any { it == norm || (tail.length >= 8 && it.endsWith(tail)) } ||
            uiState.value.favoriteContacts.any {
                it.normalizedNumber == norm || (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
            }
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        if (isPinned) {
            val updated = currentFavs.filterNot {
                it == norm || (tail.length >= 8 && it.endsWith(tail))
            }.toSet()
            repository.updateFavoriteNumbers(updated)
            uiState.value.speedDials
                .firstOrNull { it.normalizedNumber == norm || (tail.length >= 8 && it.normalizedNumber.endsWith(tail)) }
                ?.let { repository.removeSpeedDial(it.digit) }
            emitMessage(if (isBn) "প্রিয় তালিকা থেকে সরানো হয়েছে" else "Removed from Favorites")
        } else {
            repository.updateFavoriteNumbers(currentFavs + norm)
            val label = name?.takeIf { it.isNotBlank() } ?: number
            emitMessage(if (isBn) "$label প্রিয় তালিকায় যোগ হয়েছে ★" else "Added $label to Favorites ★")
        }
    }

    fun scheduleCallReminder(
        number: String,
        name: String,
        note: String,
        delayMinutes: Long
    ) = viewModelScope.launch {
        val norm = normalizePhoneNumber(number)
        if (norm.isBlank()) return@launch
        val triggerAt = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(1L) * 60_000L)
        val cleanName = name.trim().ifBlank { resolveContactName(number, uiState.value.contacts) ?: number }
        val cleanNote = note.trim().ifBlank {
            if (uiState.value.preferences.language == AppLanguage.BENGALI) "কল ব্যাক রিমাইন্ডার" else "Callback reminder"
        }
        repository.saveCallNote(number, cleanName, cleanNote, hasReminder = true)
        val updatedReminders = uiState.value.preferences.callReminderTimestamps.toMutableMap()
        updatedReminders[norm] = triggerAt
        repository.updateCallReminderTimestamps(updatedReminders)
        CallNotificationHelper.scheduleCallReminder(
            context = appContext,
            normalizedNumber = norm,
            displayNumber = number,
            contactName = cleanName,
            note = cleanNote,
            triggerAtMillis = triggerAt
        )
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(triggerAt))
        emitMessage(
            if (isBn) "কল রিমাইন্ডার সেট হয়েছে ($formattedTime)"
            else "Call reminder scheduled for $formattedTime"
        )
    }

    fun clearCallReminder(number: String) = viewModelScope.launch {
        val norm = normalizePhoneNumber(number)
        if (norm.isBlank()) return@launch
        val tail = norm.takeLast(10)
        val updated = uiState.value.preferences.callReminderTimestamps.filterKeys { key ->
            !(key == norm || (tail.length >= 8 && key.endsWith(tail)))
        }
        repository.updateCallReminderTimestamps(updated)
        CallNotificationHelper.cancelCallReminder(appContext, norm)
        val existingNote = uiState.value.callNotes.firstOrNull {
            it.normalizedNumber == norm || (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
        }
        if (existingNote != null) {
            repository.saveCallNote(existingNote.displayNumber, existingNote.contactName, existingNote.note, hasReminder = false)
        }
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(if (isBn) "রিমাইন্ডার বাতিল হয়েছে" else "Reminder cleared")
    }

    fun searchNumberDirectory(query: String) {
        numberSearchQuery.value = query
        numberSearchJob?.cancel()
        val digits = query.filter { it.isDigit() }
        if (digits.length < 3) {
            numberSearchResult.value = null
            isSearchingNumber.value = false
            return
        }
        val localContact = resolveContact(query, uiState.value.contacts)
        val norm = normalizePhoneNumber(query)
        val tail = norm.takeLast(10)
        val localNote = uiState.value.callNotes.firstOrNull {
            it.normalizedNumber == norm || (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
        }
        val offline = CallerIdIntelligence.detectOffline(query)
        if (localContact != null) {
            numberSearchResult.value = CallerIdLookupResult(
                phoneKey = norm,
                displayName = localContact.name,
                carrierOrRegion = offline?.carrierOrRegion.orEmpty().ifBlank { "Saved Contact" },
                category = CallerCategory.PERSONAL,
                source = CallerIdSource.LOCAL_CONTACT
            )
        } else if (localNote != null && localNote.contactName.isNotBlank()) {
            numberSearchResult.value = (offline ?: CallerIdLookupResult(
                phoneKey = norm,
                displayName = localNote.contactName,
                carrierOrRegion = ""
            )).copy(
                displayName = localNote.contactName,
                source = CallerIdSource.COMMUNITY_CLOUD
            )
        } else {
            numberSearchResult.value = offline
        }

        val repo = callerIdRepository
        if (repo != null && digits.length >= 4) {
            numberSearchJob = viewModelScope.launch {
                delay(280L)
                isSearchingNumber.value = true
                val cloudRes = repo.lookupNumber(query)
                if (numberSearchQuery.value == query && cloudRes != null) {
                    if (localContact == null || !cloudRes.isSpam) {
                        numberSearchResult.value = if (localContact != null && cloudRes.displayName.isNullOrBlank()) {
                            CallerIdLookupResult(
                                phoneKey = norm,
                                displayName = localContact.name,
                                carrierOrRegion = cloudRes.carrierOrRegion,
                                category = cloudRes.category,
                                spamReports = cloudRes.spamReports,
                                source = CallerIdSource.LOCAL_CONTACT
                            )
                        } else {
                            cloudRes
                        }
                    }
                }
                isSearchingNumber.value = false
            }
        }
    }

    fun reportSpamNumber(number: String, label: String, alsoBlock: Boolean = true) {
        viewModelScope.launch {
            val cleanNum = number.trim()
            if (cleanNum.isBlank()) return@launch
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            val cleanLabel = label.trim().ifBlank { if (isBn) "স্প্যাম কলার" else "Spam Caller" }
            if (alsoBlock) {
                repository.blockNumber(cleanNum, cleanLabel, "Spam Report")
            }
            val repo = callerIdRepository
            if (repo != null) {
                repo.publishCallerTag(cleanNum, cleanLabel, CallerCategory.SPAM, markAsSpam = true)
            } else {
                repository.saveCallNote(cleanNum, cleanLabel, "Reported as Spam", hasReminder = false)
            }
            if (normalizePhoneNumber(numberSearchQuery.value) == normalizePhoneNumber(cleanNum)) {
                searchNumberDirectory(cleanNum)
            }
            if (selectedContactProfile.value?.normalizedNumber == normalizePhoneNumber(cleanNum)) {
                openContactProfile(cleanNum, cleanLabel)
            }
            emitMessage(
                if (isBn) "$cleanNum স্প্যাম হিসেবে রিপোর্ট করা হয়েছে"
                else "Reported $cleanNum as Spam"
            )
        }
    }

    fun updateDndConfig(
        enabled: Boolean,
        startMinutes: Int = uiState.value.preferences.dndStartMinutes,
        endMinutes: Int = uiState.value.preferences.dndEndMinutes,
        allowFavorites: Boolean = uiState.value.preferences.dndAllowFavorites
    ) = viewModelScope.launch {
        repository.updateDndConfig(enabled, startMinutes, endMinutes, allowFavorites)
    }

    fun updateSpamProtection(enabled: Boolean) = viewModelScope.launch {
        repository.updateSpamProtection(enabled)
    }

    fun updateAutoCallBackup(enabled: Boolean) = viewModelScope.launch {
        repository.updateAutoCallBackup(enabled)
        if (enabled) {
            val prefs = uiState.value.preferences
            val json = CallBackupManager.buildBackupJson(repository, systemSnapshot.value.systemCallLogs, prefs)
            val res = CallBackupManager.writeAutoLocalSnapshot(appContext, json)
            if (res.isSuccess) {
                repository.updateLastBackupTimestamp(System.currentTimeMillis())
            }
        }
    }

    fun exportBackupToUri(uri: Uri) = viewModelScope.launch {
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        val json = CallBackupManager.buildBackupJson(
            repository = repository,
            systemCallLogs = systemSnapshot.value.systemCallLogs,
            preferences = uiState.value.preferences
        )
        val res = CallBackupManager.writeBackupToUri(appContext, uri, json)
        res.onSuccess {
            repository.updateLastBackupTimestamp(System.currentTimeMillis())
            emitMessage(if (isBn) "কল ব্যাকআপ ফাইল সেভ হয়েছে" else "Call backup exported successfully")
        }.onFailure { err ->
            emitMessage(
                if (isBn) "ব্যাকআপ ব্যর্থ: ${err.localizedMessage.orEmpty()}"
                else "Backup failed: ${err.localizedMessage.orEmpty()}"
            )
        }
    }

    fun importBackupFromUri(uri: Uri) = viewModelScope.launch {
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        val res = CallBackupManager.restoreBackupFromUri(
            context = appContext,
            uri = uri,
            repository = repository,
            currentPreferences = uiState.value.preferences
        )
        res.onSuccess { summary ->
            emitMessage(
                if (isBn) "রিস্টোর সম্পন্ন: ${summary.restoredCalls} কল, ${summary.restoredNotes} নোট, ${summary.restoredFavorites} প্রিয়জন"
                else "Restored ${summary.restoredCalls} calls, ${summary.restoredNotes} notes, ${summary.restoredFavorites} favorites"
            )
        }.onFailure { err ->
            emitMessage(
                if (isBn) "রিস্টোর ব্যর্থ: ${err.localizedMessage.orEmpty()}"
                else "Restore failed: ${err.localizedMessage.orEmpty()}"
            )
        }
    }

    fun clearLocalUserData() = viewModelScope.launch {
        repository.clearAllLocalUserData()
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(if (isBn) "লোকাল কল ডাটা ও নোট মুছে ফেলা হয়েছে" else "Local call history & notes cleared")
    }

    fun selectTab(tab: NovaTab) {
        isSettingsOpen.value = false
        isDashboardOpen.value = false
        selectedContactProfile.value = null
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

    fun openCallHistory(number: String, name: String?) {
        callHistoryTarget.value = CallerDetailTarget(
            number = number,
            normalizedNumber = normalizePhoneNumber(number),
            name = name
        )
    }

    fun closeCallHistory() {
        callHistoryTarget.value = null
    }

    fun openRoutePicker(number: String, name: String?) {
        val clean = number.trim()
        if (clean.isEmpty()) {
            val last = uiState.value.callLogs.firstOrNull()?.number.orEmpty()
            if (last.isNotEmpty()) {
                dialedDigits.value = last
                triggerLiveCallerIdLookup(last)
            }
            return
        }
        routePickerTarget.value = CallerDetailTarget(
            number = clean,
            normalizedNumber = normalizePhoneNumber(clean),
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

        val effectiveRoute = when (route) {
            CallRoute.SIM_2 -> CallRoute.SIM_2
            CallRoute.ICC_SIP -> CallRoute.ICC_SIP
            else -> CallRoute.SIM_1
        }
        val result = when (effectiveRoute) {
            CallRoute.SIM_2 -> telephony.placeSimCall(numberToCall, state.sim2)
            CallRoute.ICC_SIP -> telephony.placeIccSipCall(
                rawNumber = numberToCall,
                server = state.preferences.iccSipServer,
                username = state.preferences.iccSipUsername
            )
            else -> telephony.placeSimCall(numberToCall, state.sim1)
        }

        val isBn = state.preferences.language == AppLanguage.BENGALI
        when (result) {
            is CallDispatchResult.Dispatched -> {
                viewModelScope.launch {
                    repository.recordInAppCall(result.formattedNumber, resolvedName, effectiveRoute)
                }
                emitMessage(if (isBn) result.messageBn else result.messageEn)
            }
            is CallDispatchResult.InvalidNumber -> {
                emitMessage(if (isBn) result.reasonBn else result.reasonEn)
            }
            is CallDispatchResult.Error -> {
                emitMessage(if (isBn) result.messageBn else result.messageEn)
            }
            is CallDispatchResult.BrilliantNotInstalled -> Unit
        }
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

    fun hasWriteContactsPermission(): Boolean {
        return telephony.hasPermission(Manifest.permission.WRITE_CONTACTS)
    }

    fun saveContactWithPhoto(
        payload: NewContactPayload,
        onSaved: () -> Unit,
        onNeedPermission: () -> Unit
    ) {
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        if (!telephony.hasPermission(Manifest.permission.WRITE_CONTACTS)) {
            onNeedPermission()
            return
        }
        viewModelScope.launch {
            val res = telephony.saveContactWithPhoto(payload)
            res.onSuccess { savedName ->
                emitMessage(
                    if (isBn) "ছবি ও তথ্যসহ কন্টাক্ট সেভ হয়েছে: $savedName"
                    else "Contact saved with photo & details: $savedName"
                )
                onSaved()
                // Reload contacts immediately so new photo & name appear in Recents & Contacts
                val updatedContacts = telephony.loadDeviceContacts()
                systemSnapshot.value = systemSnapshot.value.copy(contacts = updatedContacts)
            }.onFailure { err ->
                if (err is SecurityException) {
                    onNeedPermission()
                } else {
                    emitMessage(
                        if (isBn) "কন্টাক্ট সেভ করা যায়নি: ${err.localizedMessage.orEmpty()}"
                        else "Could not save contact: ${err.localizedMessage.orEmpty()}"
                    )
                }
            }
        }
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

    fun deleteSingleCallLogEntry(entryId: String) {
        hiddenCallLogIds.value = hiddenCallLogIds.value + entryId
        if (entryId.startsWith("sys_")) {
            systemSnapshot.value = systemSnapshot.value.copy(
                systemCallLogs = systemSnapshot.value.systemCallLogs.filterNot { it.id == entryId }
            )
        }
        viewModelScope.launch {
            if (entryId.startsWith("local_")) {
                entryId.removePrefix("local_").toLongOrNull()?.let { localId ->
                    repository.deleteLocalCallRecord(localId)
                }
            } else if (entryId.startsWith("sys_")) {
                telephony.deleteSystemCallLog(entryId.removePrefix("sys_"))
            }
        }
    }

    fun deleteCallLogItem(item: CallLogItem) {
        val idsToDelete = (item.groupedIds + item.id).distinct().toSet()
        hiddenCallLogIds.value = hiddenCallLogIds.value + idsToDelete
        if (idsToDelete.any { it.startsWith("sys_") }) {
            systemSnapshot.value = systemSnapshot.value.copy(
                systemCallLogs = systemSnapshot.value.systemCallLogs.filterNot { it.id in idsToDelete }
            )
        }
        viewModelScope.launch {
            idsToDelete.forEach { entryId ->
                if (entryId.startsWith("local_")) {
                    entryId.removePrefix("local_").toLongOrNull()?.let { localId ->
                        repository.deleteLocalCallRecord(localId)
                    }
                } else if (entryId.startsWith("sys_")) {
                    telephony.deleteSystemCallLog(entryId.removePrefix("sys_"))
                }
            }
            val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
            emitMessage(if (isBn) "কল হিস্ট্রি থেকে ডিলিট হয়েছে" else "Deleted from call history")
        }
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

    fun updateBackgroundDoodle(style: BackgroundDoodleStyle) = viewModelScope.launch {
        repository.updateBackgroundDoodle(style)
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

    fun updateRecentsSearch(query: String) {
        recentsSearchQuery.value = query
    }

    fun addFavorite(contact: DeviceContact) = viewModelScope.launch {
        val norm = contact.normalizedNumber.ifBlank { normalizePhoneNumber(contact.phoneNumber) }
        if (norm.isBlank()) return@launch
        val updated = uiState.value.preferences.favoriteNumbers + norm
        repository.updateFavoriteNumbers(updated)
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(if (isBn) "${contact.name} প্রিয় তালিকায় যোগ হয়েছে ★" else "Added ${contact.name} to Favorites ★")
    }

    fun removeFavorite(contact: DeviceContact) = viewModelScope.launch {
        val norm = contact.normalizedNumber.ifBlank { normalizePhoneNumber(contact.phoneNumber) }
        val tail = norm.takeLast(10)
        val updated = uiState.value.preferences.favoriteNumbers.filterNot {
            it == norm || (tail.length >= 8 && it.endsWith(tail))
        }.toSet()
        repository.updateFavoriteNumbers(updated)
        // Also remove from local speed dials if matching so unpinning feels instant
        uiState.value.speedDials
            .firstOrNull { it.normalizedNumber == norm || (tail.length >= 8 && it.normalizedNumber.endsWith(tail)) }
            ?.let { repository.removeSpeedDial(it.digit) }
        val isBn = uiState.value.preferences.language == AppLanguage.BENGALI
        emitMessage(if (isBn) "${contact.name} প্রিয় তালিকা থেকে সরানো হয়েছে" else "Removed ${contact.name} from Favorites")
    }

    fun updateLiquidGlass(enabled: Boolean) = viewModelScope.launch {
        repository.updateLiquidGlass(enabled)
    }

    fun updateDisplayThemeMode(mode: DisplayThemeMode) = viewModelScope.launch {
        repository.updateDisplayThemeMode(mode)
    }

    fun updateFontScale(scale: AppFontScale) = viewModelScope.launch {
        repository.updateFontScale(scale)
    }

    fun updateIncomingCallDisplayStyle(style: IncomingCallDisplayStyle) = viewModelScope.launch {
        repository.updateIncomingCallDisplayStyle(style)
    }

    fun updateSlideToAnswerMode(mode: SlideToAnswerMode) = viewModelScope.launch {
        repository.updateSlideToAnswerMode(mode)
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
            .take(250)
    }

    private fun buildFavoritesList(
        contacts: List<DeviceContact>,
        speedDials: List<SpeedDialEntity>,
        pinnedNumbers: Set<String>
    ): List<DeviceContact> {
        val seen = HashSet<String>()
        val result = ArrayList<DeviceContact>()
        for (c in contacts) {
            val norm = c.normalizedNumber
            val isPinned = c.isStarred || pinnedNumbers.contains(norm) ||
                (norm.length >= 8 && pinnedNumbers.any { it.endsWith(norm.takeLast(10)) })
            if (isPinned && seen.add(norm.takeLast(10).ifEmpty { c.id })) {
                result.add(c.copy(isStarred = true))
            }
        }
        for (sd in speedDials) {
            val key = sd.normalizedNumber.takeLast(10).ifEmpty { sd.phoneNumber }
            if (seen.add(key)) {
                result.add(
                    DeviceContact(
                        id = "sd_${sd.digit}",
                        name = sd.name,
                        phoneNumber = sd.phoneNumber,
                        normalizedNumber = sd.normalizedNumber,
                        photoUri = null,
                        isStarred = true
                    )
                )
            }
        }
        return result.sortedBy { it.name.lowercase() }
    }

    private fun filterCallLogs(
        filter: CallFilterType,
        query: String,
        logs: List<CallLogItem>
    ): List<CallLogItem> {
        val byFilter = when (filter) {
            CallFilterType.ALL -> logs
            CallFilterType.MISSED -> logs.filter { it.direction == CallDirection.MISSED || it.direction == CallDirection.REJECTED }
            CallFilterType.SIP_VOIP -> logs.filter { it.route == CallRoute.ICC_SIP }
            CallFilterType.CELLULAR -> logs.filter { it.route == CallRoute.SIM_1 || it.route == CallRoute.SIM_2 }
            CallFilterType.BRILLIANT -> logs
            CallFilterType.OUTGOING -> logs.filter { it.direction == CallDirection.OUTGOING }
            CallFilterType.INCOMING -> logs.filter { it.direction == CallDirection.INCOMING }
        }
        val q = query.trim()
        val matched = if (q.isEmpty()) {
            byFilter
        } else {
            val normQ = normalizePhoneNumber(q)
            byFilter.filter { item ->
                (item.contactName?.contains(q, ignoreCase = true) == true) ||
                    item.number.contains(q, ignoreCase = true) ||
                    (normQ.isNotEmpty() && item.normalizedNumber.contains(normQ))
            }
        }
        return groupCallLogsByNumber(matched)
    }

    private fun groupCallLogsByNumber(logs: List<CallLogItem>): List<CallLogItem> {
        if (logs.isEmpty()) return emptyList()
        val grouped = LinkedHashMap<String, MutableList<CallLogItem>>(logs.size)
        for (item in logs) {
            val key = when {
                item.normalizedNumber.length >= 8 -> item.normalizedNumber.takeLast(10)
                item.normalizedNumber.isNotBlank() -> item.normalizedNumber
                item.number.isNotBlank() -> item.number.trim()
                else -> item.id
            }
            grouped.getOrPut(key) { ArrayList(4) }.add(item)
        }
        val result = ArrayList<CallLogItem>(grouped.size)
        for (entries in grouped.values) {
            val latest = entries.first()
            if (entries.size == 1) {
                result.add(latest.copy(callCount = 1, groupedIds = listOf(latest.id)))
            } else {
                val bestName = latest.contactName?.takeIf { it.isNotBlank() }
                    ?: entries.firstOrNull { !it.contactName.isNullOrBlank() }?.contactName
                val bestPhoto = latest.photoUri?.takeIf { it.isNotBlank() }
                    ?: entries.firstOrNull { !it.photoUri.isNullOrBlank() }?.photoUri
                result.add(
                    latest.copy(
                        contactName = bestName,
                        photoUri = bestPhoto,
                        callCount = entries.size,
                        groupedIds = entries.map { it.id }
                    )
                )
            }
        }
        return result
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

    private fun computeAnalyticsSummary(
        period: AnalyticsPeriod,
        logs: List<CallLogItem>,
        blockedCount: Int,
        isBn: Boolean
    ): CallAnalyticsSummary {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val windowStart = when (period) {
            AnalyticsPeriod.DAILY -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            AnalyticsPeriod.WEEKLY -> now - (6L * 24L * 60L * 60L * 1000L)
            AnalyticsPeriod.MONTHLY -> now - (27L * 24L * 60L * 60L * 1000L)
        }
        val filtered = logs.filter { it.timestamp >= windowStart }

        var outgoing = 0
        var incoming = 0
        var missed = 0
        var totalDuration = 0L
        var sim1Count = 0
        var sim2Count = 0
        val contactAgg = LinkedHashMap<String, MutableList<CallLogItem>>()

        for (item in filtered) {
            when (item.direction) {
                CallDirection.OUTGOING -> outgoing++
                CallDirection.INCOMING -> incoming++
                CallDirection.MISSED, CallDirection.REJECTED -> missed++
            }
            totalDuration += item.durationSeconds
            when (item.route) {
                CallRoute.SIM_2 -> sim2Count++
                else -> sim1Count++
            }
            val key = item.normalizedNumber.takeLast(10).ifBlank { item.number }
            if (key.isNotBlank()) {
                contactAgg.getOrPut(key) { ArrayList(4) }.add(item)
            }
        }

        val buckets = when (period) {
            AnalyticsPeriod.DAILY -> {
                val labels = if (isBn) listOf("০-৪", "৪-৮", "৮-১২", "১২-১৬", "১৬-২০", "২০-২৪")
                else listOf("0-4h", "4-8h", "8-12h", "12-16h", "16-20h", "20-24h")
                val outArr = IntArray(6)
                val inArr = IntArray(6)
                val missArr = IntArray(6)
                for (item in filtered) {
                    cal.timeInMillis = item.timestamp
                    val slot = (cal.get(Calendar.HOUR_OF_DAY) / 4).coerceIn(0, 5)
                    when (item.direction) {
                        CallDirection.OUTGOING -> outArr[slot]++
                        CallDirection.INCOMING -> inArr[slot]++
                        CallDirection.MISSED, CallDirection.REJECTED -> missArr[slot]++
                    }
                }
                labels.indices.map { idx ->
                    AnalyticsChartBucket(labels[idx], outArr[idx], inArr[idx], missArr[idx])
                }
            }
            AnalyticsPeriod.WEEKLY -> {
                val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
                val bucketsList = ArrayList<AnalyticsChartBucket>(7)
                for (offset in 6 downTo 0) {
                    cal.timeInMillis = now - (offset * 24L * 60L * 60L * 1000L)
                    val targetYear = cal.get(Calendar.YEAR)
                    val targetDay = cal.get(Calendar.DAY_OF_YEAR)
                    val label = dayFmt.format(cal.time).take(3)
                    var o = 0
                    var i = 0
                    var m = 0
                    for (item in filtered) {
                        cal.timeInMillis = item.timestamp
                        if (cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.DAY_OF_YEAR) == targetDay) {
                            when (item.direction) {
                                CallDirection.OUTGOING -> o++
                                CallDirection.INCOMING -> i++
                                CallDirection.MISSED, CallDirection.REJECTED -> m++
                            }
                        }
                    }
                    bucketsList.add(AnalyticsChartBucket(label, o, i, m))
                }
                bucketsList
            }
            AnalyticsPeriod.MONTHLY -> {
                val labels = if (isBn) listOf("১ম স.", "২য় স.", "৩য় স.", "৪র্থ স.")
                else listOf("W1", "W2", "W3", "W4")
                val outArr = IntArray(4)
                val inArr = IntArray(4)
                val missArr = IntArray(4)
                val weekMs = 7L * 24L * 60L * 60L * 1000L
                for (item in filtered) {
                    val ageWeeks = ((now - item.timestamp).coerceAtLeast(0L) / weekMs).toInt().coerceIn(0, 3)
                    val idx = 3 - ageWeeks
                    when (item.direction) {
                        CallDirection.OUTGOING -> outArr[idx]++
                        CallDirection.INCOMING -> inArr[idx]++
                        CallDirection.MISSED, CallDirection.REJECTED -> missArr[idx]++
                    }
                }
                labels.indices.map { idx ->
                    AnalyticsChartBucket(labels[idx], outArr[idx], inArr[idx], missArr[idx])
                }
            }
        }

        val topContacts = contactAgg.values
            .sortedByDescending { it.size }
            .take(5)
            .map { entries ->
                val latest = entries.first()
                TopContactStat(
                    displayName = entries.firstOrNull { !it.contactName.isNullOrBlank() }?.contactName
                        ?: latest.number,
                    number = latest.number,
                    photoUri = entries.firstOrNull { !it.photoUri.isNullOrBlank() }?.photoUri,
                    callCount = entries.size,
                    totalDurationSeconds = entries.sumOf { it.durationSeconds }
                )
            }

        return CallAnalyticsSummary(
            period = period,
            totalCalls = filtered.size,
            outgoingCalls = outgoing,
            incomingCalls = incoming,
            missedCalls = missed,
            totalDurationSeconds = totalDuration,
            sim1Calls = sim1Count,
            sim2Calls = sim2Count,
            blockedCount = blockedCount,
            buckets = buckets,
            topContacts = topContacts
        )
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
                val callerIdRepoFactory: () -> CallerIdRepository? = {
                    if (enableCloudCallerId && FirebaseApp.getApps(application).isNotEmpty()) {
                        runCatching {
                            val databaseId = application.getString(R.string.firestore_database_id)
                            val firestore = FirebaseFirestore.getInstance(databaseId)
                            CallerIdRepository(db = firestore)
                        }.getOrNull()
                    } else {
                        null
                    }
                }
                val roomDb = NovaCallDatabase.getInstance(application)
                val prefsRepo = PreferencesRepository(application)
                val dialerRepo = DialerRepository(roomDb.novaCallDao(), prefsRepo)
                val telephony = TelephonyBridgeManager(application)
                DialerViewModel(
                    appContext = application,
                    repository = dialerRepo,
                    telephony = telephony,
                    callerIdRepoFactory = callerIdRepoFactory,
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
