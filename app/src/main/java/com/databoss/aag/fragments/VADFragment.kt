package com.databoss.aag.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import com.konovalov.vad.webrtc.utils.AudioUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class VADFragment : Fragment(R.layout.fragment_vad) {

    private var isMicToggled = false
    private val TAG = "VAD Fragment"

    // config for webrtc-vad
    private val sampleRate = SampleRate.SAMPLE_RATE_16K
    private val frameSize = FrameSize.FRAME_SIZE_320
    private val mode = Mode.VERY_AGGRESSIVE
    private val silenceDurationMs = 300
    private val speechDurationMs = 50

    // config for audiorecord
    private val audioSource: Int = MediaRecorder.AudioSource.MIC
    private val sampleRateInHz: Int = sampleRate.value
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeInBytes: Int = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

    // resources
    private lateinit var audioRecord: AudioRecord

    // mediarecord
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null




    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val micIcon = view.findViewById<ImageView>(R.id.micIcon)

        micIcon.setOnClickListener {

            isMicToggled = !isMicToggled

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 42)
                return@setOnClickListener
            }

            if (isMicToggled) {
                Log.d(TAG, "Listening started")
                listenAudioRecord()

            } else {
                Log.d(TAG, "Listening stopped")
                stopAudioRecord()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun listenMediaRecorder() {
        val outputDir = File(context?.getExternalFilesDir(null), "MediaRecord")
        if (!outputDir.exists()) outputDir.mkdirs()
        outputFile = File(outputDir, "audio_${System.currentTimeMillis()}.mp3").absolutePath


        mediaRecorder = MediaRecorder(requireContext()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)

            prepare()
            start()
        }
    }

    private fun stopMediaRecorder() {
        mediaRecorder?.apply {
            stop()
            release()
            Log.i(TAG, "Audio saved at: $outputFile")
        }
        mediaRecorder = null
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun listenAudioRecord() {

        audioRecord = AudioRecord(
            audioSource,
            sampleRateInHz,
            channelConfig,
            audioFormat,
            bufferSizeInBytes
        )

        audioRecord.startRecording()

        val recordingThread = Thread {
            val audioData = ByteArray(bufferSizeInBytes)
            while (isMicToggled) {
                val bytesRead = audioRecord.read(audioData, 0, audioData.size)
                if (bytesRead > 0) {
                    Log.wtf("AudioRecord", audioData.asList().toString())
                }
            }
        }

        recordingThread.start()

    }

    fun stopAudioRecord() {

    }



}
