package com.alterego.app.feature.moment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.feature.urge.UrgeScreen

/**
 * The in-app version of a Moment, reached by tapping Today's focus line. The notification path uses
 * MomentActivity instead so the companion can appear without the rest of the app around it.
 */
@Composable
fun MomentRoute(onClose: () -> Unit, viewModel: MomentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load(momentId = null, deliveryId = -1L, startInUrgeMode = false)
    }

    if (state.inUrgeMode) {
        UrgeScreen(onClose = { viewModel.exitUrgeMode(); onClose() })
    } else {
        MomentScreen(
            state = state,
            onAction = { action -> viewModel.onAction(action) { onClose() } },
            onDismiss = { viewModel.dismiss(); onClose() },
        )
    }
}
