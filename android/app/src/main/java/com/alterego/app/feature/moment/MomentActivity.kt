package com.alterego.app.feature.moment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.AlterEgoTheme
import com.alterego.app.feature.urge.UrgeScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * The immersive Moment, opened when the user taps a notification.
 *
 * It is its own task and excluded from recents so it never becomes something to scroll back to.
 * Six to twelve seconds, then out of the way.
 */
@AndroidEntryPoint
class MomentActivity : FragmentActivity() {

    private val viewModel: MomentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val momentId = intent.getStringExtra(EXTRA_MOMENT_ID)
        val deliveryId = intent.getLongExtra(EXTRA_DELIVERY_ID, -1L)
        val openUrge = intent.getBooleanExtra(EXTRA_OPEN_URGE, false)
        viewModel.load(momentId = momentId, deliveryId = deliveryId, startInUrgeMode = openUrge)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            AlterEgoTheme(persona = state.persona) {
                if (state.inUrgeMode) {
                    UrgeScreen(onClose = { finish() })
                } else {
                    MomentScreen(
                        state = state,
                        onAction = { action -> viewModel.onAction(action) { finish() } },
                        onDismiss = { viewModel.dismiss(); finish() },
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_MOMENT_ID = "moment_id"
        private const val EXTRA_DELIVERY_ID = "delivery_id"
        private const val EXTRA_OPEN_URGE = "open_urge"

        fun intent(context: Context, momentId: String, deliveryId: Long, openUrge: Boolean = false): Intent =
            Intent(context, MomentActivity::class.java)
                .putExtra(EXTRA_MOMENT_ID, momentId)
                .putExtra(EXTRA_DELIVERY_ID, deliveryId)
                .putExtra(EXTRA_OPEN_URGE, openUrge)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        fun urgeIntent(context: Context): Intent =
            Intent(context, MomentActivity::class.java)
                .putExtra(EXTRA_OPEN_URGE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
