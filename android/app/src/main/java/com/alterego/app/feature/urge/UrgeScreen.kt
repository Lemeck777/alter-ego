package com.alterego.app.feature.urge

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.animation.AlterEgoCharacter
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.QuietButton
import com.alterego.app.core.design.SelectableCard
import com.alterego.app.domain.models.CharacterState
import com.alterego.app.domain.models.UrgeLevel

/**
 * Urge Mode.
 *
 * The whole design assumes the person is not able to make a good decision right now, so we never
 * ask them to. We ask for ten minutes, change what their body is doing, then check back.
 */
@Composable
fun UrgeScreen(onClose: () -> Unit, viewModel: UrgeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        AlterEgoCharacter(
            state = when (state.stage) {
                UrgeStage.TIMER -> CharacterState.BREATHE
                UrgeStage.CLOSING -> CharacterState.NOD
                UrgeStage.CHECK_IN -> CharacterState.LOOK
                else -> CharacterState.SERIOUS
            },
            primary = Color(state.persona?.primaryColor ?: 0xFF3E5C76L),
            accent = Color(state.persona?.accentColor ?: 0xFFC9A227L),
            size = 140.dp,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            state.personaLine,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        Crossfade(targetState = state.stage, label = "urge") { stage ->
            when (stage) {
                UrgeStage.OPENING -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pick one. Any one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    state.interventions.forEach { intervention ->
                        SelectableCard(
                            title = intervention.title,
                            subtitle = intervention.lines.firstOrNull(),
                            selected = intervention.id in state.usedInterventionIds,
                            onClick = { viewModel.chooseIntervention(intervention) },
                        )
                    }
                    QuietButton(text = "Just give me ten minutes", modifier = Modifier.padding(top = 8.dp)) { viewModel.startTimer() }
                }

                UrgeStage.INTERVENTION -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.currentIntervention?.lines?.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(text = "Done. Start the ten minutes") { viewModel.startTimer() }
                    QuietButton(text = "Something else") { viewModel.showInterventions() }
                }

                UrgeStage.TIMER -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(state.timerText, style = MaterialTheme.typography.displayLarge, color = colors.accent)
                    Text(
                        "I'm here. You don't have to do anything.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    QuietButton(text = "I'm ready to check in", modifier = Modifier.padding(top = 28.dp)) { viewModel.skipToCheckIn() }
                }

                UrgeStage.CHECK_IN -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Still strong?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBackground,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    UrgeLevel.entries.forEach { level ->
                        SelectableCard(title = level.label, selected = state.finalLevel == level) { viewModel.reportLevel(level) }
                    }
                }

                UrgeStage.CLOSING -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.closingLine,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    PrimaryButton(text = "Close", modifier = Modifier.padding(top = 28.dp), onClick = onClose)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        if (state.stage != UrgeStage.CLOSING) {
            QuietButton(text = "Leave") { viewModel.abandon(); onClose() }
        }
        Spacer(Modifier.height(24.dp))
    }
}
