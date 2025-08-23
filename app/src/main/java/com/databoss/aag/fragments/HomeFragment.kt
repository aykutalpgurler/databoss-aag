package com.databoss.aag.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.databoss.aag.R
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class HomeFragment : Fragment(R.layout.fragment_home) {

    // Create an instance of the LLM Inference task
    lateinit var llmInference: LlmInference


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the configuration options for the LLM Inference task
        val taskOptions: LlmInference.LlmInferenceOptions? = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/llm/gemma3-1b-it-int4.task")
            .setMaxTopK(64)
            .build()

        llmInference = LlmInference.createFromOptions(requireContext(), taskOptions)

        val editText = view.findViewById<EditText>(R.id.messageEditText)
        val sendButton = view.findViewById<ImageButton>(R.id.sendButton)

        var input = ""

        sendButton.setOnClickListener {

            input = editText.text.toString()
//            editText.text.clear()
//            messageBubble.text = input

            val result = llmInference.generateResponse(input)
            Log.d("GEMMA3", result)

        }

//        val result = llmInference.generateResponse(input)
//        messageBubble.text = result




    }

}