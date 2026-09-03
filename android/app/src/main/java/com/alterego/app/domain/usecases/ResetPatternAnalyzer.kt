package com.alterego.app.domain.usecases

import com.alterego.app.domain.models.ResetContext
import com.alterego.app.domain.models.ResetEvent

/**
 * Learns from resets without pretending to read minds.
 *
 * If a clear majority of resets cluster in one part of the day, and there are enough of them for
 * that to mean anything, we surface a gentle heads-up shortly before that window. We never claim
 * to have detected an emotion; we only report the user's own recorded pattern back to them.
 */
class ResetPatternAnalyzer {

    data class Pattern(
        val highRiskHour: Int?,
        val highRiskShare: Double,
        val dominantContext: ResetContext?,
        val dominantContextShare: Double,
        val sampleSize: Int,
    ) {
        val isMeaningful: Boolean
            get() = sampleSize >= MIN_SAMPLE && highRiskHour != null && highRiskShare >= MIN_SHARE

        /** The line the persona says before a known-difficult window. Null when we lack the data to say it. */
        fun headsUpText(): String? {
            if (!isMeaningful) return null
            val tail = when (dominantContext) {
                ResetContext.SOCIAL_MEDIA, ResetContext.BORED -> "Put the phone away early tonight?"
                ResetContext.COULDNT_SLEEP -> "An earlier night might do more than willpower."
                ResetContext.STRESSED -> "Breathe first. Decide later."
                ResetContext.LONELY -> "Message someone real."
                ResetContext.PORN -> "Decide now where the phone sleeps."
                else -> "Change what your body is doing."
            }
            return "This is usually where things become difficult. $tail"
        }
    }

    fun analyze(resets: List<ResetEvent>): Pattern {
        if (resets.isEmpty()) return Pattern(null, 0.0, null, 0.0, 0)

        // Group into 3-hour windows so 23:00 and 00:30 count as the same difficult stretch.
        val windowCounts = resets.groupingBy { windowOf(it.hourOfDay) }.eachCount()
        val (window, count) = windowCounts.maxByOrNull { it.value } ?: return Pattern(null, 0.0, null, 0.0, resets.size)
        val contextCounts = resets.mapNotNull { it.context }.groupingBy { it }.eachCount()
        val dominant = contextCounts.maxByOrNull { it.value }

        return Pattern(
            highRiskHour = centerHour(window),
            highRiskShare = count.toDouble() / resets.size,
            dominantContext = dominant?.key,
            dominantContextShare = dominant?.let { it.value.toDouble() / resets.size } ?: 0.0,
            sampleSize = resets.size,
        )
    }

    /** Shifted so late night (22:00-00:59) falls inside one window instead of splitting at midnight. */
    private fun windowOf(hour: Int): Int = ((hour + 2) % 24) / 3

    private fun centerHour(window: Int): Int = ((window * 3) - 1 + 24) % 24

    companion object {
        const val MIN_SAMPLE = 4
        const val MIN_SHARE = 0.5
    }
}
