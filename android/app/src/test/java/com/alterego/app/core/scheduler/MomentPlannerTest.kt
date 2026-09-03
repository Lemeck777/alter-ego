package com.alterego.app.core.scheduler

import com.alterego.app.domain.models.QuietHours
import com.alterego.app.domain.models.ReminderIntensity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

class MomentPlannerTest {

    private val planner = MomentPlanner(Random(7))
    private val date: LocalDate = LocalDate.of(2026, 9, 3)
    private val startOfDay: LocalDateTime = date.atStartOfDay()

    @Test
    fun `nothing is ever scheduled inside quiet hours`() {
        val quiet = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        repeat(50) {
            val plan = MomentPlanner(Random(it)).plan(
                date = date,
                intensity = ReminderIntensity.STRONG,
                customPerDay = 6,
                quietHours = quiet,
                now = startOfDay,
            )
            plan.times.forEach { time ->
                assertThat(quiet.contains(time.toLocalTime())).isFalse()
            }
        }
    }

    @Test
    fun `quiet hours crossing midnight are handled`() {
        val quiet = QuietHours(LocalTime.of(23, 0), LocalTime.of(6, 0))
        assertThat(quiet.contains(LocalTime.of(23, 30))).isTrue()
        assertThat(quiet.contains(LocalTime.of(2, 0))).isTrue()
        assertThat(quiet.contains(LocalTime.of(5, 59))).isTrue()
        assertThat(quiet.contains(LocalTime.of(6, 0))).isFalse()
        assertThat(quiet.contains(LocalTime.of(22, 0))).isFalse()
    }

    @Test
    fun `intensity controls how many times are planned`() {
        val quiet = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        val gentle = planner.plan(date, ReminderIntensity.GENTLE, 0, quiet, startOfDay).times
        val strong = planner.plan(date, ReminderIntensity.STRONG, 0, quiet, startOfDay).times
        assertThat(gentle.size).isAtMost(ReminderIntensity.GENTLE.momentsPerDay.last + 1)
        assertThat(strong.size).isAtLeast(ReminderIntensity.STRONG.momentsPerDay.first - 1)
        assertThat(strong.size).isAtLeast(gentle.size)
    }

    @Test
    fun `custom intensity honours the exact count`() {
        val quiet = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        val plan = planner.plan(date, ReminderIntensity.CUSTOM, customPerDay = 4, quiet, startOfDay)
        assertThat(plan.times.size).isAtMost(5)
        assertThat(plan.times).isNotEmpty()
    }

    @Test
    fun `zero custom moments plans nothing`() {
        val quiet = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        val plan = planner.plan(date, ReminderIntensity.CUSTOM, customPerDay = 0, quiet, startOfDay)
        assertThat(plan.times).isEmpty()
    }

    @Test
    fun `times already past are dropped`() {
        val quiet = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        val lateAfternoon = date.atTime(17, 0)
        val plan = planner.plan(date, ReminderIntensity.STRONG, 0, quiet, lateAfternoon)
        plan.times.forEach { assertThat(it).isGreaterThan(lateAfternoon) }
    }

    @Test
    fun `a known difficult hour gets a nudge shortly before it`() {
        val quiet = QuietHours(LocalTime.of(23, 30), LocalTime.of(7, 0))
        val plan = planner.plan(date, ReminderIntensity.GENTLE, 0, quiet, startOfDay, highRiskHour = 22)
        val target = date.atTime(21, 30)
        assertThat(plan.times.any { !it.isBefore(target.minusMinutes(20)) && !it.isAfter(target.plusMinutes(20)) }).isTrue()
    }

    @Test
    fun `planned times are ordered and unique`() {
        val quiet = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        val plan = planner.plan(date, ReminderIntensity.STRONG, 0, quiet, startOfDay)
        assertThat(plan.times).isInOrder()
        assertThat(plan.times).containsNoDuplicates()
    }

    @Test
    fun `all-day quiet hours plan nothing rather than crashing`() {
        val alwaysQuiet = QuietHours(LocalTime.of(0, 0), LocalTime.of(0, 0))
        val plan = planner.plan(date, ReminderIntensity.BALANCED, 0, alwaysQuiet, startOfDay)
        assertThat(plan.times).isEmpty()
    }
}
