package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.model.DeviceContact
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CallerIdRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Always initialize with the provisioned named database ID from firebase_applet_config.xml
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentUidOrNull(): String? = auth.currentUser?.uid

    fun observeUserProfile(userId: String): Flow<UserCloudProfile?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserCloudProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyContributedTags(userId: String): Flow<List<CallerDirectoryEntry>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "caller_directory"
        emitAll(
            db.collection(path)
                .whereEqualTo("contributedByUid", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CallerDirectoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(displayNameHint: String): Result<UserCloudProfile> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(
                    UserCloudProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserCloudProfile(id = uid, userId = uid, displayName = displayNameHint.ifBlank { "Shajamal User" }.take(80))
                Result.success(parsed)
            } else {
                val safeName = displayNameHint.trim().ifBlank { "Shajamal User" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "callerIdEnabled" to true,
                    "communityShareEnabled" to false,
                    "sharedContactCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(
                    UserCloudProfile(
                        id = uid,
                        userId = uid,
                        displayName = safeName,
                        callerIdEnabled = true,
                        communityShareEnabled = false,
                        sharedContactCount = 0L
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCloudCallerIdSettings(
        callerIdEnabled: Boolean? = null,
        communityShareEnabled: Boolean? = null,
        sharedContactCount: Long? = null
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            callerIdEnabled?.let { updates["callerIdEnabled"] = it }
            communityShareEnabled?.let { updates["communityShareEnabled"] = it }
            sharedContactCount?.let { updates["sharedContactCount"] = it.coerceIn(0L, 5000L) }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Looks up a phone number in the NovaCall Cloud Community Directory (exact document ID get).
     * Falls back seamlessly to offline carrier / BD service intelligence when not found or signed out.
     */
    suspend fun lookupNumber(rawNumber: String): CallerIdLookupResult? {
        val phoneKey = toCanonicalPhoneKey(rawNumber)
        if (phoneKey.length < 4 || phoneKey.length > 15) {
            return CallerIdIntelligence.detectOffline(rawNumber)
        }
        val offline = CallerIdIntelligence.detectOffline(rawNumber)
        if (offline?.source == CallerIdSource.SERVICE_DIRECTORY) {
            return offline
        }
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return offline
        }
        val path = "caller_directory/$phoneKey"
        return try {
            val snap = db.collection("caller_directory").document(phoneKey).get().await()
            if (snap.exists()) {
                val entry = snap.toObject(
                    CallerDirectoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (entry != null && entry.displayName.isNotBlank()) {
                    CallerIdLookupResult(
                        phoneKey = entry.phoneKey.ifBlank { phoneKey },
                        displayName = entry.displayName,
                        carrierOrRegion = entry.carrierLabel.ifBlank {
                            offline?.carrierOrRegion ?: CallerIdIntelligence.detectCarrierOrRegion(phoneKey)
                        },
                        category = entry.resolvedCategory(),
                        spamReports = entry.spamReports,
                        reportCount = entry.reportCount,
                        source = CallerIdSource.COMMUNITY_CLOUD
                    )
                } else {
                    offline
                }
            } else {
                offline
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            offline
        }
    }

    /**
     * Contributes or updates a Caller ID name/spam tag in `/caller_directory/{phoneKey}`.
     */
    suspend fun publishCallerTag(
        rawNumber: String,
        displayName: String,
        category: CallerCategory,
        markAsSpam: Boolean = category == CallerCategory.SPAM || category == CallerCategory.SCAM
    ): Result<CallerIdLookupResult> {
        val phoneKey = toCanonicalPhoneKey(rawNumber)
        if (phoneKey.length < 4 || phoneKey.length > 15) {
            return Result.failure(IllegalArgumentException("Invalid phone number"))
        }
        val cleanName = displayName.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }

        val path = "caller_directory/$phoneKey"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("caller_directory").document(phoneKey)
            val carrier = CallerIdIntelligence.detectCarrierOrRegion(phoneKey).take(60)
            val existingSnap = docRef.get().await()

            if (!existingSnap.exists()) {
                val spamCount = if (markAsSpam) 1L else 0L
                val payload = mapOf(
                    "phoneKey" to phoneKey,
                    "displayName" to cleanName,
                    "carrierLabel" to carrier,
                    "category" to category.name,
                    "spamReports" to spamCount,
                    "reportCount" to 1L,
                    "contributedByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(
                    CallerIdLookupResult(
                        phoneKey = phoneKey,
                        displayName = cleanName,
                        carrierOrRegion = carrier,
                        category = category,
                        spamReports = spamCount,
                        reportCount = 1L,
                        source = CallerIdSource.COMMUNITY_CLOUD
                    )
                )
            } else {
                val existingEntry = existingSnap.toObject(
                    CallerDirectoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val nextReportCount = ((existingEntry?.reportCount ?: 1L) + 1L).coerceAtMost(100_000L)
                val nextSpamReports = ((existingEntry?.spamReports ?: 0L) + if (markAsSpam) 1L else 0L).coerceAtMost(100_000L)
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "category" to category.name,
                    "spamReports" to nextSpamReports,
                    "reportCount" to nextReportCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (existingEntry?.contributedByUid == uid) {
                    updates["carrierLabel"] = carrier
                }
                docRef.update(updates).await()
                Result.success(
                    CallerIdLookupResult(
                        phoneKey = phoneKey,
                        displayName = cleanName,
                        carrierOrRegion = existingEntry?.carrierLabel?.ifBlank { carrier } ?: carrier,
                        category = category,
                        spamReports = nextSpamReports,
                        reportCount = nextReportCount,
                        source = CallerIdSource.COMMUNITY_CLOUD
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Explicit opt-in community sync: publishes the user's saved contact names (strictly when user
     * taps opt-in) so other NovaCall users can see who is calling from unknown numbers.
     */
    suspend fun syncOptInContacts(contacts: List<DeviceContact>, maxCount: Int = 80): Result<Int> {
        return try {
            val uid = requireUserId()
            var synced = 0
            val uniqueByKey = contacts
                .mapNotNull { contact ->
                    val key = toCanonicalPhoneKey(contact.phoneNumber)
                    val cleanName = contact.name.trim().take(80)
                    if (key.length in 7..15 && cleanName.isNotEmpty() && cleanName != contact.phoneNumber) {
                        key to cleanName
                    } else null
                }
                .distinctBy { it.first }
                .take(maxCount)

            for ((phoneKey, name) in uniqueByKey) {
                val docRef = db.collection("caller_directory").document(phoneKey)
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    val carrier = CallerIdIntelligence.detectCarrierOrRegion(phoneKey).take(60)
                    val payload = mapOf(
                        "phoneKey" to phoneKey,
                        "displayName" to name,
                        "carrierLabel" to carrier,
                        "category" to CallerCategory.PERSONAL.name,
                        "spamReports" to 0L,
                        "reportCount" to 1L,
                        "contributedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                    synced++
                }
            }

            updateCloudCallerIdSettings(
                communityShareEnabled = true,
                sharedContactCount = uniqueByKey.size.toLong()
            )
            Result.success(synced)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "caller_directory")
            Result.failure(e)
        }
    }
}
