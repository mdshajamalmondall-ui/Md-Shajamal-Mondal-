package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CallerCategory(val labelBn: String, val labelEn: String) {
    PERSONAL("ব্যক্তিগত", "Personal"),
    BUSINESS("ব্যবসা / অফিস", "Business"),
    DELIVERY("ডেলিভারি / কুরিয়ার", "Delivery"),
    SPAM("স্প্যাম কল", "Spam"),
    SCAM("প্রতারণা / স্ক্যাম", "Scam");

    companion object {
        fun fromRaw(raw: String?): CallerCategory {
            return entries.firstOrNull { it.name == raw } ?: PERSONAL
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserCloudProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val callerIdEnabled: Boolean = true,
    val communityShareEnabled: Boolean = false,
    val sharedContactCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallerDirectoryEntry(
    @DocumentId val id: String = "",
    val phoneKey: String = "",
    val displayName: String = "",
    val carrierLabel: String = "",
    val category: String = CallerCategory.PERSONAL.name,
    val spamReports: Long = 0L,
    val reportCount: Long = 1L,
    val contributedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCategory(): CallerCategory = CallerCategory.fromRaw(category)
    fun isLikelySpam(): Boolean = spamReports >= 1L || resolvedCategory() == CallerCategory.SPAM || resolvedCategory() == CallerCategory.SCAM
}

enum class CallerIdSource {
    LOCAL_CONTACT,
    COMMUNITY_CLOUD,
    SERVICE_DIRECTORY,
    CARRIER_ONLY
}

data class CallerIdLookupResult(
    val phoneKey: String,
    val displayName: String?,
    val carrierOrRegion: String,
    val category: CallerCategory = CallerCategory.PERSONAL,
    val spamReports: Long = 0L,
    val reportCount: Long = 0L,
    val source: CallerIdSource = CallerIdSource.CARRIER_ONLY
) {
    val isSpam: Boolean
        get() = spamReports >= 1L || category == CallerCategory.SPAM || category == CallerCategory.SCAM
}

/**
 * Normalizes any phone number into a canonical digits-only Firestore document key (4..15 digits).
 * Bangladesh numbers like 01712345678, +8801712345678, 8801712345678 all normalize to 8801712345678.
 */
fun toCanonicalPhoneKey(rawNumber: String): String {
    val digits = rawNumber.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val normalized = when {
        digits.startsWith("00880") && digits.length >= 15 -> digits.drop(2)
        digits.startsWith("8801") && digits.length == 13 -> digits
        digits.startsWith("01") && digits.length == 11 -> "88$digits"
        digits.startsWith("1") && digits.length == 10 && digits[1] in '3'..'9' -> "880$digits"
        digits.startsWith("00") && digits.length > 6 -> digits.drop(2)
        else -> digits
    }
    return normalized.take(15)
}
