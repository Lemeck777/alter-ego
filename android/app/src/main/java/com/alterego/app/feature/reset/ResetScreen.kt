package com.alterego.app.feature.reset

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.alterego.app.domain.models.ResetContext

/**
 * "I reset."
 *
 * There is no red, no broken character and no word for failure anywhere on this screen. The
 * previous chapter keeps its days; a new one starts.
 */
@Composable
fun ResetScreen(onClose: () -> Unit, viewModel: ResetViewModel = hiltViewModel()) {
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
        Spacer(Modifier.height(24.dp))
        AlterEgoCharacter(
            state = when (state.stage) {
                ResetStage.CONFIRM -> CharacterState.LOOK
                ResetStage.ACKNOWLEDGE -> CharacterState.NOD
                else -> CharacterState.ENCOURAGE
            },
            primary = Color(state.persona?.primaryColor ?: 0xFF3E5C76L),
            accent = Color(state.persona?.accentColor ?: 0xFFC9A227L),
            size = 140.dp,
        )
        Spacer(Modifier.height(36.dp))

        Crossfade(targetState = state.stage, label = "reset") { stage ->
            when (stage) {
                ResetStage.CONFIRM -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Ending this chapter?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        state.commitment?.title.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    PrimaryButton(text = "I reset", modifier = Modifier.padding(top = 36.dp)) { viewModel.confirmReset() }
                    QuietButton(text = "Not yet", modifier = Modifier.padding(top = 10.dp), onClick = onClose)
                }

                ResetStage.ACKNOWLEDGE -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Okay.", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
                    Spacer(Modifier.height(16.dp))
                    (state.acknowledgement?.lines ?: listOf("What happens next matters more.")).forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    PrimaryButton(text = "Start again", modifier = Modifier.padding(top = 32.dp)) { viewModel.skipReflection() }
                    QuietButton(text = "Want to understand what happened?", modifier = Modifier.padding(top = 10.dp)) {
                        viewModel.openReflection()
                    }
                }

                ResetStage.REFLECT -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "What was happening?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    ResetContext.entries.forEach { context ->
                        SelectableCard(
                            title = context.label,
                            selected = state.selectedContext == context,
                        ) { viewModel.saveReflection(context) }
                    }
                    QuietButton(text = "Skip", modifier = Modifier.padding(top = 8.dp)) { viewModel.skipReflection() }
                }

                ResetStage.NEXT_CHAPTER -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Chapter ${state.newChapterNumber}",
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.accent,
                    )
                    Text(
                        "Starts now. Nothing you already did was undone.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    PrimaryButton(text = "Close", modifier = Modifier.padding(top = 36.dp), onClick = onClose)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
