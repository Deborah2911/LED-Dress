package com.deborah.dress

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import com.deborah.dress.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.create


@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.1")
//        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
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

    var pitch by mutableStateOf(0f)

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

        val dispatcher: AudioDispatcher =
            AudioDispatcherFactory.fromDefaultMicrophone(22050, BUFFER_SIZE, 0)
        val pdh = PitchDetectionHandler { res, e ->
            val pitchInHz = res.pitch
            pitch = pitchInHz
        }
        val pitchProcessor: AudioProcessor =
            PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                22050f,
                BUFFER_SIZE,
                pdh
            )
        dispatcher.addAudioProcessor(pitchProcessor)
        Thread(dispatcher).start()

        /*lifecycleScope.launch(Dispatchers.IO) {

            val channelConfiguration = AudioFormat.CHANNEL_IN_MONO
            val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
            val blockSize = 256
            val transformer = RealDoubleFFT(blockSize)
            val bufferSize = AudioRecord.getMinBufferSize(FREQ, channelConfiguration, audioEncoding)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, FREQ,
                channelConfiguration, audioEncoding, bufferSize
            )

            val audioBuffer = ShortArray(blockSize)
            val toTransform = DoubleArray(blockSize)

            audioRecord.startRecording()

            while (isActive) {
                val bufferReadResult =
                    audioRecord.read(audioBuffer, 0, blockSize, AudioRecord.READ_NON_BLOCKING)

                var i = 0
                while (i < blockSize && i < bufferReadResult) {
                    toTransform[i] = audioBuffer[i].toDouble() / 32768.0 // signed
                    i++
                }
                // TOOD DO something with this

            }

            audioRecord.stop()
        }*/
    }

    @Composable
    fun Content() {
        var text by remember { mutableStateOf("") }

        Text(pitch.toString(), fontSize = 20.sp)

        Button(onClick = {
            text = if (nodeMcu.connect().isSuccessful) "Connected!" else "Oops"
        }) {

        }
    }

    companion object {
        const val BUFFER_SIZE = 4096
        const val FREQ = 22050
    }
}

