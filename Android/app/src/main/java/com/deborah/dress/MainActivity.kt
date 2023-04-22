package com.deborah.dress

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.deborah.dress.ui.theme.AppTheme
import io.mhssn.colorpicker.ColorPicker
import io.mhssn.colorpicker.ColorPickerType
import java.io.File
import kotlin.concurrent.thread


@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {


    /*val recorder by lazy {
        AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .build()
    }*/

    private lateinit var statusReceiver: BroadcastReceiver

    // Compose
    private var selectedColor by mutableStateOf(Color.White)
    private var amplitude by mutableStateOf(0)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
//                    startRecorder()
                } else {
                    Toast.makeText(this, "This permission is required!!!", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }

            LaunchedEffect(Unit) {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }

            AppTheme(true) {
                Scaffold { paddingValues ->
                    Box(Modifier.padding(paddingValues)) {
                        Content()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Moving the service to background when the app is visible
        moveToBackground()
    }

    override fun onResume() {
        super.onResume()

        getNotificationStatus()

        // Receiving stopwatch status from service
        val statusFilter = IntentFilter()
        statusFilter.addAction(NotificationService.STATUS_ACTION)
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent) {
                Log.d("Hello", "Received Status")
                selectedColor = Color(intent.getIntExtra(NotificationService.COLOR_KEY, Color.Black.toArgb()))
                amplitude = intent.getIntExtra(NotificationService.AMPLITUDE_KEY, 0)
            }
        }
        registerReceiver(statusReceiver, statusFilter)
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(statusReceiver)
//        unregisterReceiver(timeReceiver)

        // Moving the service to foreground when the app is in background / not visible
        moveToForeground()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Content() = Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

//        LaunchedEffect(selectedColor, amplitude) {
//        }

        Text(amplitude.toString(), fontSize = 20.sp)

        Spacer(Modifier.height(32.dp))

        ColorPicker(
            modifier = Modifier.fillMaxSize(),
            type = ColorPickerType.Circle(
                showAlphaBar = false
            ),
            onPickedColor = { selectedColor = it }
        )
    }

    private fun getNotificationStatus() {
        val stopwatchService = Intent(this, NotificationService::class.java)
        stopwatchService.putExtra(
            NotificationService.NOTIFICATION_ACTION,
            NotificationService.GET_STATUS
        )
        startService(stopwatchService)
    }

    private fun moveToForeground() {
        val stopwatchService = Intent(this, NotificationService::class.java)
        stopwatchService.putExtra(
            NotificationService.NOTIFICATION_ACTION,
            NotificationService.MOVE_TO_FOREGROUND
        )
        startService(stopwatchService)
    }

    private fun moveToBackground() {
        val stopwatchService = Intent(this, NotificationService::class.java)
        stopwatchService.putExtra(
            NotificationService.NOTIFICATION_ACTION,
            NotificationService.MOVE_TO_BACKGROUND
        )
        startService(stopwatchService)
    }
}

