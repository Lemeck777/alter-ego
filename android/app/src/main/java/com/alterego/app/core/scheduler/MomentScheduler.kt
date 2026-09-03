package com.alterego.app.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alterego.app.core.data.CustomContentRepository
import com.alterego.app.core.data.ScheduledReminder
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.models.MomentTrigger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a day's plan into Android alarms.
 *
 * Friendly nudges use inexact alarms, which is what Android recommends for anything that does not
 * need second-perfect timing. Only reminders the user explicitly asked to be exact ("6:00 AM, pray")
 * use exact alarms, and only when the OS has granted that ability.
 */
@Singleton
class MomentScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
    private val customContent: CustomContentRepository,
    private val planner: MomentPlanner,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Plans the rest of today and arms the alarms. Safe to call repeatedly; it clears first. */
    suspend fun planToday(highRiskHour: Int? = null): Int {
        val p = prefs.snapshot()
        cancelPlannedMoments()
        val now = LocalDateTime.now()
        val plan = planner.plan(
            date = LocalDate.now(),
            intensity = p.intensity,
            customPerDay = p.customMomentsPerDay,
            quietHours = p.quietHours,
            now = now,
            highRiskHour = highRiskHour,
        )
        plan.times.forEachIndexed { index, time ->
            scheduleInexact(requestCode = MOMENT_REQUEST_BASE + index, at = time, trigger = MomentTrigger.RANDOM)
        }
        prefs.update { it.copy(lastPlannedDay = LocalDate.now().toString()) }
        return plan.times.size
    }

    /** Re-arms every enabled user-created reminder. */
    suspend fun rescheduleUserReminders() {
        customContent.enabledReminders().forEach { schedule(it) }
    }

    fun schedule(reminder: ScheduledReminder) {
        val next = nextOccurrence(reminder.hour, reminder.minute)
        val pi = pendingIntent(REMINDER_REQUEST_BASE + reminder.id.toInt(), MomentTrigger.SCHEDULED, reminder.id)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (reminder.exact && canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            // Inexact window: Android batches this with other wakeups to save battery.
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, millis, INEXACT_WINDOW_MILLIS, pi)
        }
    }

    fun cancel(reminderId: Long) {
        alarmManager.cancel(pendingIntent(REMINDER_REQUEST_BASE + reminderId.toInt(), MomentTrigger.SCHEDULED, reminderId))
    }

    fun cancelPlannedMoments() {
        repeat(MAX_MOMENTS_PER_DAY) { index ->
            alarmManager.cancel(pendingIntent(MOMENT_REQUEST_BASE + index, MomentTrigger.RANDOM, -1L))
        }
    }

    private fun scheduleInexact(requestCode: Int, at: LocalDateTime, trigger: MomentTrigger) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            millis,
            INEXACT_WINDOW_MILLIS,
            pendingIntent(requestCode, trigger, -1L),
        )
    }

    private fun nextOccurrence(hour: Int, minute: Int): LocalDateTime {
        val now = LocalDateTime.now()
        val today = now.toLocalDate().atTime(hour, minute)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun pendingIntent(requestCode: Int, trigger: MomentTrigger, reminderId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, MomentAlarmReceiver::class.java)
                .setAction(MomentAlarmReceiver.ACTION_FIRE)
                .putExtra(MomentAlarmReceiver.EXTRA_TRIGGER, trigger.id)
                .putExtra(MomentAlarmReceiver.EXTRA_REMINDER_ID, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val MOMENT_REQUEST_BASE = 1_000
        const val REMINDER_REQUEST_BASE = 2_000
        const val MAX_MOMENTS_PER_DAY = 12
        const val INEXACT_WINDOW_MILLIS = 20L * 60 * 1000
    }
}
