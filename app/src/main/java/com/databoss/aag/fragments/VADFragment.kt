package com.databoss.aag.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.databoss.aag.R
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import com.konovalov.vad.webrtc.utils.AudioUtils
import java.util.concurrent.atomic.AtomicBoolean

class VADFragment : Fragment(R.layout.fragment_vad) {

    private var isMicToggled = false
    private val TAG = "VAD Fragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val micIcon = view.findViewById<ImageView>(R.id.micIcon)

        micIcon.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 42)
                return@setOnClickListener
            }

            isMicToggled = !isMicToggled

            if (isMicToggled) {
                Log.d(TAG, "Listening started")

            } else {
                Log.d(TAG, "Listening stopped")

            }
        }
    }
}
