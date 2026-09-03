package com.alterego.app.core.scheduler

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.content.MomentSelector
import com.alterego.app.core.data.CustomContentRepository
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.notifications.MomentNotifier
import com.alterego.app.core.notifications.NotificationChannels
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.TimeContext
import com.alterego.app.domain.usecases.ResetPatternAnalyzer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Chooses and delivers one Moment. Everything that decides "which message, right now" runs here so
 * the selection rules live in one testable place.
 */
@HiltWorker
class DeliverMomentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferencesRepository,
    private val content: ContentRepository,
    private val customContent: CustomContentRepository,
    private val journey: JourneyRepository,
    private val notifier: MomentNotifier,
    private val scheduler: MomentScheduler,
    private val analytics: Analytics,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val p = prefs.snapshot()
        if (!p.onboarded) return Result.success()

        val now = LocalTime.now()
        // Never speak during quiet hours, whatever the alarm says.
        if (p.quietHours.contains(now)) return Result.success()
        if (!notifier.hasPermission()) return Result.success()

        content.ensureLoaded()

        val triggerId = inputData.getString(KEY_TRIGGER) ?: MomentTrigger.RANDOM.id
        val trigger = MomentTrigger.fromId(triggerId)
        val persona = content.persona(p.personaId) ?: return Result.success()
        val commitment = journey.primaryCommitment()
        val chapter = commitment?.let { journey.openChapter(it.id) }
        val dayNumber = chapter?.dayNumber(Instant.now()) ?: 0

        val pattern = ResetPatternAnalyzer().analyze(journey.allResets())
        val currentHour = now.hour
        val riskHour = pattern.highRiskHour
        val isHighRisk = pattern.isMeaningful && riskHour != null && kotlin.math.abs(currentHour - riskHour) <= 1

        val selector = MomentSelector()
        val moment = selector.select(
            library = content.allMoments(),
            request = MomentSelector.Request(
                personaId = p.personaId,
                goals = p.goals,
                ageBand = p.ageBand?.id,
                timeContext = TimeContext.forHour(currentHour),
                trigger = if (isHighRisk) MomentTrigger.HIGH_RISK_WINDOW else trigger,
                isPlus = false,
                faithEnabled = p.faithEnabled,
                recentMomentIds = customContent.recentMomentIds(RECENCY_WINDOW_MILLIS),
                personalQuotes = customContent.personalQuoteMoments(p.personaId),
                isHighRiskWindow = isHighRisk,
            ),
        ) ?: return Result.success()

        val deliveryId = customContent.recordDelivery(moment.id, trigger)
        val channel = if (trigger == MomentTrigger.SCHEDULED) NotificationChannels.SCHEDULED else NotificationChannels.MOMENTS
        notifier.notifyMoment(
            deliveryId = deliveryId,
            momentId = moment.id,
            personaName = persona.name,
            privacy = p.notificationPrivacy,
            dayNumber = dayNumber,
            channel = channel,
        )
        analytics.track(
            LocalAnalytics.MOMENT_DELIVERED,
            mapOf("category" to moment.category.id, "trigger" to trigger.id, "persona" to p.personaId),
        )

        // A user reminder fires once; re-arm it for tomorrow.
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        if (reminderId >= 0) {
            customContent.reminder(reminderId)?.let { r -> if (r.enabled) rescheduleTomorrow(r.id) }
        }
        return Result.success()
    }

    private suspend fun rescheduleTomorrow(reminderId: Long) {
        // schedule() always arms the next occurrence, so re-arming is all that is needed.
        customContent.reminder(reminderId)?.let { scheduler.schedule(it) }
    }

    companion object {
        const val KEY_TRIGGER = "trigger"
        const val KEY_REMINDER_ID = "reminder_id"
        const val RECENCY_WINDOW_MILLIS = 14L * 24 * 60 * 60 * 1000
    }
}
