package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.BrilliantBridgeStatus
import com.example.data.model.BrilliantLaunchMode
import com.example.data.model.DefaultRoutePreference
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaDataStore: DataStore<Preferences> by preferencesDataStore(name = "novacall_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val BRILLIANT_PACKAGE = stringPreferencesKey("brilliant_package")
        val BRILLIANT_MODE = stringPreferencesKey("brilliant_mode")
        val BRILLIANT_AUTO_BD = booleanPreferencesKey("brilliant_auto_bd")
        val BRILLIANT_AUTO_COPY = booleanPreferencesKey("brilliant_auto_copy")
        val BRILLIANT_SMART_PREFIXES = stringPreferencesKey("brilliant_smart_prefixes")
        val DEFAULT_ROUTE = stringPreferencesKey("default_route")
        val SIM1_ALIAS = stringPreferencesKey("sim1_alias")
        val SIM2_ALIAS = stringPreferencesKey("sim2_alias")
        val DTMF_SOUND = booleanPreferencesKey("dtmf_sound")
        val HAPTIC = booleanPreferencesKey("haptic")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val QUICK_REPLIES = stringPreferencesKey("quick_replies_v1")
    }

    val preferencesFlow: Flow<UserPreferences> = context.novaDataStore.data.map { prefs ->
        val rawReplies = prefs[Keys.QUICK_REPLIES]
        val replies = if (rawReplies.isNullOrBlank()) {
            UserPreferences.DEFAULT_QUICK_REPLIES
        } else {
            rawReplies.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                .ifEmpty { UserPreferences.DEFAULT_QUICK_REPLIES }
        }
        UserPreferences(
            brilliantPackageName = prefs[Keys.BRILLIANT_PACKAGE]
                ?.takeIf { it.isNotBlank() }
                ?: BrilliantBridgeStatus.DEFAULT_BRILLIANT_PACKAGE,
            brilliantLaunchMode = prefs[Keys.BRILLIANT_MODE]
                ?.let { runCatching { BrilliantLaunchMode.valueOf(it) }.getOrNull() }
                ?: BrilliantLaunchMode.SMART_AUTO,
            brilliantAutoFormatBd = prefs[Keys.BRILLIANT_AUTO_BD] ?: true,
            brilliantAutoCopyClipboard = prefs[Keys.BRILLIANT_AUTO_COPY] ?: true,
            brilliantSmartPrefixes = prefs[Keys.BRILLIANT_SMART_PREFIXES] ?: "+,00",
            defaultRoute = prefs[Keys.DEFAULT_ROUTE]
                ?.let { runCatching { DefaultRoutePreference.valueOf(it) }.getOrNull() }
                ?: DefaultRoutePreference.ASK,
            sim1Alias = prefs[Keys.SIM1_ALIAS] ?: "",
            sim2Alias = prefs[Keys.SIM2_ALIAS] ?: "",
            dtmfSoundEnabled = prefs[Keys.DTMF_SOUND] ?: true,
            hapticEnabled = prefs[Keys.HAPTIC] ?: true,
            darkThemeEnabled = prefs[Keys.DARK_THEME] ?: true,
            language = prefs[Keys.LANGUAGE]
                ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.BENGALI,
            quickReplies = replies
        )
    }

    suspend fun updateBrilliantPackage(packageName: String) {
        context.novaDataStore.edit { it[Keys.BRILLIANT_PACKAGE] = packageName.trim() }
    }

    suspend fun updateBrilliantLaunchMode(mode: BrilliantLaunchMode) {
        context.novaDataStore.edit { it[Keys.BRILLIANT_MODE] = mode.name }
    }

    suspend fun updateBrilliantAutoFormatBd(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.BRILLIANT_AUTO_BD] = enabled }
    }

    suspend fun updateBrilliantAutoCopy(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.BRILLIANT_AUTO_COPY] = enabled }
    }

    suspend fun updateBrilliantSmartPrefixes(prefixes: String) {
        context.novaDataStore.edit { it[Keys.BRILLIANT_SMART_PREFIXES] = prefixes.trim() }
    }

    suspend fun updateDefaultRoute(route: DefaultRoutePreference) {
        context.novaDataStore.edit { it[Keys.DEFAULT_ROUTE] = route.name }
    }

    suspend fun updateSimAlias(slotIndex: Int, alias: String) {
        context.novaDataStore.edit {
            if (slotIndex == 0) it[Keys.SIM1_ALIAS] = alias.trim()
            else it[Keys.SIM2_ALIAS] = alias.trim()
        }
    }

    suspend fun updateDtmfSound(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.DTMF_SOUND] = enabled }
    }

    suspend fun updateHaptic(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.HAPTIC] = enabled }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.novaDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateQuickReplies(replies: List<String>) {
        context.novaDataStore.edit {
            it[Keys.QUICK_REPLIES] = replies.map { r -> r.trim() }.filter { r -> r.isNotEmpty() }.joinToString("\n")
        }
    }
}
