package com.databoss.aag.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.databoss.aag.R
import com.databoss.aag.WebRTCTest

class TestFragment : Fragment(R.layout.fragment_test) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val testButton = view.findViewById<Button>(R.id.testButton)

        testButton.setOnClickListener {
            WebRTCTest(requireContext(), "silence.wav").runTest()
        }
    }
}