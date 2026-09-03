package com.alterego.app.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.alterego.app.R
import com.alterego.app.domain.models.NotificationPrivacy
import com.alterego.app.feature.moment.MomentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the quiet notification that leads to the full-screen Moment.
 *
 * We deliberately do not use full-screen intents for ordinary nudges: Android reserves those for
 * calls and alarms. The immersive Moment screen opens only when the user taps.
 */
@Singleton
class MomentNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notifyMoment(
        deliveryId: Long,
        momentId: String,
        personaName: String,
        privacy: NotificationPrivacy,
        dayNumber: Int,
        channel: String = NotificationChannels.MOMENTS,
    ) {
        if (!hasPermission()) return
        NotificationChannels.ensure(context)

        val title = when (privacy) {
            NotificationPrivacy.PRIVATE -> context.getString(R.string.notification_private_title, personaName)
            NotificationPrivacy.NORMAL -> context.getString(R.string.notification_normal_title)
            NotificationPrivacy.EXPLICIT -> context.getString(R.string.notification_explicit_title, dayNumber)
        }
        val body = if (privacy == NotificationPrivacy.PRIVATE) "Tap when you have ten seconds." else "Tap to open."

        val open = PendingIntent.getActivity(
            context,
            deliveryId.toInt(),
            MomentActivity.intent(context, momentId, deliveryId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val good = PendingIntent.getBroadcast(
            context,
            (deliveryId * 10 + 2).toInt(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_GOOD)
                .putExtra(NotificationActionReceiver.EXTRA_DELIVERY_ID, deliveryId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val help = PendingIntent.getActivity(
            context,
            (deliveryId * 10 + 1).toInt(),
            MomentActivity.intent(context, momentId, deliveryId, openUrge = true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Explicit mode names the commitment, so keep it off the lock screen.
            .setVisibility(
                if (privacy == NotificationPrivacy.EXPLICIT) NotificationCompat.VISIBILITY_PRIVATE
                else NotificationCompat.VISIBILITY_PUBLIC,
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notification_action_good), good)
            .addAction(0, context.getString(R.string.notification_action_help), help)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG, deliveryId.toInt(), notification)
    }

    /** Ongoing, silent, lock-screen-hidden companion notification during the ten-minute urge timer. */
    fun notifyUrgeTimer(remainingText: String) {
        if (!hasPermission()) return
        NotificationChannels.ensure(context)
        val open = PendingIntent.getActivity(
            context,
            URGE_ID,
            MomentActivity.urgeIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.URGE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.urge_timer_title))
            .setContentText(remainingText.ifEmpty { context.getString(R.string.urge_timer_body) })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
        NotificationManagerCompat.from(context).notify(URGE_ID, notification)
    }

    fun cancelUrgeTimer() = NotificationManagerCompat.from(context).cancel(URGE_ID)

    fun cancel(deliveryId: Long) =
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_TAG, deliveryId.toInt())

    companion object {
        const val NOTIFICATION_TAG = "moment"
        const val URGE_ID = 7_001
    }
}
