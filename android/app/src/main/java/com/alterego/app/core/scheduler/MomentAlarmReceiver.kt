package com.alterego.app.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receives the alarm and immediately hands off to WorkManager. Broadcast receivers get a very short
 * window; WorkManager survives process death and app restarts, which is what Android recommends
 * for work that must actually happen.
 */
@AndroidEntryPoint
class MomentAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val trigger = intent.getStringExtra(EXTRA_TRIGGER) ?: "random"
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

        val request = OneTimeWorkRequestBuilder<DeliverMomentWorker>()
            .setInputData(
                Data.Builder()
                    .putString(DeliverMomentWorker.KEY_TRIGGER, trigger)
                    .putLong(DeliverMomentWorker.KEY_REMINDER_ID, reminderId)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("deliver_moment_${System.currentTimeMillis()}", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val ACTION_FIRE = "com.alterego.app.action.FIRE_MOMENT"
        const val EXTRA_TRIGGER = "trigger"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
