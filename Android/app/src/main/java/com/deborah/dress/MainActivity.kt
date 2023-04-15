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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.deborah.dress.ui.theme.AppTheme
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import retrofit2.Retrofit
import retrofit2.create
import java.io.File
import kotlin.concurrent.thread


@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.1")
        .addConverterFactory(Json.asConverterFactory(MediaType.parse("application/json")!!))
        .build()

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
    private val nodeMcu = retrofit.create<NodeMcu>()

    @Suppress("DEPRECATION")
    val recorder = MediaRecorder()

    var pitch by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
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

        thread {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(File(this.dataDir, "text.wav"))
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
            recorder.prepare();
            recorder.start();   // Recording is now started

            while (true) {
                Thread.sleep(500)
                val maxAmplitude = recorder.maxAmplitude
                Log.i("Amplitude", maxAmplitude.toString())
                pitch = maxAmplitude
            }
        }
    }

    @Composable
    fun Content() = Column {
        var text by remember { mutableStateOf("Connect") }

        Text(pitch.toString(), fontSize = 20.sp)

        Button(onClick = {
            runBlocking {
                text = if (nodeMcu.connect().isSuccessful) "Connected!" else "Oops"
            }
            recorder.stop();
            recorder.reset();   // You can reuse the object by going back to setAudioSource() step
            recorder.release(); // Now the object cannot be reused
        }) {
            Text(text = text)
        }
    }

    companion object {
        const val BUFFER_SIZE = 4096
        const val FREQ = 22050
    }
}

