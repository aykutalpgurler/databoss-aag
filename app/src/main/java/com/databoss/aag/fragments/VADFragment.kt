package com.databoss.aag.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.databoss.aag.R

class VADFragment : Fragment(R.layout.fragment_vad) {

    private var isMicToggled = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val micIcon = view.findViewById<ImageView>(R.id.micIcon)
        micIcon.setOnClickListener {
            isMicToggled = !isMicToggled
            Log.d("VAD Fragment", "Mic is clicked.")
            if (isMicToggled) Toast.makeText(context, "Listening...", Toast.LENGTH_SHORT).show()
            else Toast.makeText(context, "Stopped", Toast.LENGTH_SHORT).show()
        }
    }
}