package com.example.eqapolite

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

/**
 * Listens for apps handing off their playback session for effect processing.
 * This fires when a media app (that supports it) starts/stops playback and
 * broadcasts ACTION_OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION. Not all apps do
 * this (Discord's voice-call audio path may not), so treat this as best-effort.
 */
class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        if (sessionId == -1) return

        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                EqAppState.controller.attach(sessionId)
                EqAppState.controller.applyBands(EqAppState.currentBands)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                EqAppState.controller.release()
            }
        }
    }
}

/** Simple in-memory shared state between the receiver and the activity. */
object EqAppState {
    val controller = EqualizerController()
    var currentBands: List<Band> = emptyList()
}
