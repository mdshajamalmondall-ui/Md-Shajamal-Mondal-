package com.example.data.telephony

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.InCallActivity
import com.example.MainActivity
import com.example.R

object CallNotificationHelper {

    const val CHANNEL_INCOMING = "novacall_incoming_calls"
    const val CHANNEL_ONGOING = "novacall_ongoing_calls"
    const val CHANNEL_MISSED = "novacall_missed_calls"

    private const val NOTIF_ID_ACTIVE_CALL = 4101
    private const val NOTIF_ID_MISSED_BASE = 4200

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val incoming = NotificationChannel(
            CHANNEL_INCOMING,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen alerts for incoming calls in Shajamal"
            enableVibration(true)
            setSound(null, null) // Telecom system handles ringtone
        }

        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            "Ongoing Calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active call status and quick controls"
            setSound(null, null)
        }

        val missed = NotificationChannel(
            CHANNEL_MISSED,
            "Missed Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when a call is missed"
            enableVibration(true)
        }

        manager.createNotificationChannel(incoming)
        manager.createNotificationChannel(ongoing)
        manager.createNotificationChannel(missed)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun showIncomingCallNotification(
        context: Context,
        callerName: String,
        callerNumber: String,
        lineLabel: String
    ) {
        ensureChannels(context)
        if (!canPostNotifications(context)) return

        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPending = PendingIntent.getActivity(
            context,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerPending = PendingIntent.getBroadcast(
            context,
            102,
            Intent(context, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_ANSWER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePending = PendingIntent.getBroadcast(
            context,
            103,
            Intent(context, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_DECLINE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName.ifBlank { callerNumber })
            .setContentText("Incoming call • $lineLabel")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPending)
            .setFullScreenIntent(fullScreenPending, true)
            .addAction(0, "Answer", answerPending)
            .addAction(0, "Decline", declinePending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_ACTIVE_CALL, notif)
        }
    }

    fun showOngoingCallNotification(
        context: Context,
        callerName: String,
        callerNumber: String,
        statusLabel: String,
        isConnected: Boolean = false
    ) {
        ensureChannels(context)
        if (!canPostNotifications(context)) return

        val openCallIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            201,
            openCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hangupPending = PendingIntent.getBroadcast(
            context,
            202,
            Intent(context, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_HANGUP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName.ifBlank { callerNumber })
            .setContentText(statusLabel)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setUsesChronometer(isConnected)
            .setContentIntent(openPending)
            .addAction(0, "End Call", hangupPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_ACTIVE_CALL, notif)
        }
    }

    fun cancelActiveCallNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIF_ID_ACTIVE_CALL)
        }
    }

    fun showMissedCallNotification(context: Context, callerName: String, callerNumber: String) {
        ensureChannels(context)
        if (!canPostNotifications(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            301,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Missed Call")
            .setContentText(callerName.ifBlank { callerNumber })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching {
            val id = NOTIF_ID_MISSED_BASE + ((callerNumber.hashCode() and 0xFF))
            NotificationManagerCompat.from(context).notify(id, notif)
        }
    }
}
