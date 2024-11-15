package com.deborah.dress

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.deborah.dress.notification.NotificationService
import com.deborah.dress.ui.theme.AppTheme
import io.mhssn.colorpicker.ColorPicker
import io.mhssn.colorpicker.ColorPickerType

@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {

    private lateinit var statusReceiver: BroadcastReceiver

    // Compose
    private var selectedColor by mutableStateOf(Color.White)
    private var amplitude by mutableStateOf(0)
    private var selectedAlgorithm by mutableStateOf(LedAlgorithm.OFF)

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
                // Color is optional
                val newColor = intent.getIntExtra(NotificationService.COLOR_KEY, -1)
                if (newColor != -1) {
                    selectedColor = Color(newColor)
                }
                // Algorithm is optional
                val newAlgorithm =
                    intent.getByteExtra(NotificationService.ALGORITHM_KEY, 255.toByte())
                if (newAlgorithm != 255.toByte()) {
                    selectedAlgorithm = LedAlgorithm.fromByte(newAlgorithm)
                }

                amplitude = intent.getIntExtra(NotificationService.AMPLITUDE_KEY, 0)
            }
        }
        registerReceiver(statusReceiver, statusFilter)
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(statusReceiver)

        // Moving the service to foreground when the app is in background / not visible
        moveToForeground()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Content() = Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LaunchedEffect(selectedColor, selectedAlgorithm) {
            sendState()
        }

        Text(amplitude.toString(), fontSize = 20.sp)

        Spacer(Modifier.height(32.dp))

        ColorPicker(
            type = ColorPickerType.Circle(
                showAlphaBar = false
            ),
            onPickedColor = { selectedColor = it }
        )

        Spacer(Modifier.height(32.dp))

        LedAlgorithm.values().forEach { algorithm ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedAlgorithm == algorithm,
                        role = Role.RadioButton,
                        onClick = { selectedAlgorithm = algorithm }
                    )
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = selectedAlgorithm == algorithm, onClick = null)

                Text(
                    text = algorithm.toString(),
                    style = MaterialTheme.typography.bodyLarge.merge(),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }

    private fun sendState() = sendNotificationIntent {
        putExtra(
            NotificationService.SERVICE_ACTION,
            NotificationService.SET_STATUS
        )
        putExtra(
            NotificationService.COLOR_KEY,
            selectedColor.toArgb()
        )
        putExtra(
            NotificationService.ALGORITHM_KEY,
            selectedAlgorithm.toByte()
        )
    }

    private fun getNotificationStatus() = sendNotificationIntent {
        putExtra(
            NotificationService.SERVICE_ACTION,
            NotificationService.GET_STATUS
        )
    }

    private fun moveToForeground() = sendNotificationIntent {
        putExtra(
            NotificationService.SERVICE_ACTION,
            NotificationService.MOVE_TO_FOREGROUND
        )
    }

    private fun moveToBackground() = sendNotificationIntent {
        putExtra(
            NotificationService.SERVICE_ACTION,
            NotificationService.MOVE_TO_BACKGROUND
        )
    }

    private fun sendNotificationIntent(block: Intent.() -> Unit) {
        val intent = Intent(this, NotificationService::class.java).apply(block)
        startService(intent)
    }
}

