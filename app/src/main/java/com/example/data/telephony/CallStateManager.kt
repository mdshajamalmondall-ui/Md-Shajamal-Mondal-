package com.example.data.telephony

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.example.InCallActivity
import com.example.data.cloud.CallerIdIntelligence
import com.example.data.cloud.CallerIdRepository
import com.example.data.cloud.CallerIdSource
import com.example.data.local.NovaCallDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.normalizePhoneNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ActiveCallPhase {
    RINGING,
    CONNECTING,
    OUTGOING_RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTING,
    DISCONNECTED
}

data class ActiveCallUiState(
    val number: String,
    val displayName: String,
    val photoUri: String? = null,
    val callerNote: String? = null,
    val carrierLabel: String? = null,
    val callerIdBadge: String? = null,
    val isSpamCaller: Boolean = false,
    val lineLabel: String = "NovaCall",
    val phase: ActiveCallPhase = ActiveCallPhase.CONNECTING,
    val isIncoming: Boolean = false,
    val connectedAtMillis: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isBluetoothAvailable: Boolean = false,
    val isBluetoothOn: Boolean = false,
    val isOnHold: Boolean = false
)

object CallStateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var inCallService: InCallService? = null
    private var currentCall: Call? = null
    private var appContext: Context? = null
    private var wasIncomingRingingUnanswered = false

    private val _activeCall = MutableStateFlow<ActiveCallUiState?>(null)
    val activeCall: StateFlow<ActiveCallUiState?> = _activeCall.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            updateCallSnapshot(call, state)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateCallSnapshot(call, resolveCallState(call))
        }
    }

    fun attachService(service: InCallService) {
        inCallService = service
        appContext = service.applicationContext
    }

    fun detachService() {
        inCallService = null
    }

    fun onCallAdded(context: Context, call: Call) {
        appContext = context.applicationContext
        currentCall?.unregisterCallback(callCallback)
        currentCall = call
        call.registerCallback(callCallback)

        val state = resolveCallState(call)
        val isIncoming = state == Call.STATE_RINGING
        wasIncomingRingingUnanswered = isIncoming

        val rawNumber = call.details?.handle?.schemeSpecificPart.orEmpty()
        val lineLabel = call.details?.accountHandle?.id?.let { id ->
            when {
                id.contains("brilliant", ignoreCase = true) -> "Brilliant VoIP"
                id.endsWith("2") -> "SIM 2"
                else -> "SIM 1"
            }
        } ?: "NovaCall"

        val offlineInfo = CallerIdIntelligence.detectOffline(rawNumber)
        val initialPhase = mapState(state, isIncoming)
        val initialConnectedAt = resolveConnectedTimestamp(call, initialPhase, 0L)

        _activeCall.value = ActiveCallUiState(
            number = rawNumber,
            displayName = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
                ?: offlineInfo?.displayName
                ?: rawNumber,
            carrierLabel = offlineInfo?.carrierOrRegion,
            callerIdBadge = if (offlineInfo?.source == CallerIdSource.SERVICE_DIRECTORY) "Verified Service" else null,
            lineLabel = lineLabel,
            phase = initialPhase,
            isIncoming = isIncoming,
            connectedAtMillis = initialConnectedAt
        )

        // Resolve local contact, Cloud Caller ID (Truecaller-like community lookup), saved notes, and auto-reject
        scope.launch {
            val prefs = runCatching { PreferencesRepository(context).preferencesFlow.first() }.getOrNull()
            val dao = NovaCallDatabase.getInstance(context).novaCallDao()
            val norm = normalizePhoneNumber(rawNumber)

            // Auto-reject blocked incoming calls if enabled
            if (isIncoming && (prefs?.autoRejectBlocked ?: true) && norm.length >= 4) {
                val blocked = runCatching { dao.getAllBlockedNumbersOnce() }.getOrDefault(emptyList())
                val isBlocked = blocked.any {
                    it.normalizedNumber == norm ||
                        (norm.length >= 8 && it.normalizedNumber.endsWith(norm.takeLast(10)))
                }
                if (isBlocked) {
                    wasIncomingRingingUnanswered = false
                    rejectCall()
                    return@launch
                }
            }

            val (contactName, photoUri) = lookupContact(context, rawNumber)
            val savedNote = runCatching {
                dao.getAllCallNotesOnce().firstOrNull {
                    it.normalizedNumber == norm ||
                        (norm.length >= 8 && it.normalizedNumber.endsWith(norm.takeLast(10)))
                }?.note
            }.getOrNull()

            // If caller is NOT saved in device contacts, query NovaCall Cloud Community Caller ID!
            val cloudLookup = if (contactName.isNullOrBlank()) {
                runCatching { CallerIdRepository(context).lookupNumber(rawNumber) }.getOrNull()
            } else null

            val resolvedName = contactName
                ?: cloudLookup?.displayName
                ?: _activeCall.value?.displayName
                ?: rawNumber

            val resolvedBadge = when {
                !contactName.isNullOrBlank() -> null
                cloudLookup?.isSpam == true -> "SPAM • ${cloudLookup.spamReports} reports"
                cloudLookup?.source == CallerIdSource.COMMUNITY_CLOUD -> "Shajamal Caller ID"
                cloudLookup?.source == CallerIdSource.SERVICE_DIRECTORY -> "Verified Service"
                else -> null
            }

            val current = _activeCall.value
            if (current != null) {
                val updated = current.copy(
                    displayName = resolvedName,
                    photoUri = photoUri,
                    callerNote = savedNote,
                    carrierLabel = cloudLookup?.carrierOrRegion ?: current.carrierLabel,
                    callerIdBadge = resolvedBadge,
                    isSpamCaller = cloudLookup?.isSpam == true
                )
                _activeCall.value = updated

                if (isIncoming && (prefs?.notifyIncomingCalls ?: true)) {
                    CallNotificationHelper.showIncomingCallNotification(
                        context = context,
                        callerName = updated.displayName,
                        callerNumber = updated.number,
                        lineLabel = updated.lineLabel
                    )
                } else if (!isIncoming && (prefs?.notifyOngoingCalls ?: true)) {
                    val statusText = when (updated.phase) {
                        ActiveCallPhase.ACTIVE -> "Active Call • ${updated.lineLabel}"
                        ActiveCallPhase.OUTGOING_RINGING -> "Ringing • ${updated.lineLabel}"
                        else -> "Connecting • ${updated.lineLabel}"
                    }
                    CallNotificationHelper.showOngoingCallNotification(
                        context = context,
                        callerName = updated.displayName,
                        callerNumber = updated.number,
                        statusLabel = statusText,
                        isConnected = updated.phase == ActiveCallPhase.ACTIVE
                    )
                }

                if (!isIncoming && (prefs?.autoSpeakerOnCall == true)) {
                    setSpeaker(true)
                }
            }
        }

        launchInCallUi(context)
    }

    fun onCallRemoved(context: Context, call: Call) {
        if (currentCall == call) {
            call.unregisterCallback(callCallback)
            val lastState = _activeCall.value
            currentCall = null
            CallNotificationHelper.cancelActiveCallNotification(context)

            if (wasIncomingRingingUnanswered && lastState != null) {
                scope.launch {
                    val prefs = runCatching { PreferencesRepository(context).preferencesFlow.first() }.getOrNull()
                    if (prefs?.notifyMissedCalls != false) {
                        CallNotificationHelper.showMissedCallNotification(
                            context = context,
                            callerName = lastState.displayName,
                            callerNumber = lastState.number
                        )
                    }
                }
            }
            wasIncomingRingingUnanswered = false
            _activeCall.value = null
        }
    }

    fun onAudioStateChanged(audioState: CallAudioState) {
        val current = _activeCall.value ?: return
        val btSupported = (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0
        _activeCall.value = current.copy(
            isMuted = audioState.isMuted,
            isSpeakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER,
            isBluetoothAvailable = btSupported,
            isBluetoothOn = audioState.route == CallAudioState.ROUTE_BLUETOOTH
        )
    }

    private fun updateCallSnapshot(call: Call, state: Int) {
        val current = _activeCall.value ?: return
        val phase = mapState(state, current.isIncoming)
        if (phase == ActiveCallPhase.ACTIVE) {
            wasIncomingRingingUnanswered = false
        }
        val connectedAt = resolveConnectedTimestamp(call, phase, current.connectedAtMillis)

        val updated = current.copy(
            phase = phase,
            isOnHold = phase == ActiveCallPhase.HOLDING,
            connectedAtMillis = connectedAt
        )
        _activeCall.value = updated

        val ctx = appContext ?: return
        if (phase == ActiveCallPhase.OUTGOING_RINGING || phase == ActiveCallPhase.ACTIVE) {
            scope.launch {
                val prefs = runCatching { PreferencesRepository(ctx).preferencesFlow.first() }.getOrNull()
                if (prefs?.notifyOngoingCalls != false) {
                    val statusLabel = if (phase == ActiveCallPhase.ACTIVE) {
                        "Active Call • ${updated.lineLabel}"
                    } else {
                        "Ringing • ${updated.lineLabel}"
                    }
                    CallNotificationHelper.showOngoingCallNotification(
                        context = ctx,
                        callerName = updated.displayName,
                        callerNumber = updated.number,
                        statusLabel = statusLabel,
                        isConnected = phase == ActiveCallPhase.ACTIVE
                    )
                }
            }
        }
    }

    private fun resolveConnectedTimestamp(call: Call, phase: ActiveCallPhase, existingConnectedAt: Long): Long {
        if (phase != ActiveCallPhase.ACTIVE) return existingConnectedAt
        if (existingConnectedAt > 0L) return existingConnectedAt
        val telecomConnectTime = runCatching { call.details?.connectTimeMillis ?: 0L }.getOrDefault(0L)
        return if (telecomConnectTime > 0L) telecomConnectTime else System.currentTimeMillis()
    }

    private fun launchInCallUi(context: Context) {
        val intent = Intent(context, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { context.startActivity(intent) }
    }

    fun answerCall() {
        wasIncomingRingingUnanswered = false
        runCatching {
            currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    fun rejectCall(quickSmsMessage: String? = null) {
        wasIncomingRingingUnanswered = false
        runCatching {
            val call = currentCall ?: return
            if (resolveCallState(call) == Call.STATE_RINGING) {
                if (!quickSmsMessage.isNullOrBlank()) {
                    call.reject(true, quickSmsMessage)
                } else {
                    call.reject(false, null)
                }
            } else {
                call.disconnect()
            }
        }
    }

    fun endCall() {
        wasIncomingRingingUnanswered = false
        runCatching { currentCall?.disconnect() }
    }

    fun toggleMute() {
        val next = !(_activeCall.value?.isMuted ?: false)
        runCatching { inCallService?.setMuted(next) }
        _activeCall.value = _activeCall.value?.copy(isMuted = next)
    }

    fun toggleSpeaker() {
        val next = !(_activeCall.value?.isSpeakerOn ?: false)
        setSpeaker(next)
    }

    fun setSpeaker(enabled: Boolean) {
        val route = if (enabled) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE
        @Suppress("DEPRECATION")
        runCatching { inCallService?.setAudioRoute(route) }
        _activeCall.value = _activeCall.value?.copy(isSpeakerOn = enabled, isBluetoothOn = false)
    }

    fun toggleHold() {
        val call = currentCall ?: return
        val phase = _activeCall.value?.phase
        if (phase != ActiveCallPhase.ACTIVE && phase != ActiveCallPhase.HOLDING) return
        val currentlyHeld = _activeCall.value?.isOnHold ?: false
        runCatching {
            if (currentlyHeld) call.unhold() else call.hold()
        }
    }

    fun playInCallDtmf(digit: Char) {
        runCatching {
            currentCall?.playDtmfTone(digit)
            currentCall?.stopDtmfTone()
        }
    }

    private fun resolveCallState(call: Call): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
    }

    private fun mapState(state: Int, isIncoming: Boolean): ActiveCallPhase = when (state) {
        Call.STATE_RINGING -> ActiveCallPhase.RINGING
        Call.STATE_DIALING -> ActiveCallPhase.OUTGOING_RINGING
        Call.STATE_NEW, Call.STATE_CONNECTING, Call.STATE_SELECT_PHONE_ACCOUNT -> {
            if (isIncoming) ActiveCallPhase.RINGING else ActiveCallPhase.CONNECTING
        }
        Call.STATE_ACTIVE -> ActiveCallPhase.ACTIVE
        Call.STATE_HOLDING -> ActiveCallPhase.HOLDING
        Call.STATE_DISCONNECTING -> ActiveCallPhase.DISCONNECTING
        else -> ActiveCallPhase.DISCONNECTED
    }

    private fun lookupContact(context: Context, phoneNumber: String): Pair<String?, String?> {
        if (phoneNumber.isBlank()) return null to null
        return runCatching {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0) to cursor.getString(1)
                } else null to null
            } ?: (null to null)
        }.getOrDefault(null to null)
    }
}
