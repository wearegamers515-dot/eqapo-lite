package com.example.eqapolite

/**
 * Parses a subset of EqualizerAPO filter syntax, e.g.:
 *   Filter 1: ON PK Fc 100 Hz Gain 3.0 dB Q 1.00
 *   Filter 2: ON LSC Fc 60 Hz Gain 4.0 dB Q 0.71
 * Also supports single-line GraphicEQ format:
 *   GraphicEQ: 20 0.0; 100 3.0; 1000 -2.0; 10000 0.0
 */
data class Band(val freqHz: Int, val gainDb: Float)

object EqApoParser {

    private val filterLineRegex = Regex(
        """Filter\s+\d+:\s*ON\s+\w+\s+Fc\s+([\d.]+)\s*Hz\s+Gain\s+(-?[\d.]+)\s*dB""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String): List<Band> {
        val trimmed = text.trim()

        // GraphicEQ single-line format
        if (trimmed.startsWith("GraphicEQ", ignoreCase = true)) {
            val pairs = trimmed.substringAfter(":").split(";")
            return pairs.mapNotNull { pair ->
                val parts = pair.trim().split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val freq = parts[0].toFloatOrNull()?.toInt()
                    val gain = parts[1].toFloatOrNull()
                    if (freq != null && gain != null) Band(freq, gain) else null
                } else null
            }
        }

        // Standard multi-line Filter N: ON PK Fc ... Gain ... format
        return trimmed.lines().mapNotNull { line ->
            val match = filterLineRegex.find(line) ?: return@mapNotNull null
            val freq = match.groupValues[1].toFloatOrNull()?.toInt() ?: return@mapNotNull null
            val gain = match.groupValues[2].toFloatOrNull() ?: return@mapNotNull null
            Band(freq, gain)
        }
    }
}
