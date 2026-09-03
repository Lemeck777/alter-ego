package com.alterego.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.WorkManager
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.notifications.NotificationChannels
import com.alterego.app.core.scheduler.ContentSyncWorker
import com.alterego.app.core.scheduler.DailyPlanWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AlterEgoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var content: ContentRepository
    @Inject lateinit var prefs: UserPreferencesRepository
    @Inject lateinit var analytics: Analytics

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        scope.launch {
            content.ensureLoaded()
            val p = prefs.snapshot()
            if (p.installedAtMillis == 0L) {
                prefs.update { it.copy(installedAtMillis = System.currentTimeMillis()) }
            }
            analytics.track(LocalAnalytics.APP_OPENED)
            schedulePeriodicWork()
        }
    }

    private fun schedulePeriodicWork() {
        val wm = WorkManager.getInstance(this)
        // Re-plan the day every morning. KEEP so an existing schedule is not disturbed on each launch.
        wm.enqueueUniquePeriodicWork(
            DailyPlanWorker.PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyPlanWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            ContentSyncWorker.PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ContentSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build(),
        )
    }
}
