package com.alterego.app.core.scheduler

import com.alterego.app.domain.models.QuietHours
import com.alterego.app.domain.models.ReminderIntensity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

/**
 * Decides WHEN today's Moments fire. Pure and deterministic for a given seed so tests are stable.
 * Uses inexact scheduling downstream (Android recommends inexact alarms for friendly reminders).
 */
class MomentPlanner(private val random: Random = Random.Default) {

    data class Plan(val times: List<LocalDateTime>)

    fun plan(
        date: LocalDate,
        intensity: ReminderIntensity,
        customPerDay: Int,
        quietHours: QuietHours,
        now: LocalDateTime,
        highRiskHour: Int? = null,
    ): Plan {
        val count = when (intensity) {
            ReminderIntensity.CUSTOM -> customPerDay.coerceIn(0, 12)
            else -> random.nextInt(intensity.momentsPerDay.first, intensity.momentsPerDay.last + 1)
        }
        if (count == 0) return Plan(emptyList())

        val awake = awakeMinutes(quietHours)
        if (awake.isEmpty()) return Plan(emptyList())

        val slots = spreadEvenly(awake, count).map { minute ->
            val jitter = random.nextInt(-JITTER_MINUTES, JITTER_MINUTES + 1)
            (minute + jitter).coerceIn(awake.first(), awake.last())
        }.toMutableList()

        // One accountability nudge shortly before the user's own high-risk hour, if that hour is awake time.
        if (highRiskHour != null) {
            val target = highRiskHour * 60 - HIGH_RISK_LEAD_MINUTES
            if (target in awake.first()..awake.last() && slots.none { kotlin.math.abs(it - target) < 45 }) slots.add(target)
        }

        val times = slots.sorted().distinct()
            .map { date.atStartOfDay().plusMinutes(it.toLong()) }
            .filter { it.isAfter(now.plusMinutes(MIN_LEAD_MINUTES)) }
        return Plan(times)
    }

    /** Minutes-of-day that are outside quiet hours. */
    fun awakeMinutes(quietHours: QuietHours): List<Int> = (0 until 1440 step 5).filter { m -> !quietHours.contains(LocalTime.ofSecondOfDay(m * 60L)) }

    private fun spreadEvenly(awake: List<Int>, count: Int): List<Int> {
        if (count == 1) return listOf(awake[awake.size / 2])
        val step = (awake.size - 1).toDouble() / (count + 1)
        return (1..count).map { awake[(it * step).toInt().coerceIn(0, awake.size - 1)] }
    }

    companion object {
        const val JITTER_MINUTES = 35
        const val HIGH_RISK_LEAD_MINUTES = 30
        const val MIN_LEAD_MINUTES = 2L
    }
}
