package com.example.data.model

import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Immutable

enum class CallRoute {
    SIM_1,
    SIM_2,
    BRILLIANT,
    ICC_SIP
}

enum class DefaultRoutePreference {
    ASK,
    SIM_1,
    SIM_2,
    BRILLIANT
}

enum class BrilliantLaunchMode {
    SMART_AUTO,
    DIRECT_CALL_INTENT,
    DIAL_INTENT,
    SIP_URI,
    LAUNCH_AND_COPY
}

enum class KeypadSoundStyle {
    DTMF_CLASSIC,
    STUDIO_CHIME,
    SOFT_CLICK
}

enum class CallFilterType {
    ALL,
    MISSED,
    SIP_VOIP,
    CELLULAR,
    BRILLIANT,
    OUTGOING,
    INCOMING
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED
}

enum class AppLanguage {
    BENGALI,
    ENGLISH
}

enum class AppThemePalette {
    ROYAL_SAPPHIRE,
    EMERALD_FOREST,
    ROYAL_AMETHYST,
    OCEAN_TEAL,
    CRIMSON_VELVET,
    SUNSET_AMBER,
    MIDNIGHT_ROSE,
    CYBER_INDIGO,
    PEARL_WHITE,
    MINT_BREEZE;

    val isLightPalette: Boolean
        get() = this == PEARL_WHITE || this == MINT_BREEZE
}

enum class BackgroundDoodleStyle {
    NONE,
    PHONE_DOODLE,
    COSMIC_STARS,
    FLORAL_LEAVES,
    GEOMETRIC_WAVES,
    HEARTS_SPARKLE
}

enum class DisplayThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppFontScale(val multiplier: Float) {
    SMALL(0.90f),
    MEDIUM(1.0f),
    LARGE(1.13f)
}

enum class IncomingCallDisplayStyle {
    FULL_SCREEN,
    BANNER
}

enum class SlideToAnswerMode {
    LOCKED_ONLY,
    ALWAYS,
    NEVER
}

data class CallingAccountOption(
    val id: String,
    val label: String,
    val packageName: String,
    val isVoipOrBrilliant: Boolean
)

@Immutable
data class SimCardInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val number: String,
    val isDetected: Boolean,
    val phoneAccountHandle: PhoneAccountHandle? = null
)

data class BrilliantBridgeStatus(
    val isInstalled: Boolean = false,
    val packageName: String = DEFAULT_BRILLIANT_PACKAGE,
    val appLabel: String = "Brilliant Connect",
    val hasTelecomVoipAccount: Boolean = false,
    val telecomAccountLabel: String? = null,
    val availableCallingAccounts: List<CallingAccountOption> = emptyList()
) {
    companion object {
        const val DEFAULT_BRILLIANT_PACKAGE = "com.brilliant.connect.com.bd"
        val KNOWN_PACKAGES = listOf(
            "com.brilliant.connect.com.bd",
            "com.brilliant.connect"
        )
    }
}

@Immutable
data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val photoUri: String? = null,
    val isStarred: Boolean = false
)

@Immutable
data class CallLogItem(
    val id: String,
    val number: String,
    val normalizedNumber: String,
    val contactName: String?,
    val photoUri: String? = null,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSeconds: Long,
    val route: CallRoute,
    val isLocalNovaRecord: Boolean = false,
    val callCount: Int = 1,
    val groupedIds: List<String> = emptyList()
)

