package com.alterego.app.domain.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ChapterTest {

    private val start: Instant = Instant.parse("2026-09-01T08:00:00Z")

    private fun chapter(endedAt: Instant? = null) = Chapter(1, 1, 1, start, endedAt)

    @Test
    fun `day one starts at one not zero`() {
        assertThat(chapter().dayNumber(start)).isEqualTo(1)
        assertThat(chapter().dayNumber(start.plus(23, ChronoUnit.HOURS))).isEqualTo(1)
        assertThat(chapter().dayNumber(start.plus(24, ChronoUnit.HOURS))).isEqualTo(2)
    }

    @Test
    fun `a closed chapter stops counting at its end`() {
        val end = start.plus(10, ChronoUnit.DAYS)
        val closed = chapter(endedAt = end)
        val muchLater = end.plus(100, ChronoUnit.DAYS)
        assertThat(closed.dayNumber(muchLater)).isEqualTo(11)
        assertThat(closed.isOpen).isFalse()
    }

    @Test
    fun `a clock that moves backwards cannot produce negative time`() {
        val before = start.minus(5, ChronoUnit.DAYS)
        assertThat(chapter().durationMillis(before)).isEqualTo(0)
        assertThat(chapter().dayNumber(before)).isEqualTo(1)
    }
}
