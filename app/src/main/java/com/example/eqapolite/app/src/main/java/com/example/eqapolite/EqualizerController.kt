package com.example.eqapolite

import android.media.audiofx.Equalizer
import android.util.Log
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Wraps Android's built-in Equalizer audio effect.
 *
 * IMPORTANT LIMITATION: Android's Equalizer effect only attaches to a specific
 * audio session (i.e. one app's playback stream). It cannot be force-attached
 * to another app's *microphone input* without root or a virtual audio driver
 * (that's what RootlessJamesDSP does under the hood). This controller is
 * therefore only useful for output-side EQ (shaping what YOU hear), not for
 * boosting/EQ'ing your mic before it reaches Discord.
 */
class EqualizerController {

    private var equalizer: Equalizer? = null

    fun attach(audioSessionId: Int) {
        release()
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Failed to attach to session $audioSessionId", e)
        }
    }

    fun applyBands(bands: List<Band>) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands
        if (numBands.toInt() == 0 || bands.isEmpty()) return

        // Android's Equalizer effect only exposes a handful of fixed bands
        // (commonly 5), each pinned to a specific center frequency. We can't
        // add arbitrary bands like a real EQ APO filter chain -- instead we
        // find, for each hardware band, the closest band from the parsed
        // filter and use its gain.
        for (i in 0 until numBands) {
            val bandIndex = i.toShort()
            val centerFreqMilliHz = eq.getCenterFreq(bandIndex) // in mHz
            val centerFreqHz = centerFreqMilliHz / 1000

            val closest = bands.minByOrNull { abs(it.freqHz - centerFreqHz) } ?: continue

            val range = eq.bandLevelRange // [minMilliBel, maxMilliBel]
            val minDb = range[0] / 100f
            val maxDb = range[1] / 100f
            val clampedGainDb = closest.gainDb.coerceIn(minDb, maxDb)

            val levelMilliBel = (clampedGainDb * 100).roundToInt().toShort()
            try {
                eq.setBandLevel(bandIndex, levelMilliBel)
            } catch (e: Exception) {
                Log.e("EqualizerController", "setBandLevel failed for band $i", e)
            }
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}
