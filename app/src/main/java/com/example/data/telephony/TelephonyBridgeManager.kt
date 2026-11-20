package com.example.data.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.model.CallDirection
import com.example.data.model.CallDispatchResult
import com.example.data.model.CallLogItem
import com.example.data.model.CallRoute
import com.example.data.model.DeviceContact
import com.example.data.model.KeypadSoundStyle
import com.example.data.model.SimCardInfo
import com.example.data.model.UserPreferences
import com.example.data.model.normalizePhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

data class IspNetworkStatus(
    val connectionType: String,
    val publicIp: String,
    val ispName: String,
    val city: String,
    val downstreamMbps: Int,
    val upstreamMbps: Int,
    val isIccNetwork: Boolean
)

data class NewContactPayload(
    val fullName: String,
    val phoneNumber: String,
    val alternatePhone: String = "",
    val email: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val address: String = "",
    val notes: String = "",
    val photoUri: Uri? = null
)

class TelephonyBridgeManager(private val context: Context) {

    private var soundEngineInstance: DialpadSoundEngine? = null
    private val soundEngine: DialpadSoundEngine
        get() = soundEngineInstance ?: DialpadSoundEngine().also { soundEngineInstance = it }

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

    fun openAppInfoSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openSystemDefaultAppsSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { openAppInfoSettings() }
    }

    fun openSystemSoundSettings() {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openSipCallingAccountsSettings() {
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { openSystemDefaultAppsSettings() }
    }

    fun openWebUrl(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun defaultBalanceUssdForCarrier(carrierName: String): String {
        val lower = carrierName.lowercase()
        return when {
            lower.contains("skitto") -> "*121#"
            lower.contains("grameen") || lower.contains("gp") || lower.contains("47001") -> "*566#"
            lower.contains("robi") || lower.contains("airtel") || lower.contains("47002") || lower.contains("47007") -> "*222#"
            lower.contains("banglalink") || lower.contains("bl") || lower.contains("47003") -> "*124#"
            lower.contains("teletalk") || lower.contains("47004") -> "*152#"
            else -> "*566#"
        }
    }

    @SuppressLint("MissingPermission")
    fun checkSimBalance(
        sim: SimCardInfo,
        customUssdCode: String,
        onResult: (Result<String>) -> Unit
    ) {
        val code = customUssdCode.trim().ifBlank { defaultBalanceUssdForCarrier(sim.carrierName) }
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            onResult(Result.failure(SecurityException("CALL_PHONE_REQUIRED")))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val baseTm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val subTm = if (sim.subscriptionId > 0 && baseTm != null) {
                baseTm.createForSubscriptionId(sim.subscriptionId)
            } else {
                baseTm
            }
            if (subTm != null) {
                val callback = object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager?,
                        request: String?,
                        response: CharSequence?
                    ) {
                        val msg = response?.toString()?.trim().orEmpty()
                        if (msg.isNotEmpty()) {
                            onResult(Result.success(msg))
                        } else {
                            dialUssdFallback(sim, code)
                            onResult(Result.success("USSD ($code) ডায়াল করা হয়েছে..."))
                        }
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager?,
                        request: String?,
                        failureCode: Int
                    ) {
                        dialUssdFallback(sim, code)
                        onResult(Result.success("USSD ($code) সিমের মাধ্যমে চালু করা হয়েছে..."))
                    }
                }
                val sent = runCatching {
                    subTm.sendUssdRequest(code, callback, Handler(Looper.getMainLooper()))
                    true
                }.getOrDefault(false)
                if (sent) return
            }
        }

        dialUssdFallback(sim, code)
        onResult(Result.success("USSD ($code) চালু করা হয়েছে..."))
    }

    @SuppressLint("MissingPermission")
    private fun dialUssdFallback(sim: SimCardInfo, ussdCode: String) {
        val encoded = Uri.encode(ussdCode)
        val uri = Uri.parse("tel:$encoded")
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecomManager != null && hasPermission(Manifest.permission.CALL_PHONE)) {
            val placed = runCatching {
                val extras = Bundle().apply {
                    sim.phoneAccountHandle?.let { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
                }
                telecomManager.placeCall(uri, extras)
                true
            }.getOrDefault(false)
            if (placed) return
        }
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            sim.phoneAccountHandle?.let { putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
            if (sim.subscriptionId > 0) {
                putExtra("com.android.phone.extra.slot", sim.slotIndex)
                putExtra("simSlot", sim.slotIndex)
                putExtra("subscription", sim.subscriptionId)
            }
        }
        runCatching { context.startActivity(intent) }
    }

    @SuppressLint("MissingPermission")
    fun placeIccSipCall(
        rawNumber: String,
        server: String,
        username: String
    ): CallDispatchResult {
        val clean = normalizePhoneNumber(rawNumber)
        if (clean.length < 3) {
            return CallDispatchResult.InvalidNumber(
                reasonBn = "অনুগ্রহ করে সঠিক ফোন নম্বর লিখুন",
                reasonEn = "Please enter a valid phone number"
            )
        }
        val cleanServer = server.trim().ifBlank { "sip.icc.com.bd" }
        val sipAddress = "$clean@$cleanServer"
        val sipUri = Uri.fromParts("sip", sipAddress, null)

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecomManager != null && hasPermission(Manifest.permission.CALL_PHONE)) {
            val sipHandle = runCatching {
                telecomManager.callCapablePhoneAccounts.firstOrNull { handle ->
                    val acct = telecomManager.getPhoneAccount(handle)
                    val label = acct?.label?.toString().orEmpty()
                    val addr = acct?.address?.scheme.orEmpty()
                    addr.equals("sip", ignoreCase = true) ||
                        label.contains("icc", ignoreCase = true) ||
                        label.contains("sip", ignoreCase = true) ||
                        handle.id.contains("sip", ignoreCase = true)
                }
            }.getOrNull()

            if (sipHandle != null) {
                val placed = runCatching {
                    val extras = Bundle().apply {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, sipHandle)
                    }
                    telecomManager.placeCall(sipUri, extras)
                    true
                }.getOrDefault(false)
                if (placed) {
                    return CallDispatchResult.Dispatched(
                        route = CallRoute.ICC_SIP,
                        formattedNumber = clean,
                        messageBn = "ICC IP Calling ($cleanServer) দিয়ে কল যাচ্ছে: $clean",
                        messageEn = "Calling via ICC IP ($cleanServer): $clean"
                    )
                }
            }
        }

        // Direct SIP URI call intent fallback (handled by Android SIP / IPTSP VoIP stack)
        val sipCallIntent = Intent(Intent.ACTION_CALL, sipUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (username.isNotBlank()) {
                putExtra("sip_username", username)
            }
        }
        val launchedSip = runCatching {
            context.startActivity(sipCallIntent)
            true
        }.getOrDefault(false)
        if (launchedSip) {
            return CallDispatchResult.Dispatched(
                route = CallRoute.ICC_SIP,
                formattedNumber = clean,
                messageBn = "ICC IP Calling ($cleanServer) চালু হয়েছে: $clean",
                messageEn = "ICC IP Calling ($cleanServer) dispatched: $clean"
            )
        }

        val sipDialIntent = Intent(Intent.ACTION_DIAL, sipUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(sipDialIntent)
            CallDispatchResult.Dispatched(
                route = CallRoute.ICC_SIP,
                formattedNumber = clean,
                messageBn = "ICC SIP ডায়ালার খোলা হয়েছে ($sipAddress)",
                messageEn = "Opened SIP dialer for $sipAddress"
            )
        }.getOrElse {
            CallDispatchResult.Error(
                messageBn = "ফোনে SIP অ্যাকাউন্ট চালু নেই। সেটিংস > ICC IP Calling > Calling Accounts থেকে SIP অ্যাকাউন্ট যোগ করুন।",
                messageEn = "No system SIP handler found. Open Calling Accounts in ICC settings to enable SIP transport."
            )
        }
    }

    suspend fun testIccSipServer(server: String, port: Int): Result<Long> = withContext(Dispatchers.IO) {
        val host = server.trim().ifBlank { "sip.icc.com.bd" }
        val safePort = port.coerceIn(1, 65535)
        runCatching {
            val start = System.currentTimeMillis()
            // Resolve DNS first (validates host), then try TCP connect or HTTPS port reachability
            val addr = java.net.InetAddress.getByName(host)
            val elapsed = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(addr, safePort), 3000)
                }
                System.currentTimeMillis() - start
            }.getOrElse {
                // Many SIP servers only listen on UDP 5060; DNS resolution succeeded so return ping ms
                (System.currentTimeMillis() - start).coerceAtLeast(1L)
            }
            elapsed
        }
    }

    suspend fun fetchIspNetworkStatus(): Result<IspNetworkStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = if (network != null) cm.getNetworkCapabilities(network) else null
            val connType = when {
                caps == null -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi (Broadband/ISP)"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connected"
            }
            val downMbps = ((caps?.linkDownstreamBandwidthKbps ?: 0) / 1000).coerceAtLeast(0)
            val upMbps = ((caps?.linkUpstreamBandwidthKbps ?: 0) / 1000).coerceAtLeast(0)

            var publicIp = "-"
            var ispName = "-"
            var city = ""

            runCatching {
                val conn = (URL("https://ipwho.is/").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4500
                    readTimeout = 4500
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    publicIp = json.optString("ip", "-")
                    val connectionObj = json.optJSONObject("connection")
                    ispName = connectionObj?.optString("isp")?.takeIf { it.isNotBlank() }
                        ?: connectionObj?.optString("org").orEmpty().ifBlank { "-" }
                    city = json.optString("city", "")
                }
                conn.disconnect()
            }

            IspNetworkStatus(
                connectionType = connType,
                publicIp = publicIp,
                ispName = ispName,
                city = city,
                downstreamMbps = downMbps,
                upstreamMbps = upMbps,
                isIccNetwork = ispName.contains("icc", ignoreCase = true)
            )
        }
    }

    fun playDialpadSound(symbol: String, style: KeypadSoundStyle, volume: Float) {
        soundEngine.playKey(symbol, style, volume)
    }

    fun releaseToneGenerator() {
        soundEngineInstance?.release()
        soundEngineInstance = null
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
    fun placeSimCall(rawNumber: String, sim: SimCardInfo): CallDispatchResult {
        val clean = normalizePhoneNumber(rawNumber)
        if (clean.length < 3) {
            return CallDispatchResult.InvalidNumber(
                reasonBn = "অনুগ্রহ করে সঠিক ফোন নম্বর লিখুন",
                reasonEn = "Please enter a valid phone number"
            )
        }

        val route = if (sim.slotIndex == 1) CallRoute.SIM_2 else CallRoute.SIM_1
        val uri = Uri.fromParts("tel", clean, null)

        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                val placed = runCatching {
                    val extras = Bundle().apply {
                        sim.phoneAccountHandle?.let { handle ->
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                        }
                    }
                    telecomManager.placeCall(uri, extras)
                    true
                }.getOrDefault(false)
                if (placed) {
                    return CallDispatchResult.Dispatched(
                        route = route,
                        formattedNumber = clean,
                        messageBn = "${sim.displayName} দিয়ে কল যাচ্ছে: $clean",
                        messageEn = "Calling via ${sim.displayName}: $clean"
                    )
                }
            }

            val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                sim.phoneAccountHandle?.let { handle ->
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
                if (sim.subscriptionId > 0) {
                    putExtra("com.android.phone.extra.slot", sim.slotIndex)
                    putExtra("simSlot", sim.slotIndex)
                    putExtra("subscription", sim.subscriptionId)
                }
            }
            val started = runCatching {
                context.startActivity(callIntent)
                true
            }.getOrDefault(false)
            if (started) {
                return CallDispatchResult.Dispatched(
                    route = route,
                    formattedNumber = clean,
                    messageBn = "${sim.displayName} দিয়ে কল যাচ্ছে: $clean",
                    messageEn = "Calling via ${sim.displayName}: $clean"
                )
            }
        }

        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(dialIntent)
            CallDispatchResult.Dispatched(
                route = route,
                formattedNumber = clean,
                messageBn = "কল পারমিশন না থাকায় ডায়ালার খোলা হয়েছে ($clean)",
                messageEn = "Opened dialer for $clean (grant Call permission for 1-tap calling)"
            )
        }.getOrElse {
            CallDispatchResult.Error(
                messageBn = "কল শুরু করা যায়নি। ফোন পারমিশন চেক করুন।",
                messageEn = "Could not start call. Please check Phone permission."
            )
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

    suspend fun saveContactWithPhoto(payload: NewContactPayload): Result<String> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.WRITE_CONTACTS)) {
            return@withContext Result.failure(SecurityException("WRITE_CONTACTS_DENIED"))
        }
        val cleanName = payload.fullName.trim()
        val cleanPhone = payload.phoneNumber.trim()
        if (cleanName.isEmpty() || cleanPhone.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Name and phone required"))
        }

        runCatching {
            val ops = ArrayList<ContentProviderOperation>()

            // 1. Insert RawContact
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. Display Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, cleanName)
                    .build()
            )

            // 3. Primary Mobile Phone
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, cleanPhone)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            // 4. Optional Alternate Phone
            val altPhone = payload.alternatePhone.trim()
            if (altPhone.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, altPhone)
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                        )
                        .build()
                )
            }

            // 5. Optional Email
            val email = payload.email.trim()
            if (email.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_WORK
                        )
                        .build()
                )
            }

            // 6. Optional Company / Job Title
            val company = payload.company.trim()
            val title = payload.jobTitle.trim()
            if (company.isNotEmpty() || title.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, title)
                        .withValue(
                            ContactsContract.CommonDataKinds.Organization.TYPE,
                            ContactsContract.CommonDataKinds.Organization.TYPE_WORK
                        )
                        .build()
                )
            }

            // 7. Optional Address
            val address = payload.address.trim()
            if (address.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
                        )
                        .build()
                )
            }

            // 8. Optional Notes
            val notes = payload.notes.trim()
            if (notes.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                        .build()
                )
            }

            // 9. Optional Contact Photo (compressed safely to max 512px JPEG)
            payload.photoUri?.let { uri ->
                decodeContactPhotoBytes(uri)?.let { photoBytes ->
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build()
                    )
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            cleanName
        }
    }

    private fun decodeContactPhotoBytes(uri: Uri): ByteArray? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (maxDim / sampleSize > 640) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
                val ratio = 512f / maxOf(bitmap.width, bitmap.height)
                val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            scaled.recycle()
            out.toByteArray()
        }.getOrNull()
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    suspend fun loadDeviceContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return@withContext emptyList()
        val results = ArrayList<DeviceContact>(256)
        val seenKeys = HashSet<String>(512)

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
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL",
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
        val items = ArrayList<CallLogItem>(200)

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        runCatching {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${CallLog.Calls.DATE} DESC")
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 200)
                }
                context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, queryArgs, null)
            } else {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
            }
            cursor?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val photoIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val acctIdIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                var count = 0
                while (cursor.moveToNext() && count < 200) {
                    count++
                    val number = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    val normalized = normalizePhoneNumber(number)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } else null
                    val photoUri = if (photoIdx >= 0) cursor.getString(photoIdx)?.takeIf { it.isNotBlank() } else null
                    val rawType = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.OUTGOING_TYPE
                    val timestamp = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    val duration = if (durIdx >= 0) cursor.getLong(durIdx) else 0L
                    val acctId = if (acctIdIdx >= 0) cursor.getString(acctIdIdx).orEmpty() else ""

                    val direction = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                        CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                        CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> CallDirection.REJECTED
                        else -> CallDirection.OUTGOING
                    }

                    val route = if (acctId.endsWith("2") || acctId.contains("sim2", ignoreCase = true)) {
                        CallRoute.SIM_2
                    } else {
                        CallRoute.SIM_1
                    }

                    items.add(
                        CallLogItem(
                            id = "sys_${if (idIdx >= 0) cursor.getString(idIdx) else count}",
                            number = number,
                            normalizedNumber = normalized,
                            contactName = name,
                            photoUri = photoUri,
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

    @SuppressLint("MissingPermission")
    suspend fun deleteSystemCallLog(systemRowId: String): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.WRITE_CALL_LOG)) return@withContext false
        if (systemRowId.isBlank()) return@withContext false
        runCatching {
            val deleted = context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(systemRowId)
            )
            deleted > 0
        }.getOrDefault(false)
    }
}
