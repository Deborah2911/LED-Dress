package com.deborah.dress

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread


class NotificationService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var updateTimer = Timer()
    private var isForeground = true

    private val udpServer = UdpServer(1234)

    @Suppress("DEPRECATION")
    private val recorder = MediaRecorder()

    private var color = Color.Black
    private var amplitude = 0

    private fun startRecorder() {
        thread {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(File(this.cacheDir, "temp.wav"))
            recorder.prepare()
            recorder.start()

            while (true) {
                Thread.sleep(400)
                val maxAmplitude = recorder.maxAmplitude
                Log.i("Amplitude", maxAmplitude.toString())
                amplitude = maxAmplitude
                udpServer.send(color, amplitude)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager
        createChannel()

        startRecorder()

        val task = object : TimerTask() {
            override fun run() {
                if (isForeground) {
                    updateNotification()
                } else {
                    sendStatus()
                }
            }
        }

        updateTimer.scheduleAtFixedRate(task, 0, 1000)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("NotificationService", "onBind was called")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(NOTIFICATION_ACTION)

        Log.d("Stopwatch", "onStartCommand Action: $action")

        when (action) {
            STOP -> stopSelf()
            GET_STATUS -> sendStatus()
            MOVE_TO_FOREGROUND -> moveToForeground()
            MOVE_TO_BACKGROUND -> moveToBackground()
            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    /*
   * This function is triggered when the app is not visible to the user anymore
   * It check if the stopwatch is running, if it is then it starts a foreground service
   * with the notification.
   * We run another timer to update the notification every second.
   * */
    private fun moveToForeground() {
        startForeground(1, buildNotification())
        isForeground = true
    }

    /*
    * This function is triggered when the app is visible again to the user
    * It cancels the timer which was updating the notification every second
    * It also stops the foreground service and removes the notification
    * */
    private fun moveToBackground() {
        isForeground = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            "Audio Recording",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationChannel.setShowBadge(true)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    /*
    * This function is responsible for broadcasting the status of the stopwatch
    * Broadcasts if the stopwatch is running and also the time elapsed
    * */
    private fun sendStatus() {
        val statusIntent = Intent()
        statusIntent.action = STATUS_ACTION
        statusIntent.putExtra(COLOR_KEY, color.toArgb())
        statusIntent.putExtra(AMPLITUDE_KEY, amplitude)
        sendBroadcast(statusIntent)
    }

    private fun buildNotification(): Notification {
        val title = "Listening to microphone"

        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopSelf = Intent(this, NotificationService::class.java)
        stopSelf.action = STOP
        val pStopSelf = PendingIntent.getService(
            this,
            0,
            stopSelf,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setOngoing(true)
            .setContentText("Amplitude: $amplitude")
            .setColorized(true)
            .setColor(color.toArgb())
            .addAction(R.drawable.baseline_stop_circle_24, "Stop", pStopSelf)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setContentIntent(pIntent)
            .setAutoCancel(true)
            .build()
    }


    /*
    * This function uses the notificationManager to update the existing notification with the new notification
    * */
    private fun updateNotification() {
        notificationManager.notify(
            1,
            buildNotification()
        )
    }

    companion object {
        const val CHANNEL_ID = "audio"

        // Service Actions
        const val STOP = "STOP"
        const val GET_STATUS = "GET_STATUS"
        const val MOVE_TO_FOREGROUND = "MOVE_TO_FOREGROUND"
        const val MOVE_TO_BACKGROUND = "MOVE_TO_BACKGROUND"

        // Intent Actions
        const val STATUS_ACTION = "status"

        // Intent Extras
        const val NOTIFICATION_ACTION = "notification_action"
        const val COLOR_KEY = "color"
        const val AMPLITUDE_KEY = "amplitude"
    }
}