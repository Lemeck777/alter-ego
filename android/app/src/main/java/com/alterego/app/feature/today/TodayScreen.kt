package com.alterego.app.feature.today

import androidx.compose.foundation.clickable
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
import com.alterego.app.domain.models.CharacterState

/**
 * Home. Deliberately almost empty.
 *
 * The person downloaded this app to escape noise, so Today shows who is with them, how long the
 * current chapter has run, one line of focus, and the one button that matters in a hard moment.
 */
@Composable
fun TodayScreen(
    onOpenMoment: () -> Unit,
    onOpenUrge: () -> Unit,
    onOpenBiology: () -> Unit,
    onOpenPersona: () -> Unit,
    onOpenReset: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text("ALTER EGO", style = MaterialTheme.typography.labelSmall, color = colors.muted)

        Spacer(Modifier.height(36.dp))
        Text(
            (state.persona?.name ?: "").uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.muted,
            modifier = Modifier.clickable(onClick = onOpenPersona),
        )
        Spacer(Modifier.height(12.dp))
        AlterEgoCharacter(
            state = if (state.headsUp != null) CharacterState.SERIOUS else CharacterState.IDLE,
            primary = Color(state.persona?.primaryColor ?: 0xFF3E5C76L),
            accent = Color(state.persona?.accentColor ?: 0xFFC9A227L),
            size = 150.dp,
            modifier = Modifier.clickable(onClick = onOpenPersona),
        )

        if (state.chapter != null) {
            Spacer(Modifier.height(36.dp))
            Text(
                "DAY ${state.dayNumber}",
                style = MaterialTheme.typography.displayMedium,
                color = colors.onBackground,
            )
            Text(
                state.elapsedText,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.accent,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            state.supportLine,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )

        state.headsUp?.let { headsUp ->
            Spacer(Modifier.height(20.dp))
            Text(
                headsUp,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.todaysFocus?.let { moment ->
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMoment),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("TODAY'S FOCUS", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                Spacer(Modifier.height(6.dp))
                moment.lines.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        PrimaryButton(text = "I need a moment", onClick = onOpenUrge)

        if (state.chapter != null) {
            QuietButton(text = "I reset", modifier = Modifier.padding(top = 10.dp), onClick = onOpenReset)
        }

        if (state.showBiology) {
            state.biologyPhase?.let { phase ->
                Spacer(Modifier.height(24.dp))
                Text(
                    "Biology - Day ${state.dayNumber}: ${phase.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(onClick = onOpenBiology),
                )
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}
