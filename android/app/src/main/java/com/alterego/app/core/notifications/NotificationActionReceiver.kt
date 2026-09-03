package com.alterego.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.data.CustomContentRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles the "I'm good" action so the user can answer without opening the app at all. */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var customContent: CustomContentRepository
    @Inject lateinit var notifier: MomentNotifier
    @Inject lateinit var analytics: Analytics

    override fun onReceive(context: Context, intent: Intent) {
        val deliveryId = intent.getLongExtra(EXTRA_DELIVERY_ID, -1L)
        if (deliveryId < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == ACTION_GOOD) {
                    customContent.setReaction(deliveryId, "good")
                    analytics.track("moment_reaction", mapOf("reaction" to "good", "surface" to "notification"))
                }
                notifier.cancel(deliveryId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_GOOD = "com.alterego.app.action.GOOD"
        const val EXTRA_DELIVERY_ID = "delivery_id"
    }
}
