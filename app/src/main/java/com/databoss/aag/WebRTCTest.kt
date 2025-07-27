package com.databoss.aag

import android.content.Context
import android.util.Log
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.SampleRate
import com.konovalov.vad.webrtc.config.Mode
import java.io.File
import java.io.FileOutputStream
import java.util.logging.Logger
import kotlin.system.measureNanoTime


// Issue: first chunks are false positive:
// classified as speech although they are silence
class WebRTCTest(private val context: Context, private val fileName: String) {

    // use { ... }: This is a Kotlin scope function that ensures the vadWebRTC resources
    // are automatically closed when the block is finished, preventing memory leaks.
    fun runTest() {
        VadWebRTC(
            sampleRate = SampleRate.SAMPLE_RATE_16K,
            frameSize = FrameSize.FRAME_SIZE_320,
            mode = Mode.VERY_AGGRESSIVE,
            silenceDurationMs = 300,
            speechDurationMs = 50
        ).use { vadWebRTC ->
            // PCM 16 bit encoding (2 bytes)
            // each frame has 320 samples
            // 320 * 2 = 640
            // feeding vad with one frame of audio as ByteArray of 640 bytes
            // tested with multiply factor of 1 and vad does not work properly
            val chunkSize = vadWebRTC.frameSize.value * 2

            context.assets.open(fileName).buffered().use { input ->
                val audioHeader = ByteArray(44).apply { input.read(this) }
                // first 44 bytes are universally alloc for header
                // header will be used in generated wav

                var speechData = byteArrayOf()
                var allSpeechData = byteArrayOf()

                while (input.available() > 0) {
                    val frameChunk = ByteArray(chunkSize).apply { input.read(this) }
                    // code is eq to two lines below
                    // val buffer = ByteArray(chunkSize)
                    // input.read(buffer)

                    if (vadWebRTC.isSpeech(frameChunk)) {
//                        Log.d("vad",frameChunk.asList().size.toString())
                        Log.d("vad",frameChunk.asList().toString())
                        allSpeechData += frameChunk
                        speechData += frameChunk
                        Log.d("WebRTC", "speech detected")
                    } else {
                        if (speechData.isNotEmpty()) {
                            val timestamp = System.currentTimeMillis() // unique

                            val outputDir = File(context.getExternalFilesDir(null), "VAD")
                            if (!outputDir.exists()) outputDir.mkdirs()
                            val speechFile = File(outputDir, "speech_$timestamp.wav")

                            Log.i("WebRTC", "file is saved ${speechFile.absolutePath}")
                            FileOutputStream(speechFile).use { outputStream ->
                                outputStream.write(audioHeader)
                                outputStream.write(speechData)
                            }
                            speechData = byteArrayOf()
                        }
                    }
//                    Log.i("WebRTC", "speech data size: ${speechData.size}")

                }
                val outputDir = File(context.getExternalFilesDir(null), "VAD")
                if (!outputDir.exists()) outputDir.mkdirs()
                val allSpeechFile = File(outputDir, "all_speech.wav")
                Log.i("WebRTC", "Speech segments are saved to: ${allSpeechFile.absolutePath}")
                FileOutputStream(allSpeechFile).use { outputStream ->
                    outputStream.write(audioHeader)
                    outputStream.write(allSpeechData)
                }
            }
        }
    }
}