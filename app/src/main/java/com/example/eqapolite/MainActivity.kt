package com.example.eqapolite

import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: AppCompatTextView
    private val receiver = AudioSessionReceiver()

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        contentResolver.openInputStream(uri)?.use { stream ->
            val text = stream.bufferedReader().readText()
            val bands = EqApoParser.parse(text)
            if (bands.isEmpty()) {
                Toast.makeText(this, "No filters recognized in that file", Toast.LENGTH_LONG).show()
                return@use
            }
            EqAppState.currentBands = bands
            EqAppState.controller.applyBands(bands)
            statusText.text = buildString {
                append("Loaded ${bands.size} bands:\n")
                bands.forEach { append("  ${it.freqHz} Hz -> ${it.gainDb} dB\n") }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = androidx.appcompat.widget.LinearLayoutCompat(this).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val loadButton = AppCompatButton(this).apply {
            text = "Load EQ APO / GraphicEQ filter file"
            setOnClickListener { pickFile.launch(arrayOf("text/plain")) }
        }

        statusText = AppCompatTextView(this).apply {
            text = "No filter loaded yet.\n\n" +
                "Note: this EQ only applies to apps that hand off their " +
                "audio session (ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION). " +
                "It cannot process another app's microphone input " +
                "(e.g. Discord's outgoing voice) without root -- that needs " +
                "a virtual audio driver like RootlessJamesDSP uses."
            setPadding(0, 48, 0, 0)
        }

        layout.addView(loadButton)
        layout.addView(statusText)
        setContentView(layout)

        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        EqAppState.controller.release()
    }
}
