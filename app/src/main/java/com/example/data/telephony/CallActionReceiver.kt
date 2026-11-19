package com.example.data.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER -> CallStateManager.answerCall()
            ACTION_DECLINE -> CallStateManager.rejectCall()
            ACTION_HANGUP -> CallStateManager.endCall()
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.example.novacall.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.example.novacall.ACTION_DECLINE"
        const val ACTION_HANGUP = "com.example.novacall.ACTION_HANGUP"
    }
}
