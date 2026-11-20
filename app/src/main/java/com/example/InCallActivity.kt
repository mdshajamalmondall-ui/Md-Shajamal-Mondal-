package com.example

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.local.CallNoteEntity
import com.example.data.local.NovaCallDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.UserPreferences
import com.example.data.model.normalizePhoneNumber
import com.example.data.telephony.CallRecorderManager
import com.example.data.telephony.CallStateManager
import com.example.ui.screens.InCallScreen
import com.example.ui.theme.NovaCallTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenDisplay()

        val prefsRepo = PreferencesRepository(applicationContext)
        val dao = NovaCallDatabase.getInstance(applicationContext).novaCallDao()

        setContent {
            val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())
            val activeCall by CallStateManager.activeCall.collectAsStateWithLifecycle()
            val isDeviceLocked = remember {
                (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
            }

            val recordPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    CallStateManager.toggleRecording(this@InCallActivity)
                } else {
                    val isBn = prefs.language == AppLanguage.BENGALI
                    Toast.makeText(
                        this@InCallActivity,
                        if (isBn) "কল রেকর্ড করতে মাইক্রোফোন অনুমতি দিন" else "Allow Microphone permission to record calls",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            LaunchedEffect(activeCall) {
                if (activeCall == null) {
                    finish()
                }
            }

            NovaCallTheme(
                darkTheme = prefs.darkThemeEnabled,
                themePalette = prefs.themePalette,
                displayThemeMode = prefs.displayThemeMode,
                fontScale = prefs.fontScale,
                liquidGlassEnabled = prefs.liquidGlassEnabled
            ) {
                activeCall?.let { state ->
                    InCallScreen(
                        callState = state,
                        language = prefs.language,
                        quickReplies = prefs.quickReplies,
                        liquidGlassEnabled = prefs.liquidGlassEnabled,
                        slideToAnswerMode = prefs.slideToAnswerMode,
                        isDeviceLocked = isDeviceLocked,
                        onAnswer = { CallStateManager.answerCall() },
                        onDecline = { CallStateManager.rejectCall() },
                        onRejectWithSms = { msg -> CallStateManager.rejectCall(msg) },
                        onSetReminder = { reminderLabel ->
                            val normalized = normalizePhoneNumber(state.number)
                            if (normalized.isNotBlank()) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    dao.upsertCallNote(
                                        CallNoteEntity(
                                            normalizedNumber = normalized,
                                            displayNumber = state.number,
                                            contactName = state.displayName,
                                            note = reminderLabel,
                                            hasReminder = true,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            val isBn = prefs.language == AppLanguage.BENGALI
                            Toast.makeText(
                                this@InCallActivity,
                                if (isBn) "রিমাইন্ডার সেট হয়েছে: $reminderLabel" else "Reminder saved: $reminderLabel",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onEndCall = { CallStateManager.endCall() },
                        onToggleMute = { CallStateManager.toggleMute() },
                        onToggleSpeaker = { CallStateManager.toggleSpeaker() },
                        onToggleHold = { CallStateManager.toggleHold() },
                        onToggleRecord = {
                            if (CallRecorderManager.hasRecordPermission(this@InCallActivity)) {
                                CallStateManager.toggleRecording(this@InCallActivity)
                            } else {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onPlayDtmf = { digit -> CallStateManager.playInCallDtmf(digit) },
                        onOpenDialerToAddCall = {
                            startActivity(
                                Intent(this@InCallActivity, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
