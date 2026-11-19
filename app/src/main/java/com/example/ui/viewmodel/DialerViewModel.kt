package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DialerRepository
import com.example.data.local.AppCallRecordEntity
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallNoteEntity
import com.example.data.local.NovaCallDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppLanguage
import com.example.data.model.BrilliantBridgeStatus
import com.example.data.model.BrilliantLaunchMode
import com.example.data.model.CallDirection
import com.example.data.model.CallDispatchResult
import com.example.data.model.CallFilterType
import com.example.data.model.CallLogItem
import com.example.data.model.CallRoute
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.DeviceContact
import com.example.data.model.SimCardInfo
import com.example.data.model.UserPreferences
import com.example.data.model.normalizePhoneNumber
import com.example.data.telephony.TelephonyBridgeManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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
    val hasReadCallLog: Boolean = false
) {
    val allGranted: Boolean
        get() = hasCallPhone && hasReadPhoneState && hasReadContacts && hasReadCallLog
}

data class CallerDetailTarget(
    val number: String,
    val normalizedNumber: String,
    val name: String?
)

data class DialerUiState(
    val activeTab: NovaTab = NovaTab.KEYPAD,
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
    val isLoadingSystemData: Boolean = false
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
    val simCards: List<SimCardInfo>,
    val brilliantStatus: BrilliantBridgeStatus,
    val contacts: List<DeviceContact>,
    val systemCallLogs: List<CallLogItem>,
    val isLoading: Boolean
)

private data class UiInputSnapshot(
    val activeTab: NovaTab,
    val dialedDigits: String,
    val contactSearchQuery: String,
    val recentsFilter: CallFilterType,
    val selectedCallerDetail: CallerDetailTarget?,
    val routePickerTarget: CallerDetailTarget?
)

class DialerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NovaCallDatabase.getInstance(application)
    private val prefsRepo = PreferencesRepository(application)
    private val repository = DialerRepository(database.novaCallDao(), prefsRepo)
    private val telephony = TelephonyBridgeManager(application)

    private val activeTab = MutableStateFlow(NovaTab.KEYPAD)
    private val dialedDigits = MutableStateFlow("")
    private val contactSearchQuery = MutableStateFlow("")
    private val recentsFilter = MutableStateFlow(CallFilterType.ALL)
    private val selectedCallerDetail = MutableStateFlow<CallerDetailTarget?>(null)
    private val routePickerTarget = MutableStateFlow<CallerDetailTarget?>(null)

    private val systemSnapshot = MutableStateFlow(
        SystemDataSnapshot(
            permissions = currentPermissions(),
            simCards = emptyList(),
            brilliantStatus = BrilliantBridgeStatus(),
            contacts = emptyList(),
            systemCallLogs = emptyList(),
            isLoading = false
        )
    )

    private val _events = MutableSharedFlow<DialerUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DialerUiEvent> = _events.asSharedFlow()

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
        activeTab,
        dialedDigits,
        contactSearchQuery,
        recentsFilter,
        combine(selectedCallerDetail, routePickerTarget) { d, r -> d to r }
    ) { tab, digits, query, filter, overlays ->
        UiInputSnapshot(tab, digits, query, filter, overlays.first, overlays.second)
    }

    val uiState: StateFlow<DialerUiState> = combine(
        roomSnapshotFlow,
        systemSnapshot,
        uiInputFlow
    ) { room, sys, input ->
        val mergedLogs = mergeCallLogs(
            systemLogs = sys.systemCallLogs,
            localRecords = room.appCallRecords,
            contacts = sys.contacts
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
            isLoadingSystemData = sys.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DialerUiState()
    )

    init {
        refreshSystemData()
        viewModelScope.launch {
            repository.preferences.collect { prefs ->
                refreshBridgeAndSims(prefs)
            }
        }
    }

    override fun onCleared() {
        telephony.releaseToneGenerator()
        super.onCleared()
    }

    private fun currentPermissions(): PermissionSnapshot = PermissionSnapshot(
        hasCallPhone = telephony.hasPermission(Manifest.permission.CALL_PHONE),
        hasReadPhoneState = telephony.hasPermission(Manifest.permission.READ_PHONE_STATE),
        hasReadContacts = telephony.hasPermission(Manifest.permission.READ_CONTACTS),
        hasReadCallLog = telephony.hasPermission(Manifest.permission.READ_CALL_LOG)
    )

    fun refreshSystemData() {
        viewModelScope.launch {
            val perms = currentPermissions()
            systemSnapshot.value = systemSnapshot.value.copy(permissions = perms, isLoading = true)
            val prefs = uiState.value.preferences
            val sims = telephony.loadSimCards(prefs)
            val brilliant = telephony.inspectBrilliantBridge(prefs.brilliantPackageName)
            val contacts = telephony.loadDeviceContacts()
            val logs = telephony.loadSystemCallLogs()
            systemSnapshot.value = SystemDataSnapshot(
                permissions = perms,
                simCards = sims,
                brilliantStatus = brilliant,
                contacts = contacts,
                systemCallLogs = logs,
                isLoading = false
            )
        }
    }

    private fun refreshBridgeAndSims(prefs: UserPreferences) {
        viewModelScope.launch {
            val sims = telephony.loadSimCards(prefs)
            val brilliant = telephony.inspectBrilliantBridge(prefs.brilliantPackageName)
            systemSnapshot.value = systemSnapshot.value.copy(
                permissions = currentPermissions(),
                simCards = sims,
                brilliantStatus = brilliant
            )
        }
    }

    fun selectTab(tab: NovaTab) {
        activeTab.value = tab
    }

    fun prefillNumberFromIntent(rawNumber: String) {
        val clean = normalizePhoneNumber(rawNumber)
        if (clean.isNotEmpty()) {
            dialedDigits.value = clean
            activeTab.value = NovaTab.KEYPAD
        }
    }

    fun onDialDigitPress(symbol: String) {
        if (dialedDigits.value.length < 24) {
            dialedDigits.value += symbol
        }
        if (uiState.value.preferences.dtmfSoundEnabled) {
            telephony.playDtmfTone(symbol)
        }
    }

    fun onDialBackspace() {
        if (dialedDigits.value.isNotEmpty()) {
            dialedDigits.value = dialedDigits.value.dropLast(1)
        }
    }

    fun onDialClear() {
        dialedDigits.value = ""
    }

    fun setDialedNumber(number: String) {
        dialedDigits.value = number.trim()
        activeTab.value = NovaTab.KEYPAD
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

    fun triggerDefaultOrPickCall(number: String, contactName: String? = null) {
        val state = uiState.value
        val trimmed = number.trim()
        if (trimmed.isEmpty()) {
            val lastOutgoing = state.callLogs.firstOrNull()?.number
            if (!lastOutgoing.isNullOrBlank()) {
                dialedDigits.value = lastOutgoing
            }
            return
        }
        when (state.preferences.defaultRoute) {
            DefaultRoutePreference.ASK -> openRoutePicker(trimmed, contactName)
            DefaultRoutePreference.SIM_1 -> placeCall(trimmed, CallRoute.SIM_1, contactName)
            DefaultRoutePreference.SIM_2 -> placeCall(trimmed, CallRoute.SIM_2, contactName)
            DefaultRoutePreference.BRILLIANT -> placeCall(trimmed, CallRoute.BRILLIANT, contactName)
        }
    }

    fun placeCall(rawNumber: String, route: CallRoute, contactName: String? = null) {
        val state = uiState.value
        val numberToCall = rawNumber.trim().ifEmpty {
            val last = state.callLogs.firstOrNull()?.number.orEmpty()
            if (last.isNotEmpty()) {
                dialedDigits.value = last
            }
            return
        }

        val resolvedName = contactName ?: resolveContactName(numberToCall, state.contacts)
        val result = when (route) {
            CallRoute.SIM_1 -> telephony.placeSimCall(numberToCall, state.sim1)
            CallRoute.SIM_2 -> telephony.placeSimCall(numberToCall, state.sim2)
            CallRoute.BRILLIANT -> telephony.placeBrilliantCall(
                rawNumber = numberToCall,
                bridgeStatus = state.brilliantStatus,
                prefs = state.preferences
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

    fun clearLocalCallRecords() {
        viewModelScope.launch { repository.clearLocalCallRecords() }
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

    fun updateDefaultRoute(route: DefaultRoutePreference) = viewModelScope.launch {
        repository.updateDefaultRoute(route)
    }

    fun updateSimAlias(slotIndex: Int, alias: String) = viewModelScope.launch {
        repository.updateSimAlias(slotIndex, alias)
    }

    fun updateDtmfSound(enabled: Boolean) = viewModelScope.launch {
        repository.updateDtmfSound(enabled)
    }

    fun updateHaptic(enabled: Boolean) = viewModelScope.launch {
        repository.updateHaptic(enabled)
    }

    fun updateDarkTheme(enabled: Boolean) = viewModelScope.launch {
        repository.updateDarkTheme(enabled)
    }

    fun updateLanguage(language: AppLanguage) = viewModelScope.launch {
        repository.updateLanguage(language)
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

    private fun resolveContactName(number: String, contacts: List<DeviceContact>): String? {
        val norm = normalizePhoneNumber(number)
        if (norm.length < 4) return null
        val tail = norm.takeLast(10)
        return contacts.firstOrNull {
            it.normalizedNumber == norm ||
                (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
        }?.name
    }

    private fun mergeCallLogs(
        systemLogs: List<CallLogItem>,
        localRecords: List<AppCallRecordEntity>,
        contacts: List<DeviceContact>
    ): List<CallLogItem> {
        val localMapped = localRecords.map { rec ->
            val route = runCatching { CallRoute.valueOf(rec.route) }.getOrDefault(CallRoute.SIM_1)
            CallLogItem(
                id = "local_${rec.id}",
                number = rec.number,
                normalizedNumber = rec.normalizedNumber,
                contactName = rec.contactName ?: resolveContactName(rec.number, contacts),
                direction = CallDirection.OUTGOING,
                timestamp = rec.timestamp,
                durationSeconds = 0L,
                route = route,
                isLocalNovaRecord = true
            )
        }
        // Keep local records that aren't immediate duplicates of system outgoing logs within 15s (always preserve BRILLIANT records)
        val filteredLocal = localMapped.filter { local ->
            if (local.route == CallRoute.BRILLIANT) return@filter true
            systemLogs.none { sys ->
                sys.normalizedNumber.takeLast(10) == local.normalizedNumber.takeLast(10) &&
                    kotlin.math.abs(sys.timestamp - local.timestamp) < 15_000L
            }
        }
        return (filteredLocal + systemLogs)
            .sortedByDescending { it.timestamp }
            .take(150)
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
        return contacts.filter { contact ->
            contact.normalizedNumber.contains(digits) || nameToT9(contact.name).contains(digits)
        }.take(6)
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
}
