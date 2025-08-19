package com.databoss.aag.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.databoss.aag.R
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class VADFragment : Fragment(R.layout.fragment_vad) {

    private var isMicToggled = false
    private val TAG = "VAD Fragment"

    // WebRTC VAD config
    private val sampleRate = SampleRate.SAMPLE_RATE_16K
    private val frameSize = FrameSize.FRAME_SIZE_320
    private val mode = Mode.NORMAL
    private val silenceDurationMs = 300
    private val speechDurationMs = 50

    // AudioRecord config
    private val audioSource = android.media.MediaRecorder.AudioSource.MIC
    private val sampleRateInHz = sampleRate.value
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeInBytes = AudioRecord.getMinBufferSize(
        sampleRateInHz, channelConfig, audioFormat
    )

    // Resources
    private var audioRecord: AudioRecord? = null
    private var vad: VadWebRTC? = null
    private var recordingThread: Thread? = null

    // Vosk STT
    private var model: Model? = null
    private var recognizer: Recognizer? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val micIcon = view.findViewById<ImageView>(R.id.micIcon)

        // Load vosk model in a background thread
        Thread {
            try {
//                model = Model("/Users/aykutalpgurler/AndroidStudioProjects/aag/app/src/main/assets/vosk-model-small-tr-0.3")
//                recognizer = Recognizer(model, 16000.0f)
                initVoskModel()
                Log.i(TAG, "Vosk model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load vosk model", e)
            }
        }.start()

        micIcon.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    42
                )
                return@setOnClickListener
            }

            isMicToggled = !isMicToggled

            if (isMicToggled) {
                Log.d(TAG, "Listening started")
                listenAudioRecord()
            } else {
                Log.d(TAG, "Listening stopped")
                stopAudioRecord()
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun listenAudioRecord() {
        audioRecord = AudioRecord(
            audioSource,
            sampleRateInHz,
            channelConfig,
            audioFormat,
            bufferSizeInBytes
        )

        vad = VadWebRTC(
            sampleRate = sampleRate,
            frameSize = frameSize,
            mode = mode,
            silenceDurationMs = silenceDurationMs,
            speechDurationMs = speechDurationMs
        )

        val chunkSize = frameSize.value * 2
        val pendingBytes = mutableListOf<Byte>()
        var allSpeechData = byteArrayOf()
        var allDataChunks = byteArrayOf()
        val rawAudioData = ByteArrayOutputStream()

//        if (model == null || recognizer == null) {
//            initVoskModel()
//        }

        audioRecord?.startRecording()

        recordingThread = Thread {
            val audioData = ByteArray(bufferSizeInBytes)
            while (isMicToggled && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord?.read(audioData, 0, audioData.size) ?: 0
                if (bytesRead > 0) {
                    rawAudioData.write(audioData, 0, bytesRead)
                    for (i in 0 until bytesRead) pendingBytes.add(audioData[i])
                }
                while (pendingBytes.size >= chunkSize) {
                    val chunk = pendingBytes.subList(0, chunkSize).toByteArray()
                    allDataChunks += chunk
                    pendingBytes.subList(0, chunkSize).clear()

                    val isSpeech = vad?.isSpeech(chunk) ?: false
                    requireActivity().runOnUiThread {
                        val micIcon = view?.findViewById<ImageView>(R.id.micIcon)
                        if (!isMicToggled) {
                            micIcon?.clearColorFilter()
                            micIcon?.setImageResource(R.drawable.mic_off_24px)
                            return@runOnUiThread
                        }
                        if (isSpeech) {
                            micIcon?.setColorFilter(
                                resources.getColor(R.color.active, null)
                            )
                            allSpeechData += chunk

                            // Speech-to-Text
                            recognizer?.let { rec ->
                                val hasFinal = rec.acceptWaveForm(chunk, chunk.size)
                                val text = if (hasFinal) {
                                    JSONObject(rec.result).getString("text")
                                } else {
                                    JSONObject(rec.partialResult).optString("partial")
                                }
//                                Log.i(TAG, "Recognized: $text")
                                if (text.isNotEmpty()) {
                                    Log.i(TAG, "Recognized: $text")
                                }
                            }

                        } else {
                            micIcon?.clearColorFilter()
                            micIcon?.setImageResource(R.drawable.mic_24px)
                        }
                    }
                }
            }
            save(rawAudioData.toByteArray(), "raw_data", "RTVAD")
            save(allSpeechData, "speech_chunks", "RTVAD")
            save(allDataChunks, "all_chunks", "RTVAD")

//            // Speech-to-Text
//            recognizer?.let { rec ->
//                val hasFinal = rec.acceptWaveForm(allSpeechData, allSpeechData.size)
//                val text = if (hasFinal) {
//                    JSONObject(rec.result).getString("text")
//                } else {
//                    JSONObject(rec.partialResult).optString("partial")
//                }
//                Log.i(TAG, "Final Text: $text")
////                                if (text.isNotEmpty()) {
////                                    Log.i(TAG, "Recognized: $text")
////                                }
//            }

        }

        recordingThread?.start()
//        rawAudioData.close()
    }

    private fun stopAudioRecord() {
        isMicToggled = false

        // Stop thread
        recordingThread?.join()
        recordingThread = null

        // Stop and release AudioRecord
        audioRecord?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord", e)
            }
        }
        audioRecord = null

        // Close VAD
        vad?.close()
        vad = null

