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
            ACTION_CALL_REMINDER -> {
                val number = intent.getStringExtra(EXTRA_REMINDER_NUMBER).orEmpty()
                val name = intent.getStringExtra(EXTRA_REMINDER_NAME).orEmpty()
                val note = intent.getStringExtra(EXTRA_REMINDER_NOTE).orEmpty()
                if (number.isNotBlank()) {
                    CallNotificationHelper.showCallReminderNotification(
                        context = context,
                        callerName = name,
                        callerNumber = number,
                        note = note
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.example.novacall.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.example.novacall.ACTION_DECLINE"
        const val ACTION_HANGUP = "com.example.novacall.ACTION_HANGUP"
        const val ACTION_CALL_REMINDER = "com.example.novacall.ACTION_CALL_REMINDER"
        const val EXTRA_REMINDER_NUMBER = "extra_reminder_number"
        const val EXTRA_REMINDER_NAME = "extra_reminder_name"
        const val EXTRA_REMINDER_NOTE = "extra_reminder_note"
    }
}
