package com.example.data.telephony

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

class NovaInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallStateManager.attachService(this)
    }

    override fun onDestroy() {
        CallStateManager.detachService()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallStateManager.attachService(this)
        CallStateManager.onCallAdded(applicationContext, call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallStateManager.onCallRemoved(applicationContext, call)
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        CallStateManager.onAudioStateChanged(audioState)
    }
}