//        // Close Vosk
//        recognizer?.close()
//        recognizer = null
//        model?.close()
//        model = null

        Log.d(TAG, "AudioRecord, VAD released.")
    }

    override fun onPause() {
        super.onPause()
        if (isMicToggled) {
            stopAudioRecord()
            // Close Vosk
            recognizer?.close()
            recognizer = null
            model?.close()
            model = null
            Log.d("FragmentPause", "Vosk released")

        }
    }

//    override fun onStop() {
//        super.onStop()
//        if (isMicToggled) {
//            stopAudioRecord()
//            // Close Vosk
//            recognizer?.close()
//            recognizer = null
//            model?.close()
//            model = null
//            Log.d("FragmentStop", "Vosk released")
//
//        }
//    }

    private fun createWavHeader(
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = totalAudioLen + 36
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen.toInt() and 0xff).toByte()
        header[5] = ((totalDataLen.toInt() shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen.toInt() shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen.toInt() shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[20] = 1
        header[22] = channels.toByte()
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        header[32] = (blockAlign.toInt() and 0xff).toByte()
        header[33] = ((blockAlign.toInt() shr 8) and 0xff).toByte()
        header[34] = bitsPerSample.toByte()
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen.toInt() and 0xff).toByte()
        header[41] = ((totalAudioLen.toInt() shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen.toInt() shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen.toInt() shr 24) and 0xff).toByte()
        return header
    }

    private fun save(data: ByteArray, fileName: String, dirName: String) {
        val outputDir = File(requireContext().getExternalFilesDir(null), dirName)
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "$fileName.wav")
        val audioHeader = createWavHeader(
            data.size.toLong(),
            sampleRate.value,
            1
        )
        Log.i(TAG, "Speech segments are saved to: ${file.absolutePath}")
        FileOutputStream(file).use { outputStream ->
            outputStream.write(audioHeader)
            outputStream.write(data)
        }
    }

    // generated with Gemini 2.5 pro
    private fun initVoskModel() {
        Thread {
            // StorageService.unpack() will copy the folder from your assets
            // to the app's internal storage and return the absolute path to it.
            StorageService.unpack(requireContext(), "vosk-model-small-tr-0.3", "model",
                { model ->
                    this.model = model
                    this.recognizer = Recognizer(model, 16000.0f)
                    Log.i(TAG, "Vosk model loaded successfully from internal storage.")

                    // You can enable UI elements here if needed
                    // requireActivity().runOnUiThread { micIcon.isEnabled = true }

                },
                { exception ->
                    Log.e(TAG, "Failed to load vosk model", exception)
                })
        }.start()
    }

}
