package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.DialerRepository
import com.example.data.local.AppCallRecordEntity
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallNoteEntity
import com.example.data.local.SpeedDialEntity
import com.example.data.model.CallLogItem
import com.example.data.model.FavoriteGroup
import com.example.data.model.UserPreferences
import com.example.data.model.normalizePhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupRestoreSummary(
    val restoredCalls: Int,
    val restoredNotes: Int,
    val restoredBlocked: Int,
    val restoredSpeedDials: Int,
    val restoredFavorites: Int
)

object CallBackupManager {

    private const val BACKUP_VERSION = 2

    suspend fun buildBackupJson(
        repository: DialerRepository,
        systemCallLogs: List<CallLogItem>,
        preferences: UserPreferences
    ): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Shajamal PRO DIALER")
        root.put("version", BACKUP_VERSION)
        root.put("createdAt", System.currentTimeMillis())

        val speedDials = repository.getSpeedDialsSnapshot()
        val blocked = repository.getBlockedNumbersSnapshot()
        val notes = repository.getCallNotesSnapshot()
        val appRecords = repository.getAppCallRecordsSnapshot()

        // Merge local records and recent system logs cleanly into callRecords array (avoid duplicate timestamps)
        val callRecordsArr = JSONArray()
        val seenKeys = HashSet<String>()
        appRecords.forEach { rec ->
            val key = "${rec.normalizedNumber}_${rec.timestamp / 1000L}"
            if (seenKeys.add(key)) {
                callRecordsArr.put(
                    JSONObject().apply {
                        put("number", rec.number)
                        put("normalizedNumber", rec.normalizedNumber)
                        put("contactName", rec.contactName ?: "")
                        put("route", rec.route)
                        put("timestamp", rec.timestamp)
                    }
                )
            }
        }
        systemCallLogs.take(200).forEach { log ->
            val key = "${log.normalizedNumber}_${log.timestamp / 1000L}"
            if (seenKeys.add(key)) {
                callRecordsArr.put(
                    JSONObject().apply {
                        put("number", log.number)
                        put("normalizedNumber", log.normalizedNumber)
                        put("contactName", log.contactName ?: "")
                        put("route", log.route.name)
                        put("timestamp", log.timestamp)
                    }
                )
            }
        }
        root.put("callRecords", callRecordsArr)

        val notesArr = JSONArray()
        notes.forEach { n ->
            notesArr.put(
                JSONObject().apply {
                    put("normalizedNumber", n.normalizedNumber)
                    put("displayNumber", n.displayNumber)
                    put("contactName", n.contactName)
                    put("note", n.note)
                    put("hasReminder", n.hasReminder)
                    put("updatedAt", n.updatedAt)
                }
            )
        }
        root.put("callNotes", notesArr)

        val blockedArr = JSONArray()
        blocked.forEach { b ->
            blockedArr.put(
                JSONObject().apply {
                    put("normalizedNumber", b.normalizedNumber)
                    put("displayNumber", b.displayNumber)
                    put("label", b.label)
                    put("reason", b.reason)
                    put("blockedAt", b.blockedAt)
                }
            )
        }
        root.put("blockedNumbers", blockedArr)

        val speedArr = JSONArray()
        speedDials.forEach { sd ->
            speedArr.put(
                JSONObject().apply {
                    put("digit", sd.digit)
                    put("name", sd.name)
                    put("phoneNumber", sd.phoneNumber)
                    put("normalizedNumber", sd.normalizedNumber)
                    put("preferredRoute", sd.preferredRoute)
                }
            )
        }
        root.put("speedDials", speedArr)

        val favArr = JSONArray()
        preferences.favoriteNumbers.forEach { favArr.put(it) }
        root.put("favoriteNumbers", favArr)

        val favGroupsObj = JSONObject()
        preferences.favoriteGroups.forEach { (k, v) -> favGroupsObj.put(k, v.name) }
        root.put("favoriteGroups", favGroupsObj)

        val quickRepliesArr = JSONArray()
        preferences.quickReplies.forEach { quickRepliesArr.put(it) }
        root.put("quickReplies", quickRepliesArr)

