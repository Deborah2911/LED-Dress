package com.deborah.dress.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.deborah.dress.LedAlgorithm
import com.deborah.dress.Recorder
import com.deborah.dress.UdpServer
import java.util.Timer
import java.util.TimerTask


class NotificationService : Service() {

    private val notificationHelper by lazy { NotificationHelper(this) }
    private val updateTimer = Timer()
    private val udpServer = UdpServer(1234)
    private val recorder by lazy { Recorder(this) }

    private var isForeground = true

    // State
    private var color = Color.Black
    @Volatile
    private var amplitude = 0
    private var ledAlgorithm = LedAlgorithm.OFF

    override fun onCreate() {
        super.onCreate()

        notificationHelper.createChannel()

        recorder.start { maxAmplitude ->
            amplitude = maxAmplitude
            udpServer.send(color, amplitude, ledAlgorithm)
        }

        val task = object : TimerTask() {
            override fun run() {
                if (isForeground) {
                    notificationHelper.updateNotification(color.toArgb(), amplitude)
                } else {
                    sendAmplitude()
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
        val action = intent?.getStringExtra(SERVICE_ACTION)

        Log.d("Stopwatch", "onStartCommand Action: $action")

        when (action) {
            STOP -> stopSelf()
            GET_STATUS -> sendStatus()
            SET_STATUS -> receiveState(intent)
            MOVE_TO_FOREGROUND -> moveToForeground()
            MOVE_TO_BACKGROUND -> moveToBackground()
            else -> {
                // Stop all background threads, otherwise the service won't stop properly
                recorder.stop()
                updateTimer.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    // region Receive from Activity

    private fun moveToForeground() {
        startForeground(1, notificationHelper.buildNotification(color.toArgb(), amplitude))
        isForeground = true
    }

    private fun moveToBackground() {
        isForeground = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun receiveState(intent: Intent) {
        color = Color(intent.getIntExtra(COLOR_KEY, 0))
        ledAlgorithm = LedAlgorithm.fromByte(intent.getByteExtra(ALGORITHM_KEY, 0))
    }

    // endregion Receive from Activity

    // region Send to Activity

    private fun sendStatus() {
        val statusIntent = Intent().apply {
            action = STATUS_ACTION
            putExtra(COLOR_KEY, color.toArgb())
            putExtra(AMPLITUDE_KEY, amplitude)
            putExtra(ALGORITHM_KEY, ledAlgorithm.toByte())
        }

        sendBroadcast(statusIntent)
    }

    private fun sendAmplitude() {
        val statusIntent = Intent().apply {
            action = STATUS_ACTION
            putExtra(AMPLITUDE_KEY, amplitude)
        }

        sendBroadcast(statusIntent)
    }

    // endregion Send to Activity

    companion object {

        // Service Actions
        const val STOP = "STOP"
        const val GET_STATUS = "GET_STATUS"
        const val SET_STATUS = "SET_STATUS"
        const val MOVE_TO_FOREGROUND = "MOVE_TO_FOREGROUND"
        const val MOVE_TO_BACKGROUND = "MOVE_TO_BACKGROUND"

        // Intent Actions
        const val STATUS_ACTION = "status"

        // Intent Extras
        const val SERVICE_ACTION = "service_action"
        const val COLOR_KEY = "color"
        const val AMPLITUDE_KEY = "amplitude"
        const val ALGORITHM_KEY = "algorithm"
    }
}