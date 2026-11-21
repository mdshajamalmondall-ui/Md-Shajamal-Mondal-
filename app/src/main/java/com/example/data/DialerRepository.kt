package com.example.data

import com.example.data.local.AppCallRecordEntity
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallNoteEntity
import com.example.data.local.NovaCallDao
import com.example.data.local.PreferencesRepository
import com.example.data.local.SpeedDialEntity
import com.example.data.model.AppFontScale
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemePalette
import com.example.data.model.BackgroundDoodleStyle
import com.example.data.model.BrilliantLaunchMode
import com.example.data.model.CallRoute
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.DisplayThemeMode
import com.example.data.model.FavoriteGroup
import com.example.data.model.IncomingCallDisplayStyle
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.SlideToAnswerMode
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

    suspend fun clearAllLocalUserData() {
        dao.clearAppCallRecords()
        dao.clearCallNotes()
        dao.clearBlockedNumbers()
        dao.clearSpeedDials()
    }

    suspend fun getSpeedDialsSnapshot(): List<SpeedDialEntity> = dao.getAllSpeedDialsOnce()
    suspend fun getBlockedNumbersSnapshot(): List<BlockedNumberEntity> = dao.getAllBlockedNumbersOnce()
    suspend fun getCallNotesSnapshot(): List<CallNoteEntity> = dao.getAllCallNotesOnce()
    suspend fun getAppCallRecordsSnapshot(): List<AppCallRecordEntity> = dao.getAllAppCallRecordsOnce()

    suspend fun restoreBackupEntities(
        speedDials: List<SpeedDialEntity>,
        blockedNumbers: List<BlockedNumberEntity>,
        callNotes: List<CallNoteEntity>,
        callRecords: List<AppCallRecordEntity>
    ) {
        speedDials.forEach { dao.upsertSpeedDial(it) }
        blockedNumbers.forEach { dao.upsertBlockedNumber(it) }
        callNotes.forEach { dao.upsertCallNote(it) }
        callRecords.forEach { dao.insertAppCallRecord(it.copy(id = 0)) }
    }

    suspend fun updateBrilliantPackage(pkg: String) = preferencesRepository.updateBrilliantPackage(pkg)
    suspend fun updateBrilliantLaunchMode(mode: BrilliantLaunchMode) = preferencesRepository.updateBrilliantLaunchMode(mode)
    suspend fun updateBrilliantAutoFormatBd(enabled: Boolean) = preferencesRepository.updateBrilliantAutoFormatBd(enabled)
    suspend fun updateBrilliantAutoCopy(enabled: Boolean) = preferencesRepository.updateBrilliantAutoCopy(enabled)
    suspend fun updateBrilliantSmartPrefixes(prefixes: String) = preferencesRepository.updateBrilliantSmartPrefixes(prefixes)
    suspend fun updateBrilliantExternalHandoff(enabled: Boolean) = preferencesRepository.updateBrilliantExternalHandoff(enabled)
    suspend fun updateBrilliantAccount(name: String, number: String) = preferencesRepository.updateBrilliantAccount(name, number)
    suspend fun updateBrilliantLinkedAccountId(accountId: String) = preferencesRepository.updateBrilliantLinkedAccountId(accountId)
    suspend fun updateDefaultRoute(route: DefaultRoutePreference) = preferencesRepository.updateDefaultRoute(route)
    suspend fun updateSimAlias(slotIndex: Int, alias: String) = preferencesRepository.updateSimAlias(slotIndex, alias)
    suspend fun updateDtmfSound(enabled: Boolean) = preferencesRepository.updateDtmfSound(enabled)
    suspend fun updateKeypadSoundStyle(style: KeypadSoundStyle) = preferencesRepository.updateKeypadSoundStyle(style)
    suspend fun updateKeypadSoundVolume(volume: Float) = preferencesRepository.updateKeypadSoundVolume(volume)
    suspend fun updateHaptic(enabled: Boolean) = preferencesRepository.updateHaptic(enabled)
    suspend fun updateDarkTheme(enabled: Boolean) = preferencesRepository.updateDarkTheme(enabled)
    suspend fun updateThemePalette(palette: AppThemePalette) = preferencesRepository.updateThemePalette(palette)
    suspend fun updateBackgroundDoodle(style: BackgroundDoodleStyle) = preferencesRepository.updateBackgroundDoodle(style)
    suspend fun updateLanguage(language: AppLanguage) = preferencesRepository.updateLanguage(language)
    suspend fun updateNotifyIncoming(enabled: Boolean) = preferencesRepository.updateNotifyIncoming(enabled)
    suspend fun updateNotifyMissed(enabled: Boolean) = preferencesRepository.updateNotifyMissed(enabled)
    suspend fun updateNotifyOngoing(enabled: Boolean) = preferencesRepository.updateNotifyOngoing(enabled)
    suspend fun updateVibrateOnRing(enabled: Boolean) = preferencesRepository.updateVibrateOnRing(enabled)
    suspend fun updateAutoRejectBlocked(enabled: Boolean) = preferencesRepository.updateAutoRejectBlocked(enabled)
    suspend fun updateAutoSpeaker(enabled: Boolean) = preferencesRepository.updateAutoSpeaker(enabled)
    suspend fun updateQuickReplies(replies: List<String>) = preferencesRepository.updateQuickReplies(replies)
    suspend fun updateLiquidGlass(enabled: Boolean) = preferencesRepository.updateLiquidGlass(enabled)
    suspend fun updateDisplayThemeMode(mode: DisplayThemeMode) = preferencesRepository.updateDisplayThemeMode(mode)
    suspend fun updateFontScale(scale: AppFontScale) = preferencesRepository.updateFontScale(scale)
    suspend fun updateIncomingCallDisplayStyle(style: IncomingCallDisplayStyle) = preferencesRepository.updateIncomingCallDisplayStyle(style)
    suspend fun updateSlideToAnswerMode(mode: SlideToAnswerMode) = preferencesRepository.updateSlideToAnswerMode(mode)
    suspend fun updateFavoriteNumbers(numbers: Set<String>) = preferencesRepository.updateFavoriteNumbers(numbers)
    suspend fun updateFavoriteGroups(groups: Map<String, FavoriteGroup>) = preferencesRepository.updateFavoriteGroups(groups)
    suspend fun updateCallReminderTimestamps(reminders: Map<String, Long>) = preferencesRepository.updateCallReminderTimestamps(reminders)
    suspend fun updateDndConfig(enabled: Boolean, startMinutes: Int, endMinutes: Int, allowFavorites: Boolean) =
        preferencesRepository.updateDndConfig(enabled, startMinutes, endMinutes, allowFavorites)
    suspend fun updateSpamProtection(enabled: Boolean) = preferencesRepository.updateSpamProtection(enabled)
    suspend fun updateAutoCallBackup(enabled: Boolean) = preferencesRepository.updateAutoCallBackup(enabled)
    suspend fun updateLastBackupTimestamp(timestamp: Long) = preferencesRepository.updateLastBackupTimestamp(timestamp)
    suspend fun updateSimBalanceUssd(slotIndex: Int, ussdCode: String) = preferencesRepository.updateSimBalanceUssd(slotIndex, ussdCode)
    suspend fun updateCustomRingtone(uri: String, title: String) = preferencesRepository.updateCustomRingtone(uri, title)
    suspend fun updateIccSipConfig(
        enabled: Boolean,
        server: String,
        port: Int,
        username: String,
        password: String,
        ispCustomerId: String
    ) = preferencesRepository.updateIccSipConfig(enabled, server, port, username, password, ispCustomerId)
}
