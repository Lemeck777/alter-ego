package com.alterego.app.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.LocalPersonaColors

/**
 * The whole of onboarding is about a minute. Every screen asks one thing, and the notification
 * request comes last, in context, after the user has met their companion.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) viewModel.onNotificationsDenied()
        viewModel.finish(onFinished)
    }

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn(tween280()) togetherWith fadeOut(tween160()) },
            label = "onboarding",
        ) { step ->
            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(onNext = viewModel::next)
                    OnboardingStep.GOALS -> GoalsStep(state = state, onToggle = viewModel::toggleGoal, onNext = viewModel::next)
                    OnboardingStep.COMMITMENT_RULE -> CommitmentRuleStep(
                        state = state,
                        onSelect = viewModel::setRule,
                        onCustomRule = viewModel::setCustomRule,
                        onNext = viewModel::next,
                    )
                    OnboardingStep.AGE -> AgeStep(state = state, onSelect = viewModel::setAgeBand, onNext = viewModel::next)
                    OnboardingStep.PERSONA -> PersonaStep(
                        state = state,
                        onSelect = viewModel::setPersona,
                        onShowAll = viewModel::showAllPersonas,
                        onNext = viewModel::next,
                    )
                    OnboardingStep.INTENSITY -> IntensityStep(state = state, onSelect = viewModel::setIntensity, onNext = viewModel::next)
                    OnboardingStep.QUIET_HOURS -> QuietHoursStep(state = state, onChange = viewModel::setQuietHours, onNext = viewModel::next)
                    OnboardingStep.INTERVENTIONS -> InterventionsStep(state = state, onToggle = viewModel::toggleIntervention, onNext = viewModel::next)
                    OnboardingStep.NOTIFICATIONS -> NotificationsStep(
                        state = state,
                        onAllow = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.finish(onFinished)
                            }
                        },
                        onSkip = { viewModel.onNotificationsDenied(); viewModel.finish(onFinished) },
                    )
                    OnboardingStep.DONE -> FirstMomentStep(state = state, onDone = onFinished)
                }
            }
        }
    }
}

private fun tween280() = androidx.compose.animation.core.tween<Float>(280)
private fun tween160() = androidx.compose.animation.core.tween<Float>(160)
