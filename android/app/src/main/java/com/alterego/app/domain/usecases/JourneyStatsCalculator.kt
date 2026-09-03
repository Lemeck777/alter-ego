package com.alterego.app.domain.usecases

import com.alterego.app.domain.models.Chapter
import com.alterego.app.domain.models.JourneyStats
import com.alterego.app.domain.models.ResetContext
import com.alterego.app.domain.models.ResetEvent
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Reflective statistics. Hard rule: lifetime progress never decreases because of one reset. */
class JourneyStatsCalculator(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun compute(chapters: List<Chapter>, resets: List<ResetEvent>, togetherSince: Instant, now: Instant): JourneyStats {
        val open = chapters.firstOrNull { it.isOpen }
        val closed = chapters.filter { !it.isOpen }
        val yearStart = now.atZone(zone).withDayOfYear(1).truncatedTo(ChronoUnit.DAYS).toInstant()

        val lifetimeCommittedDays = chapters.sumOf { it.durationMillis(now) } / Chapter.MILLIS_PER_DAY
        val lifetimeDays = (ChronoUnit.DAYS.between(togetherSince, now)).coerceAtLeast(1)

        val committedThisYear = chapters.sumOf { overlapMillis(it, yearStart, now) } / Chapter.MILLIS_PER_DAY
        val daysThisYear = ChronoUnit.DAYS.between(maxOf(yearStart, togetherSince), now).coerceAtLeast(1)
        val restartsThisYear = resets.count { it.occurredAt >= yearStart }

        val durationsDays = chapters.map { (it.durationMillis(now) / Chapter.MILLIS_PER_DAY).toInt() }
        val hourMode = resets.groupingBy { it.hourOfDay }.eachCount().maxByOrNull { it.value }?.key
        val contextMode = resets.mapNotNull { it.context }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        return JourneyStats(
            currentChapter = open,
            currentDay = open?.dayNumber(now) ?: 0,
            chaptersCompleted = closed.size,
            committedDaysThisYear = committedThisYear.toInt(),
            restartsThisYear = restartsThisYear,
            alignedPercentThisYear = ((committedThisYear * 100) / daysThisYear).toInt().coerceIn(0, 100),
            longestChapterDays = durationsDays.maxOrNull() ?: 0,
            averageChapterDays = if (durationsDays.isEmpty()) 0 else durationsDays.average().toInt(),
            lifetimeCommittedDays = lifetimeCommittedDays.toInt(),
            lifetimeDays = lifetimeDays.toInt(),
            togetherSince = togetherSince,
            daysTogether = lifetimeDays.toInt(),
            mostCommonResetHour = hourMode,
            mostCommonResetContext = contextMode as ResetContext?,
        )
    }

    private fun overlapMillis(chapter: Chapter, from: Instant, to: Instant): Long {
        val start = maxOf(chapter.startedAt, from).toEpochMilli()
        val end = minOf(chapter.endedAt ?: to, to).toEpochMilli()
        return (end - start).coerceAtLeast(0)
    }
}