        root.toString(2)
    }

    suspend fun writeBackupToUri(context: Context, uri: Uri, jsonContent: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonContent.toByteArray(Charsets.UTF_8))
                    stream.flush()
                } ?: error("Cannot open output stream for backup file")
                Unit
            }
        }

    suspend fun writeAutoLocalSnapshot(context: Context, jsonContent: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                val target = File(dir, "shajamal_auto_backup.json")
                target.writeText(jsonContent, Charsets.UTF_8)
                target
            }
        }

    suspend fun restoreBackupFromUri(
        context: Context,
        uri: Uri,
        repository: DialerRepository,
        currentPreferences: UserPreferences
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Cannot read backup file")
            restoreBackupFromJson(raw, repository, currentPreferences)
        }
    }

    suspend fun restoreLatestLocalSnapshot(
        context: Context,
        repository: DialerRepository,
        currentPreferences: UserPreferences
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.filesDir, "backups/shajamal_auto_backup.json")
            if (!file.exists()) error("No local auto backup snapshot found")
            restoreBackupFromJson(file.readText(Charsets.UTF_8), repository, currentPreferences)
        }
    }

    private suspend fun restoreBackupFromJson(
        raw: String,
        repository: DialerRepository,
        currentPreferences: UserPreferences
    ): BackupRestoreSummary {
        val root = JSONObject(raw)

        val recordsList = ArrayList<AppCallRecordEntity>()
        val recArr = root.optJSONArray("callRecords")
        if (recArr != null) {
            for (i in 0 until recArr.length()) {
                val obj = recArr.optJSONObject(i) ?: continue
                val num = obj.optString("number").trim()
                val norm = obj.optString("normalizedNumber").ifBlank { normalizePhoneNumber(num) }
                if (norm.isNotBlank()) {
                    recordsList.add(
                        AppCallRecordEntity(
                            number = num.ifBlank { norm },
                            normalizedNumber = norm,
                            contactName = obj.optString("contactName").takeIf { it.isNotBlank() },
                            route = obj.optString("route", "SIM_1"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val notesList = ArrayList<CallNoteEntity>()
        val notesArr = root.optJSONArray("callNotes")
        if (notesArr != null) {
            for (i in 0 until notesArr.length()) {
                val obj = notesArr.optJSONObject(i) ?: continue
                val norm = obj.optString("normalizedNumber").trim()
                val note = obj.optString("note").trim()
                if (norm.isNotBlank() && note.isNotBlank()) {
                    notesList.add(
                        CallNoteEntity(
                            normalizedNumber = norm,
                            displayNumber = obj.optString("displayNumber", norm),
                            contactName = obj.optString("contactName", norm),
                            note = note,
                            hasReminder = obj.optBoolean("hasReminder", false),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val blockedList = ArrayList<BlockedNumberEntity>()
        val blockedArr = root.optJSONArray("blockedNumbers")
        if (blockedArr != null) {
            for (i in 0 until blockedArr.length()) {
                val obj = blockedArr.optJSONObject(i) ?: continue
                val norm = obj.optString("normalizedNumber").trim()
                if (norm.isNotBlank()) {
                    blockedList.add(
                        BlockedNumberEntity(
                            normalizedNumber = norm,
                            displayNumber = obj.optString("displayNumber", norm),
                            label = obj.optString("label", norm),
                            reason = obj.optString("reason", "Spam"),
                            blockedAt = obj.optLong("blockedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val speedList = ArrayList<SpeedDialEntity>()
        val speedArr = root.optJSONArray("speedDials")
        if (speedArr != null) {
            for (i in 0 until speedArr.length()) {
                val obj = speedArr.optJSONObject(i) ?: continue
                val digit = obj.optInt("digit", -1)
                val num = obj.optString("phoneNumber").trim()
                val norm = obj.optString("normalizedNumber").ifBlank { normalizePhoneNumber(num) }
                if (digit in 1..9 && norm.isNotBlank()) {
                    speedList.add(
                        SpeedDialEntity(
                            digit = digit,
                            name = obj.optString("name", num),
                            phoneNumber = num.ifBlank { norm },
                            normalizedNumber = norm,
                            preferredRoute = obj.optString("preferredRoute", "SIM_1")
                        )
                    )
                }
            }
        }

        repository.restoreBackupEntities(
            speedDials = speedList,
            blockedNumbers = blockedList,
            callNotes = notesList,
            callRecords = recordsList
        )

        val favSet = currentPreferences.favoriteNumbers.toMutableSet()
        val favArr = root.optJSONArray("favoriteNumbers")
        var restoredFavs = 0
        if (favArr != null) {
            for (i in 0 until favArr.length()) {
                val num = favArr.optString(i).trim()
                if (num.isNotBlank()) {
                    favSet.add(num)
                    restoredFavs++
                }
            }
        }
        repository.updateFavoriteNumbers(favSet)

        val favGroups = currentPreferences.favoriteGroups.toMutableMap()
        val groupsObj = root.optJSONObject("favoriteGroups")
        if (groupsObj != null) {
            val keys = groupsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val grp = runCatching { FavoriteGroup.valueOf(groupsObj.optString(k)) }.getOrNull()
                if (grp != null && grp != FavoriteGroup.ALL) {
                    favGroups[k] = grp
                }
            }
            repository.updateFavoriteGroups(favGroups)
        }

        val quickArr = root.optJSONArray("quickReplies")
        if (quickArr != null && quickArr.length() > 0) {
            val list = (0 until quickArr.length())
                .map { quickArr.optString(it).trim() }
                .filter { it.isNotEmpty() }
            if (list.isNotEmpty()) {
                repository.updateQuickReplies(list)
            }
        }

        return BackupRestoreSummary(
            restoredCalls = recordsList.size,
            restoredNotes = notesList.size,
            restoredBlocked = blockedList.size,
            restoredSpeedDials = speedList.size,
            restoredFavorites = restoredFavs
        )
    }
}
