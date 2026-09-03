package com.alterego.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.alterego.app.R

object NotificationChannels {
    const val MOMENTS = "moments"
    const val SCHEDULED = "scheduled"
    const val URGE = "urge"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                MOMENTS,
                context.getString(R.string.notification_channel_moments),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_moments_desc)
                setShowBadge(false)
                enableVibration(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                SCHEDULED,
                context.getString(R.string.notification_channel_scheduled),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_scheduled_desc)
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                URGE,
                context.getString(R.string.notification_channel_urge),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_urge_desc)
                setShowBadge(false)
            },
        )
    }
}
