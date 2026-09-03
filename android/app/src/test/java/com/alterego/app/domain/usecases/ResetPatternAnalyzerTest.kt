package com.alterego.app.domain.usecases

import com.alterego.app.domain.models.ResetContext
import com.alterego.app.domain.models.ResetEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class ResetPatternAnalyzerTest {

    private val analyzer = ResetPatternAnalyzer()

    private fun reset(hour: Int, context: ResetContext? = null) = ResetEvent(
        id = 0, commitmentId = 1, chapterId = 1, occurredAt = Instant.now(), context = context, note = null, hourOfDay = hour,
    )

    @Test
    fun `no resets means no pattern`() {
        val pattern = analyzer.analyze(emptyList())
        assertThat(pattern.isMeaningful).isFalse()
        assertThat(pattern.headsUpText()).isNull()
    }

    @Test
    fun `a small sample is never treated as a pattern`() {
        val pattern = analyzer.analyze(listOf(reset(23), reset(0), reset(23)))
        assertThat(pattern.sampleSize).isEqualTo(3)
        assertThat(pattern.isMeaningful).isFalse()
    }

    @Test
    fun `late night resets across midnight group together`() {
        val resets = listOf(reset(23), reset(0), reset(23), reset(1), reset(9))
        val pattern = analyzer.analyze(resets)
        assertThat(pattern.isMeaningful).isTrue()
        assertThat(pattern.highRiskShare).isAtLeast(0.5)
    }

    @Test
    fun `scattered resets do not produce a false pattern`() {
        val resets = listOf(reset(2), reset(7), reset(11), reset(15), reset(19), reset(22))
        val pattern = analyzer.analyze(resets)
        assertThat(pattern.isMeaningful).isFalse()
    }

    @Test
    fun `the dominant context shapes the advice`() {
        val resets = listOf(
            reset(23, ResetContext.SOCIAL_MEDIA),
            reset(0, ResetContext.SOCIAL_MEDIA),
            reset(23, ResetContext.SOCIAL_MEDIA),
            reset(1, ResetContext.BORED),
        )
        val pattern = analyzer.analyze(resets)
        assertThat(pattern.dominantContext).isEqualTo(ResetContext.SOCIAL_MEDIA)
        assertThat(pattern.headsUpText()).contains("Put the phone away")
    }

    @Test
    fun `tiredness advice points at sleep rather than willpower`() {
        val resets = List(5) { reset(1, ResetContext.COULDNT_SLEEP) }
        val pattern = analyzer.analyze(resets)
        assertThat(pattern.headsUpText()).contains("earlier night")
    }

    @Test
    fun `advice never blames the person`() {
        val resets = List(6) { reset(23, ResetContext.STRESSED) }
        val text = analyzer.analyze(resets).headsUpText().orEmpty()
        listOf("fail", "weak", "should have", "again").forEach { banned ->
            assertThat(text.lowercase()).doesNotContain(banned)
        }
    }
}
