package com.example.data.telephony

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.local.NovaCallDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.normalizePhoneNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NovaCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val norm = normalizePhoneNumber(rawNumber)

        scope.launch {
            val prefs = runCatching { PreferencesRepository(applicationContext).preferencesFlow.first() }.getOrNull()
            val shouldCheck = prefs?.autoRejectBlocked ?: true

            var blocked = false
            if (shouldCheck && norm.length >= 4) {
                val list = runCatching {
                    NovaCallDatabase.getInstance(applicationContext).novaCallDao().getAllBlockedNumbersOnce()
                }.getOrDefault(emptyList())
                blocked = list.any {
                    it.normalizedNumber == norm ||
                        (norm.length >= 8 && it.normalizedNumber.endsWith(norm.takeLast(10)))
                }
            }

            val response = if (blocked) {
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
            } else {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            }
            respondToCall(callDetails, response)
        }
    }
}
