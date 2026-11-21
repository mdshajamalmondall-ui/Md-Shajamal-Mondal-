package com.example.data.telephony

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.R
import com.example.data.cloud.CallerCategory
import com.example.data.cloud.CallerIdRepository
import com.example.data.local.NovaCallDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.normalizePhoneNumber
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

class NovaCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val norm = normalizePhoneNumber(rawNumber)
        val tail = norm.takeLast(10)

        scope.launch {
            val prefs = runCatching { PreferencesRepository(applicationContext).preferencesFlow.first() }.getOrNull()
            val shouldCheckBlocked = prefs?.autoRejectBlocked ?: true
            val shouldCheckSpam = prefs?.spamProtectionEnabled ?: true

            var blocked = false
            if (shouldCheckBlocked && norm.length >= 4) {
                val list = runCatching {
                    NovaCallDatabase.getInstance(applicationContext).novaCallDao().getAllBlockedNumbersOnce()
                }.getOrDefault(emptyList())
                blocked = list.any {
                    it.normalizedNumber == norm ||
                        (tail.length >= 8 && it.normalizedNumber.endsWith(tail))
                }
            }

            // Smart Spam Call Protection: check community directory quickly (<= 1.2s) if not already blocked
            var highRiskSpam = false
            if (!blocked && shouldCheckSpam && norm.length >= 6 && FirebaseApp.getApps(applicationContext).isNotEmpty()) {
                val lookup = withTimeoutOrNull(1200L) {
                    runCatching {
                        val dbId = applicationContext.getString(R.string.firestore_database_id)
                        CallerIdRepository(FirebaseFirestore.getInstance(dbId)).lookupNumber(rawNumber)
                    }.getOrNull()
                }
                if (lookup != null && (lookup.category == CallerCategory.SCAM || lookup.spamReports >= 3L)) {
                    highRiskSpam = true
                }
            }

            // DND Mode quiet-hours check
            var dndSilence = false
            if (!blocked && !highRiskSpam && prefs?.dndEnabled == true) {
                val nowCal = Calendar.getInstance()
                val nowMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                val inDndWindow = isInsideDndWindow(nowMins, prefs.dndStartMinutes, prefs.dndEndMinutes)
                if (inDndWindow) {
                    val isFav = prefs.dndAllowFavorites && (
                        prefs.favoriteNumbers.contains(norm) ||
                            (tail.length >= 8 && prefs.favoriteNumbers.any { it.endsWith(tail) })
                        )
                    if (!isFav) {
                        dndSilence = true
                    }
                }
            }

            val response = when {
                blocked || highRiskSpam -> CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
                dndSilence -> CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
                else -> CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            }
            respondToCall(callDetails, response)
        }
    }

    private fun isInsideDndWindow(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            // Overnight schedule (e.g., 23:00 to 07:00)
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }
}
