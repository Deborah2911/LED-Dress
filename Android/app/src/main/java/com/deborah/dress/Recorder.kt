package com.deborah.dress

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import kotlin.concurrent.thread

class Recorder(private val ctx: Context) {

    @Suppress("DEPRECATION")
    private val recorder = MediaRecorder()
    @Volatile
    private var running = false

    private var recorderThread: Thread? = null

    fun start(callback: (amplitude: Int) -> Unit) {
        running = true
        recorderThread = thread {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(File(ctx.cacheDir, "temp.wav"))
            recorder.prepare()
            recorder.start()

            while (running) {
                Thread.sleep(400)
                val maxAmplitude = recorder.maxAmplitude

                Log.i("Amplitude", maxAmplitude.toString())
                callback(maxAmplitude)
            }
        }
    }

    fun stop() {
        running = false
    }
}