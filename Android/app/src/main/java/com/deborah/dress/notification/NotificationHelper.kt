package com.deborah.dress.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.deborah.dress.MainActivity
import com.deborah.dress.R

class NotificationHelper(private val ctx: Context) {

    private val manager = ctx.getSystemService<NotificationManager>()!!

    fun createChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            "Audio Recording",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationChannel.setShowBadge(true)

        manager.createNotificationChannel(notificationChannel)
    }

    fun buildNotification(color: Int, amplitude: Int): Notification {
        val title = "Listening to microphone"

        val intent = Intent(ctx, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopSelf = Intent(ctx, NotificationService::class.java)
        stopSelf.action = NotificationService.STOP
        val pStopSelf = PendingIntent.getService(
            ctx,
            0,
            stopSelf,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle(title)
            .setOngoing(true)
            .setContentText("Amplitude: $amplitude")
            .setColorized(true)
            .setColor(color)
            .addAction(R.drawable.baseline_stop_circle_24, "Stop", pStopSelf)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setContentIntent(pIntent)
            .setAutoCancel(true)
            .build()
    }

    fun updateNotification(color: Int, amplitude: Int) {
        manager.notify(
            1,
            buildNotification(color, amplitude)
        )
    }

    companion object {
        const val CHANNEL_ID = "audio"
    }
}