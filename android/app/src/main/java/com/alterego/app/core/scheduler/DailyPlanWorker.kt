package com.alterego.app.core.scheduler

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.usecases.ResetPatternAnalyzer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs once a day (and after boot) to lay out the day's Moments and re-arm user reminders.
 * The high-risk hour comes from the user's own reset history, never from guesswork.
 */
@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferencesRepository,
    private val journey: JourneyRepository,
    private val scheduler: MomentScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.snapshot().onboarded) return Result.success()
        val pattern = ResetPatternAnalyzer().analyze(journey.allResets())
        scheduler.planToday(highRiskHour = pattern.highRiskHour.takeIf { pattern.isMeaningful })
        scheduler.rescheduleUserReminders()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "daily_plan"
        const val PERIODIC_NAME = "daily_plan_periodic"
    }
}
