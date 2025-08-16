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
import java.io.File
import java.io.FileOutputStream

class VADFragment : Fragment(R.layout.fragment_vad) {

    private var isMicToggled = false
    private val TAG = "VAD Fragment"

    // WebRTC VAD config
    private val sampleRate = SampleRate.SAMPLE_RATE_16K
    private val frameSize = FrameSize.FRAME_SIZE_320
    private val mode = Mode.VERY_AGGRESSIVE
    private val silenceDurationMs = 10
    private val speechDurationMs = 100

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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val micIcon = view.findViewById<ImageView>(R.id.micIcon)

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
//                micIcon.setImageResource(R.drawable.mic_24px)
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

        audioRecord?.startRecording()

        recordingThread = Thread {
            val audioData = ByteArray(bufferSizeInBytes)
            while (isMicToggled && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord?.read(audioData, 0, audioData.size) ?: 0
                if (bytesRead > 0) {
                    for (i in 0 until bytesRead) pendingBytes.add(audioData[i])
                }
                Log.d("Pending Bytes", "${pendingBytes.size} - $pendingBytes")
                while (pendingBytes.size >= chunkSize) {
                    val chunk = pendingBytes.subList(0, chunkSize).toByteArray()
                    allDataChunks += chunk
//                    Log.d("Chunk", "${chunk.size} - ${chunk.asList()}")
                    pendingBytes.subList(0, chunkSize).clear()

                    val isSpeech = vad?.isSpeech(chunk) ?: false
//                    Log.d("VAD", "Speech detected $isSpeech")
                    Log.d("Chunk", "$isSpeech - ${chunk.asList()}")

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
                        } else {
                            micIcon?.clearColorFilter()
                            micIcon?.setImageResource(R.drawable.mic_24px)
                        }
                    }
                }
            }
            save(audioData, "raw_data", "RTVAD")
            save(allSpeechData, "speech_chunks", "RTVAD")
            save(allDataChunks, "all_chunks", "RTVAD")
        }

        recordingThread?.start()
    }

    private fun stopAudioRecord() {
        isMicToggled = false

        // Stop thread
        recordingThread?.join()
        recordingThread = null


//        val micIcon = view?.findViewById<ImageView>(R.id.micIcon)
//        micIcon?.clearColorFilter()
//        micIcon?.setImageResource(R.drawable.mic_off_24px)

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

        Log.d(TAG, "AudioRecord and VAD released.")
    }

    override fun onStop() {
        super.onStop()
        if (isMicToggled) {
            stopAudioRecord()
        }
    }

    private fun createWavHeader(
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = totalAudioLen + 36

        val header = ByteArray(44)

        // RIFF/WAVE header
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

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
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
        header[35] = 0

        // data chunk
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
        // Save collected speech data
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
}

