package com.alterego.app.domain.usecases

import com.alterego.app.domain.models.Chapter
import com.alterego.app.domain.models.ResetContext
import com.alterego.app.domain.models.ResetEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class JourneyStatsCalculatorTest {

    private val zone = ZoneId.of("UTC")
    private val calculator = JourneyStatsCalculator(zone)
    private val now: Instant = Instant.parse("2026-09-03T12:00:00Z")

    private fun chapter(id: Long, number: Int, startDaysAgo: Long, endDaysAgo: Long? = null) = Chapter(
        id = id,
        commitmentId = 1,
        number = number,
        startedAt = now.minus(startDaysAgo, ChronoUnit.DAYS),
        endedAt = endDaysAgo?.let { now.minus(it, ChronoUnit.DAYS) },
    )

    private fun reset(hour: Int, daysAgo: Long, context: ResetContext? = null) = ResetEvent(
        id = 0,
        commitmentId = 1,
        chapterId = 1,
        occurredAt = now.minus(daysAgo, ChronoUnit.DAYS),
        context = context,
        note = null,
        hourOfDay = hour,
    )

    @Test
    fun `current day counts from chapter start`() {
        val stats = calculator.compute(listOf(chapter(1, 1, startDaysAgo = 16)), emptyList(), now.minus(200, ChronoUnit.DAYS), now)
        assertThat(stats.currentDay).isEqualTo(17)
    }

    @Test
    fun `a reset never reduces lifetime committed days`() {
        val chapters = listOf(
            chapter(1, 1, startDaysAgo = 100, endDaysAgo = 60),
            chapter(2, 2, startDaysAgo = 60, endDaysAgo = 20),
            chapter(3, 3, startDaysAgo = 20),
        )
        val stats = calculator.compute(chapters, listOf(reset(23, 60), reset(1, 20)), now.minus(100, ChronoUnit.DAYS), now)

        // 40 + 40 + 20 days of kept commitment survive both resets.
        assertThat(stats.lifetimeCommittedDays).isEqualTo(100)
        assertThat(stats.chaptersCompleted).isEqualTo(2)
        assertThat(stats.currentDay).isEqualTo(21)
    }

    @Test
    fun `longest chapter reports the best run not the current one`() {
        val chapters = listOf(
            chapter(1, 1, startDaysAgo = 150, endDaysAgo = 104),
            chapter(2, 2, startDaysAgo = 104, endDaysAgo = 100),
            chapter(3, 3, startDaysAgo = 5),
        )
        val stats = calculator.compute(chapters, emptyList(), now.minus(150, ChronoUnit.DAYS), now)
        assertThat(stats.longestChapterDays).isEqualTo(46)
    }

    @Test
    fun `aligned percent is capped at one hundred`() {
        val chapters = listOf(chapter(1, 1, startDaysAgo = 400))
        val stats = calculator.compute(chapters, emptyList(), now.minus(400, ChronoUnit.DAYS), now)
        assertThat(stats.alignedPercentThisYear).isAtMost(100)
    }

    @Test
    fun `most common reset hour and context are surfaced`() {
        val resets = listOf(
            reset(23, 10, ResetContext.BORED),
            reset(23, 20, ResetContext.BORED),
            reset(9, 30, ResetContext.STRESSED),
        )
        val stats = calculator.compute(listOf(chapter(1, 1, startDaysAgo = 5)), resets, now.minus(60, ChronoUnit.DAYS), now)
        assertThat(stats.mostCommonResetHour).isEqualTo(23)
        assertThat(stats.mostCommonResetContext).isEqualTo(ResetContext.BORED)
    }

    @Test
    fun `empty history does not crash and reports zeroes`() {
        val stats = calculator.compute(emptyList(), emptyList(), now, now)
        assertThat(stats.currentDay).isEqualTo(0)
        assertThat(stats.lifetimeCommittedDays).isEqualTo(0)
        assertThat(stats.longestChapterDays).isEqualTo(0)
    }
}