@Immutable
data class UserPreferences(
    val brilliantPackageName: String = BrilliantBridgeStatus.DEFAULT_BRILLIANT_PACKAGE,
    val brilliantLaunchMode: BrilliantLaunchMode = BrilliantLaunchMode.SMART_AUTO,
    val brilliantAutoFormatBd: Boolean = true,
    val brilliantAutoCopyClipboard: Boolean = true,
    val brilliantSmartPrefixes: String = "+,00",
    val brilliantExternalHandoff: Boolean = false,
    val brilliantAccountName: String = "",
    val brilliantAccountNumber: String = "",
    val brilliantLinkedAccountId: String = "",
    val defaultRoute: DefaultRoutePreference = DefaultRoutePreference.ASK,
    val sim1Alias: String = "",
    val sim2Alias: String = "",
    val dtmfSoundEnabled: Boolean = true,
    val keypadSoundStyle: KeypadSoundStyle = KeypadSoundStyle.DTMF_CLASSIC,
    val keypadSoundVolume: Float = 0.9f,
    val hapticEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val themePalette: AppThemePalette = AppThemePalette.ROYAL_SAPPHIRE,
    val backgroundDoodle: BackgroundDoodleStyle = BackgroundDoodleStyle.PHONE_DOODLE,
    val language: AppLanguage = AppLanguage.BENGALI,
    val notifyIncomingCalls: Boolean = true,
    val notifyMissedCalls: Boolean = true,
    val notifyOngoingCalls: Boolean = true,
    val vibrateOnRing: Boolean = true,
    val autoRejectBlocked: Boolean = true,
    val autoSpeakerOnCall: Boolean = false,
    val quickReplies: List<String> = DEFAULT_QUICK_REPLIES,
    val liquidGlassEnabled: Boolean = true,
    val displayThemeMode: DisplayThemeMode = DisplayThemeMode.DARK,
    val fontScale: AppFontScale = AppFontScale.MEDIUM,
    val incomingCallDisplayStyle: IncomingCallDisplayStyle = IncomingCallDisplayStyle.FULL_SCREEN,
    val slideToAnswerMode: SlideToAnswerMode = SlideToAnswerMode.LOCKED_ONLY,
    val favoriteNumbers: Set<String> = emptySet(),
    val sim1BalanceUssd: String = "",
    val sim2BalanceUssd: String = "",
    val customRingtoneUri: String = "",
    val customRingtoneTitle: String = "",
    val iccSipEnabled: Boolean = false,
    val iccSipServer: String = "sip.icc.com.bd",
    val iccSipPort: Int = 5060,
    val iccSipUsername: String = "",
    val iccSipPassword: String = "",
    val iccIspCustomerId: String = ""
) {
    companion object {
        val DEFAULT_QUICK_REPLIES = listOf(
            "আমি এখন ব্যস্ত আছি, একটু পর কল করছি।",
            "মিটিংয়ে আছি, জরুরি হলে মেসেজ দিন।",
            "রাস্তায় আছি, পৌঁছে ফোন দিচ্ছি।",
            "Can't talk right now, I'll call you back soon."
        )
    }
}

sealed interface CallDispatchResult {
    data class Dispatched(val route: CallRoute, val formattedNumber: String, val messageBn: String, val messageEn: String) : CallDispatchResult
    data class BrilliantNotInstalled(val packageName: String) : CallDispatchResult
    data class InvalidNumber(val reasonBn: String, val reasonEn: String) : CallDispatchResult
    data class Error(val messageBn: String, val messageEn: String) : CallDispatchResult
}

fun normalizePhoneNumber(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val hasLeadingPlus = trimmed.startsWith("+")
    val digitsOnly = trimmed.filter { it.isDigit() }
    return if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
}

fun formatBangladeshNumberForBrilliant(raw: String, autoFormatBd: Boolean): String {
    val normalized = normalizePhoneNumber(raw)
    if (!autoFormatBd || normalized.isEmpty()) return normalized
    return when {
        normalized.startsWith("01") && normalized.length == 11 -> "+88$normalized"
        normalized.startsWith("8801") && normalized.length == 13 -> "+$normalized"
        else -> normalized
    }
}

fun formatDisplayNumber(raw: String): String {
    val clean = normalizePhoneNumber(raw)
    return when {
        clean.startsWith("+8801") && clean.length == 14 ->
            "${clean.substring(0, 4)} ${clean.substring(4, 8)}-${clean.substring(8)}"
        clean.startsWith("01") && clean.length == 11 ->
            "${clean.substring(0, 5)}-${clean.substring(5)}"
        else -> raw
    }
}
