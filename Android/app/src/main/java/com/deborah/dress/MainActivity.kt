package com.deborah.dress

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.deborah.dress.ui.theme.AppTheme
import io.mhssn.colorpicker.ColorPicker
import io.mhssn.colorpicker.ColorPickerType
import kotlinx.coroutines.delay
import java.io.File
import kotlin.concurrent.thread


@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {

    private val udpServer = UdpServer(1234)

    /*@OptIn(ExperimentalSerializationApi::class)
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.4.1")
        .addConverterFactory(Json.asConverterFactory(MediaType.parse("application/json")!!))
        .build()*/

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
    }
    private val nodeMcu = retrofit.create<NodeMcu>()*/

    @Suppress("DEPRECATION")
    private val recorder = MediaRecorder()

    var amplitude by mutableStateOf(0)

    private fun startRecorder() {
        thread {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setOutputFile(File(this.dataDir, "text.wav"))
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            recorder.prepare()
            recorder.start()

            while (true) {
                Thread.sleep(500)
                val maxAmplitude = recorder.maxAmplitude
                Log.i("Amplitude", maxAmplitude.toString())
                amplitude = maxAmplitude
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    startRecorder()
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

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Content() = Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var selectedColor by remember { mutableStateOf(Color.White) }

        LaunchedEffect(Unit) {
            while (true) {
                udpServer.send(selectedColor, amplitude)
                delay(200)
            }
        }

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
}

