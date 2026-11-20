package com.example.data

import com.example.data.local.AppCallRecordEntity
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallNoteEntity
import com.example.data.local.NovaCallDao
import com.example.data.local.PreferencesRepository
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppLanguage
import com.example.data.model.BrilliantLaunchMode
import com.example.data.model.CallRoute
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.UserPreferences
import com.example.data.model.normalizePhoneNumber
import kotlinx.coroutines.flow.Flow

class DialerRepository(
    private val dao: NovaCallDao,
    private val preferencesRepository: PreferencesRepository
) {
    val speedDials: Flow<List<SpeedDialEntity>> = dao.observeSpeedDials()
    val blockedNumbers: Flow<List<BlockedNumberEntity>> = dao.observeBlockedNumbers()
    val callNotes: Flow<List<CallNoteEntity>> = dao.observeCallNotes()
    val appCallRecords: Flow<List<AppCallRecordEntity>> = dao.observeAppCallRecords()
    val preferences: Flow<UserPreferences> = preferencesRepository.preferencesFlow

    suspend fun saveSpeedDial(digit: Int, name: String, number: String, route: CallRoute) {
        val normalized = normalizePhoneNumber(number)
        if (normalized.isEmpty()) return
        dao.upsertSpeedDial(
            SpeedDialEntity(
                digit = digit.coerceIn(1, 9),
                name = name.trim().ifBlank { number },
                phoneNumber = number.trim(),
                normalizedNumber = normalized,
                preferredRoute = route.name
            )
        )
    }

    suspend fun removeSpeedDial(digit: Int) = dao.deleteSpeedDial(digit)

    suspend fun blockNumber(number: String, label: String, reason: String) {
        val normalized = normalizePhoneNumber(number)
        if (normalized.isEmpty()) return
        dao.upsertBlockedNumber(
            BlockedNumberEntity(
                normalizedNumber = normalized,
                displayNumber = number.trim(),
                label = label.trim().ifBlank { number.trim() },
                reason = reason.trim()
            )
        )
    }

    suspend fun unblockNumber(normalizedNumber: String) = dao.deleteBlockedNumber(normalizedNumber)

    suspend fun saveCallNote(number: String, contactName: String, note: String, hasReminder: Boolean) {
        val normalized = normalizePhoneNumber(number)
        if (normalized.isEmpty() || note.isBlank()) return
        dao.upsertCallNote(
            CallNoteEntity(
                normalizedNumber = normalized,
                displayNumber = number.trim(),
                contactName = contactName.trim().ifBlank { number.trim() },
                note = note.trim(),
                hasReminder = hasReminder,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCallNote(normalizedNumber: String) = dao.deleteCallNote(normalizedNumber)

    suspend fun recordInAppCall(number: String, contactName: String?, route: CallRoute) {
        val normalized = normalizePhoneNumber(number)
        if (normalized.isEmpty()) return
        dao.insertAppCallRecord(
            AppCallRecordEntity(
                number = number.trim(),
                normalizedNumber = normalized,
                contactName = contactName?.takeIf { it.isNotBlank() },
                route = route.name,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLocalCallRecord(id: Long) = dao.deleteAppCallRecordById(id)

    suspend fun clearLocalCallRecords() = dao.clearAppCallRecords()

    suspend fun updateBrilliantPackage(pkg: String) = preferencesRepository.updateBrilliantPackage(pkg)
    suspend fun updateBrilliantLaunchMode(mode: BrilliantLaunchMode) = preferencesRepository.updateBrilliantLaunchMode(mode)
    suspend fun updateBrilliantAutoFormatBd(enabled: Boolean) = preferencesRepository.updateBrilliantAutoFormatBd(enabled)
    suspend fun updateBrilliantAutoCopy(enabled: Boolean) = preferencesRepository.updateBrilliantAutoCopy(enabled)
    suspend fun updateBrilliantSmartPrefixes(prefixes: String) = preferencesRepository.updateBrilliantSmartPrefixes(prefixes)
    suspend fun updateBrilliantExternalHandoff(enabled: Boolean) = preferencesRepository.updateBrilliantExternalHandoff(enabled)
    suspend fun updateDefaultRoute(route: DefaultRoutePreference) = preferencesRepository.updateDefaultRoute(route)
    suspend fun updateSimAlias(slotIndex: Int, alias: String) = preferencesRepository.updateSimAlias(slotIndex, alias)
    suspend fun updateDtmfSound(enabled: Boolean) = preferencesRepository.updateDtmfSound(enabled)
    suspend fun updateKeypadSoundStyle(style: KeypadSoundStyle) = preferencesRepository.updateKeypadSoundStyle(style)
    suspend fun updateKeypadSoundVolume(volume: Float) = preferencesRepository.updateKeypadSoundVolume(volume)
    suspend fun updateHaptic(enabled: Boolean) = preferencesRepository.updateHaptic(enabled)
    suspend fun updateDarkTheme(enabled: Boolean) = preferencesRepository.updateDarkTheme(enabled)
    suspend fun updateLanguage(language: AppLanguage) = preferencesRepository.updateLanguage(language)
    suspend fun updateNotifyIncoming(enabled: Boolean) = preferencesRepository.updateNotifyIncoming(enabled)
    suspend fun updateNotifyMissed(enabled: Boolean) = preferencesRepository.updateNotifyMissed(enabled)
    suspend fun updateNotifyOngoing(enabled: Boolean) = preferencesRepository.updateNotifyOngoing(enabled)
    suspend fun updateVibrateOnRing(enabled: Boolean) = preferencesRepository.updateVibrateOnRing(enabled)
    suspend fun updateAutoRejectBlocked(enabled: Boolean) = preferencesRepository.updateAutoRejectBlocked(enabled)
    suspend fun updateAutoSpeaker(enabled: Boolean) = preferencesRepository.updateAutoSpeaker(enabled)
    suspend fun updateQuickReplies(replies: List<String>) = preferencesRepository.updateQuickReplies(replies)
}
