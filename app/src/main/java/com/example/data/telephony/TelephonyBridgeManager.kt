package com.example.data.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.data.model.BrilliantBridgeStatus
import com.example.data.model.BrilliantLaunchMode
import com.example.data.model.CallDirection
import com.example.data.model.CallDispatchResult
import com.example.data.model.CallLogItem
import com.example.data.model.CallRoute
import com.example.data.model.DeviceContact
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.SimCardInfo
import com.example.data.model.UserPreferences
import com.example.data.model.formatBangladeshNumberForBrilliant
import com.example.data.model.normalizePhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TelephonyBridgeManager(private val context: Context) {

    private val soundEngine = DialpadSoundEngine()

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isDefaultDialer(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            }
        }
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecom?.defaultDialerPackage == context.packageName
    }

    fun createRequestDefaultDialerIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
        }
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }

    fun openAppNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun playDialpadSound(symbol: String, style: KeypadSoundStyle, volume: Float) {
        soundEngine.playKey(symbol, style, volume)
    }

    fun releaseToneGenerator() {
        soundEngine.release()
    }

    @SuppressLint("MissingPermission")
    suspend fun loadSimCards(prefs: UserPreferences): List<SimCardInfo> = withContext(Dispatchers.IO) {
        val defaultSim1 = SimCardInfo(
            slotIndex = 0,
            subscriptionId = -1,
            displayName = prefs.sim1Alias.ifBlank { "SIM 1" },
            carrierName = if (hasPermission(Manifest.permission.READ_PHONE_STATE)) "SIM Slot 1" else "SIM 1",
            number = "",
            isDetected = false
        )
        val defaultSim2 = SimCardInfo(
            slotIndex = 1,
            subscriptionId = -1,
            displayName = prefs.sim2Alias.ifBlank { "SIM 2" },
            carrierName = if (hasPermission(Manifest.permission.READ_PHONE_STATE)) "SIM Slot 2" else "SIM 2",
            number = "",
            isDetected = false
        )

        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            return@withContext listOf(defaultSim1, defaultSim2)
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

        val accountHandles: List<PhoneAccountHandle> = runCatching {
            telecomManager?.callCapablePhoneAccounts ?: emptyList()
        }.getOrDefault(emptyList())

        val subscriptions = runCatching {
            subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
        }.getOrDefault(emptyList())

        val slotMap = mutableMapOf<Int, SimCardInfo>()
        subscriptions.forEachIndexed { idx, sub ->
            val slot = sub.simSlotIndex.coerceIn(0, 1)
            val matchedHandle = accountHandles.firstOrNull { handle ->
                handle.id.contains(sub.subscriptionId.toString()) ||
                    (sub.iccId != null && handle.id.contains(sub.iccId))
            } ?: accountHandles.getOrNull(idx)

            val alias = if (slot == 0) prefs.sim1Alias else prefs.sim2Alias
            val carrier = sub.carrierName?.toString()?.takeIf { it.isNotBlank() }
                ?: sub.displayName?.toString()?.takeIf { it.isNotBlank() }
                ?: "SIM ${slot + 1}"

            @Suppress("DEPRECATION")
            val phoneNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { subscriptionManager?.getPhoneNumber(sub.subscriptionId).orEmpty() }.getOrDefault("")
            } else {
                runCatching { sub.number.orEmpty() }.getOrDefault("")
            }

            slotMap[slot] = SimCardInfo(
                slotIndex = slot,
                subscriptionId = sub.subscriptionId,
                displayName = alias.ifBlank { carrier },
                carrierName = carrier,
                number = phoneNumber,
                isDetected = true,
                phoneAccountHandle = matchedHandle
            )
        }

        if (slotMap.isEmpty() && accountHandles.isNotEmpty()) {
            accountHandles.take(2).forEachIndexed { idx, handle ->
                val account = runCatching { telecomManager?.getPhoneAccount(handle) }.getOrNull()
                val label = account?.label?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${idx + 1}"
                val alias = if (idx == 0) prefs.sim1Alias else prefs.sim2Alias
                slotMap[idx] = SimCardInfo(
                    slotIndex = idx,
                    subscriptionId = idx + 1,
                    displayName = alias.ifBlank { label },
                    carrierName = label,
                    number = "",
                    isDetected = true,
                    phoneAccountHandle = handle
                )
            }
        }

        listOf(
            slotMap[0] ?: defaultSim1,
            slotMap[1] ?: defaultSim2
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun inspectBrilliantBridge(preferredPackage: String): BrilliantBridgeStatus = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val candidates = (listOf(preferredPackage.trim()) + BrilliantBridgeStatus.KNOWN_PACKAGES)
            .filter { it.isNotBlank() }
            .distinct()

        var installedPkg: String? = null
        var appLabel = "Brilliant Connect"

        for (pkg in candidates) {
            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
            }.getOrNull()
            if (appInfo != null) {
                installedPkg = pkg
                appLabel = runCatching { pm.getApplicationLabel(appInfo).toString() }
                    .getOrDefault("Brilliant Connect")
                break
            }
        }

        var telecomLabel: String? = null
        if (hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val handles = runCatching { telecomManager?.callCapablePhoneAccounts ?: emptyList() }
                .getOrDefault(emptyList())
            val brilliantHandle = handles.firstOrNull { handle ->
                val pkgMatch = handle.componentName.packageName.contains("brilliant", ignoreCase = true) ||
                    (installedPkg != null && handle.componentName.packageName == installedPkg)
                val label = runCatching { telecomManager?.getPhoneAccount(handle)?.label?.toString() }.getOrNull()
                pkgMatch || (label?.contains("brilliant", ignoreCase = true) == true)
            }
            if (brilliantHandle != null) {
                telecomLabel = runCatching {
                    telecomManager?.getPhoneAccount(brilliantHandle)?.label?.toString()
                }.getOrNull() ?: "Brilliant VoIP"
            }
        }

        BrilliantBridgeStatus(
            isInstalled = installedPkg != null || telecomLabel != null,
            packageName = installedPkg ?: preferredPackage.ifBlank { BrilliantBridgeStatus.DEFAULT_BRILLIANT_PACKAGE },
            appLabel = appLabel,
            hasTelecomVoipAccount = telecomLabel != null,
            telecomAccountLabel = telecomLabel
        )
    }

    @SuppressLint("MissingPermission")
    fun placeSimCall(
        rawNumber: String,
        simInfo: SimCardInfo
    ): CallDispatchResult {
        val cleanNumber = normalizePhoneNumber(rawNumber)
        if (cleanNumber.length < 3) {
            return CallDispatchResult.InvalidNumber(
                reasonBn = "সঠিক ফোন নম্বর লিখুন",
                reasonEn = "Enter a valid phone number"
            )
        }
        val route = if (simInfo.slotIndex == 0) CallRoute.SIM_1 else CallRoute.SIM_2
        val telUri = Uri.fromParts("tel", cleanNumber, null)

        // Use TelecomManager.placeCall so when NovaCall is Default Dialer, the call stays 100% inside NovaCall's InCallActivity!
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                val placed = runCatching {
                    val extras = Bundle().apply {
                        simInfo.phoneAccountHandle?.let {
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
                        }
                    }
                    telecomManager.placeCall(telUri, extras)
                    true
                }.getOrDefault(false)

                if (placed) {
                    return CallDispatchResult.Dispatched(
                        route = route,
                        formattedNumber = cleanNumber,
                        messageBn = "${simInfo.displayName} দিয়ে কল যাচ্ছে...",
                        messageEn = "Calling via ${simInfo.displayName}..."
                    )
                }
            }

            val callIntent = Intent(Intent.ACTION_CALL, telUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("com.android.phone.force.slot", true)
                putExtra("com.android.phone.extra.slot", simInfo.slotIndex)
                putExtra("simSlot", simInfo.slotIndex)
                putExtra("slot_id", simInfo.slotIndex)
                if (simInfo.subscriptionId >= 0) {
                    putExtra("subscription", simInfo.subscriptionId)
                }
                simInfo.phoneAccountHandle?.let {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
                }
            }
            val started = runCatching {
                context.startActivity(callIntent)
                true
            }.getOrDefault(false)
            if (started) {
                return CallDispatchResult.Dispatched(
                    route = route,
                    formattedNumber = cleanNumber,
                    messageBn = "${simInfo.displayName} দিয়ে কল যাচ্ছে...",
                    messageEn = "Calling via ${simInfo.displayName}..."
                )
            }
        }

        val dialIntent = Intent(Intent.ACTION_DIAL, telUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("com.android.phone.extra.slot", simInfo.slotIndex)
        }
        return runCatching {
            context.startActivity(dialIntent)
            CallDispatchResult.Dispatched(
                route = route,
                formattedNumber = cleanNumber,
                messageBn = "কল শুরু করতে ফোনের CALL পারমিশন দিন",
                messageEn = "Grant CALL permission for direct in-app calling"
            )
        }.getOrElse {
            CallDispatchResult.Error(
                messageBn = "কল শুরু করা যায়নি: ${it.localizedMessage.orEmpty()}",
                messageEn = "Unable to place call: ${it.localizedMessage.orEmpty()}"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun placeBrilliantCall(
        rawNumber: String,
        bridgeStatus: BrilliantBridgeStatus,
        prefs: UserPreferences,
        fallbackSim: SimCardInfo
    ): CallDispatchResult {
        val formatted = formatBangladeshNumberForBrilliant(rawNumber, prefs.brilliantAutoFormatBd)
        if (formatted.length < 3) {
            return CallDispatchResult.InvalidNumber(
                reasonBn = "ব্রিলিয়ান্ট কলের জন্য সঠিক নম্বর লিখুন",
                reasonEn = "Enter a valid phone number for Brilliant Call"
            )
        }

        val targetPackage = bridgeStatus.packageName.ifBlank { prefs.brilliantPackageName }

        if (prefs.brilliantAutoCopyClipboard) {
            copyToClipboard("Brilliant Call", formatted)
        }

        // Priority 1: Telecom VoIP PhoneAccountHandle (keeps call 100% INSIDE NovaCall InCallActivity!)
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val handles = runCatching { telecomManager?.callCapablePhoneAccounts ?: emptyList() }
                .getOrDefault(emptyList())
            val brilliantHandle = handles.firstOrNull { handle ->
                val pkgMatch = handle.componentName.packageName == targetPackage ||
                    handle.componentName.packageName.contains("brilliant", ignoreCase = true)
                val label = runCatching { telecomManager?.getPhoneAccount(handle)?.label?.toString() }.getOrNull()
                pkgMatch || (label?.contains("brilliant", ignoreCase = true) == true)
            }
            if (telecomManager != null && brilliantHandle != null) {
                val telUri = Uri.fromParts("tel", formatted, null)
                val placed = runCatching {
                    val extras = Bundle().apply {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, brilliantHandle)
                    }
                    telecomManager.placeCall(telUri, extras)
                    true
                }.getOrDefault(false)
                if (placed) {
                    return CallDispatchResult.Dispatched(
                        route = CallRoute.BRILLIANT,
                        formattedNumber = formatted,
                        messageBn = "অ্যাপের ভেতর থেকেই ব্রিলিয়ান্ট VoIP কল যাচ্ছে...",
                        messageEn = "Calling via Brilliant VoIP inside NovaCall..."
                    )
                }
            }
        }

        // Priority 2: Stay strictly inside NovaCall when external handoff is disabled (default!)
        if (!prefs.brilliantExternalHandoff) {
            if (hasPermission(Manifest.permission.CALL_PHONE)) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val telUri = Uri.fromParts("tel", formatted, null)
                val placedInApp = runCatching {
                    val extras = Bundle().apply {
                        fallbackSim.phoneAccountHandle?.let {
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
                        }
                    }
                    telecomManager?.placeCall(telUri, extras)
                    telecomManager != null
                }.getOrDefault(false)

                if (placedInApp) {
                    return CallDispatchResult.Dispatched(
                        route = CallRoute.BRILLIANT,
                        formattedNumber = formatted,
                        messageBn = "অ্যাপের ভেতর থেকেই কল যাচ্ছে ($formatted) • ব্রিলিয়ান্ট আলাদা খোলা হয়নি",
                        messageEn = "Calling in-app ($formatted) without leaving NovaCall"
                    )
                }
            }
            return CallDispatchResult.Error(
                messageBn = "অ্যাপের ভেতর থেকে কল করতে NovaCall-কে ডিফল্ট ফোন অ্যাপ ও CALL পারমিশন দিন",
                messageEn = "Set NovaCall as Default Phone App & grant CALL permission for in-app calling"
            )
        }

        // Priority 3 (Only when user explicitly enabled External Brilliant Handoff in Settings)
        if (!bridgeStatus.isInstalled) {
            return CallDispatchResult.BrilliantNotInstalled(targetPackage)
        }

        val telUri = Uri.parse("tel:${Uri.encode(formatted)}")
        val sipUri = Uri.parse("sip:$formatted")

        val intentOrder: List<Intent> = when (prefs.brilliantLaunchMode) {
            BrilliantLaunchMode.DIRECT_CALL_INTENT -> listOf(
                buildPackageIntent(Intent.ACTION_CALL, telUri, targetPackage),
                buildPackageIntent(Intent.ACTION_DIAL, telUri, targetPackage),
                buildPackageIntent(Intent.ACTION_VIEW, telUri, targetPackage)
            )
            BrilliantLaunchMode.DIAL_INTENT -> listOf(
                buildPackageIntent(Intent.ACTION_DIAL, telUri, targetPackage),
                buildPackageIntent(Intent.ACTION_VIEW, telUri, targetPackage)
            )
            BrilliantLaunchMode.SIP_URI -> listOf(
                buildPackageIntent(Intent.ACTION_VIEW, sipUri, targetPackage),
                buildPackageIntent(Intent.ACTION_CALL, sipUri, targetPackage),
                buildPackageIntent(Intent.ACTION_DIAL, telUri, targetPackage)
            )
            BrilliantLaunchMode.LAUNCH_AND_COPY -> emptyList()
            BrilliantLaunchMode.SMART_AUTO -> listOf(
                buildPackageIntent(Intent.ACTION_CALL, telUri, targetPackage),
                buildPackageIntent(Intent.ACTION_DIAL, telUri, targetPackage),
                buildPackageIntent(Intent.ACTION_VIEW, telUri, targetPackage),
                buildPackageIntent(Intent.ACTION_VIEW, sipUri, targetPackage)
            )
        }

        val pm = context.packageManager
        for (intent in intentOrder) {
            if (intent.action == Intent.ACTION_CALL && !hasPermission(Manifest.permission.CALL_PHONE)) {
                continue
            }
            val canHandle = runCatching { pm.resolveActivity(intent, 0) != null }.getOrDefault(false)
            if (canHandle) {
                val started = runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (started) {
                    return CallDispatchResult.Dispatched(
                        route = CallRoute.BRILLIANT,
                        formattedNumber = formatted,
                        messageBn = "ব্রিলিয়ান্ট কানেক্টে কল পাঠানো হয়েছে ($formatted)",
                        messageEn = "Routed to Brilliant Connect ($formatted)"
                    )
                }
            }
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("phone_number", formatted)
            putExtra("number", formatted)
            putExtra("tel", formatted)
            putExtra(Intent.EXTRA_PHONE_NUMBER, formatted)
        }

        if (launchIntent != null) {
            val launched = runCatching {
                context.startActivity(launchIntent)
                true
            }.getOrDefault(false)
            if (launched) {
                return CallDispatchResult.Dispatched(
                    route = CallRoute.BRILLIANT,
                    formattedNumber = formatted,
                    messageBn = "ব্রিলিয়ান্ট খোলা হয়েছে ও নম্বর কপি করা হয়েছে ($formatted)",
                    messageEn = "Opened Brilliant Connect with $formatted copied"
                )
            }
        }

        return CallDispatchResult.BrilliantNotInstalled(targetPackage)
    }

    private fun buildPackageIntent(action: String, uri: Uri, pkg: String): Intent {
        return Intent(action, uri).apply {
            setPackage(pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openBrilliantApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openPlayStoreForBrilliant(packageName: String) {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openedMarket = runCatching {
            context.startActivity(marketIntent)
            true
        }.getOrDefault(false)

        if (!openedMarket) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(webIntent) }
        }
    }

    fun openWhatsAppChat(rawNumber: String): Boolean {
        val clean = normalizePhoneNumber(rawNumber)
        if (clean.length < 6) return false
        val waDigits = when {
            clean.startsWith("+") -> clean.drop(1)
            clean.startsWith("01") && clean.length == 11 -> "88$clean"
            else -> clean
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waDigits")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openSmsComposer(rawNumber: String, message: String = ""): Boolean {
        val clean = normalizePhoneNumber(rawNumber)
        if (clean.isEmpty()) return false
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(clean)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (message.isNotBlank()) {
                putExtra("sms_body", message)
            }
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openAddContact(rawNumber: String, suggestedName: String = ""): Boolean {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (rawNumber.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.PHONE, rawNumber)
            }
            if (suggestedName.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.NAME, suggestedName)
            }
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    suspend fun loadDeviceContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return@withContext emptyList()
        val results = mutableListOf<DeviceContact>()
        val seenKeys = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val starIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (cursor.moveToNext()) {
                    val number = cursor.getString(numIdx)?.trim().orEmpty()
                    val normalized = normalizePhoneNumber(number)
                    if (normalized.isEmpty()) continue
                    val name = cursor.getString(nameIdx)?.trim().orEmpty().ifBlank { number }
                    val dedupeKey = "$name|$normalized"
                    if (!seenKeys.add(dedupeKey)) continue

                    results.add(
                        DeviceContact(
                            id = "${cursor.getString(idIdx)}_$normalized",
                            name = name,
                            phoneNumber = number,
                            normalizedNumber = normalized,
                            photoUri = cursor.getString(photoIdx),
                            isStarred = cursor.getInt(starIdx) == 1
                        )
                    )
                }
            }
        }
        results
    }

    @SuppressLint("MissingPermission")
    suspend fun loadSystemCallLogs(): List<CallLogItem> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.READ_CALL_LOG)) return@withContext emptyList()
        val items = mutableListOf<CallLogItem>()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID,
            CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME
        )

        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val acctIdIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                val compIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)

                var count = 0
                while (cursor.moveToNext() && count < 150) {
                    count++
                    val number = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    val normalized = normalizePhoneNumber(number)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } else null
                    val rawType = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.OUTGOING_TYPE
                    val timestamp = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    val duration = if (durIdx >= 0) cursor.getLong(durIdx) else 0L
                    val acctId = if (acctIdIdx >= 0) cursor.getString(acctIdIdx).orEmpty() else ""
                    val comp = if (compIdx >= 0) cursor.getString(compIdx).orEmpty() else ""

                    val direction = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                        CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                        CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> CallDirection.REJECTED
                        else -> CallDirection.OUTGOING
                    }

                    val route = when {
                        comp.contains("brilliant", ignoreCase = true) -> CallRoute.BRILLIANT
                        acctId.endsWith("2") || acctId.contains("sim2", ignoreCase = true) -> CallRoute.SIM_2
                        else -> CallRoute.SIM_1
                    }

                    items.add(
                        CallLogItem(
                            id = "sys_${if (idIdx >= 0) cursor.getString(idIdx) else count}",
                            number = number,
                            normalizedNumber = normalized,
                            contactName = name,
                            direction = direction,
                            timestamp = timestamp,
                            durationSeconds = duration,
                            route = route,
                            isLocalNovaRecord = false
                        )
                    )
                }
            }
        }
        items
    }
}
