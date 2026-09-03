package com.alterego.app.feature.moment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alterego.app.core.animation.AlterEgoCharacter
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.QuietButton
import com.alterego.app.domain.models.CharacterState
import com.alterego.app.domain.models.MomentAction
import kotlinx.coroutines.delay

/**
 * The signature experience: character enters, looks at you, says one to three short lines,
 * offers at most two choices, and leaves. Lines appear one at a time so it reads like speech,
 * not like a wall of text.
 */
@Composable
fun MomentScreen(state: MomentUiState, onAction: (MomentAction) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalPersonaColors.current
    val moment = state.moment
    var visibleLines by remember { mutableIntStateOf(0) }

    LaunchedEffect(moment?.id) {
        visibleLines = 0
        val lines = moment?.lines ?: return@LaunchedEffect
        delay(420)
        lines.indices.forEach { index ->
            visibleLines = index + 1
            // A beat between lines. Long enough to land, short enough to stay under ten seconds.
            delay(if (index == 0) 900 else 1400)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 30.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))

        AlterEgoCharacter(
            state = moment?.animation ?: CharacterState.LOOK,
            primary = Color(state.persona?.primaryColor ?: 0xFF3E5C76L),
            accent = Color(state.persona?.accentColor ?: 0xFFC9A227L),
            size = 170.dp,
        )

        Spacer(Modifier.height(40.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            moment?.lines?.forEachIndexed { index, line ->
                AnimatedVisibility(
                    visible = index < visibleLines,
                    enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 4 },
                ) {
                    Text(
                        line,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        moment?.source?.let { source ->
            if (moment.evidenceType == "scripture") {
                Text(source, style = MaterialTheme.typography.labelSmall, color = colors.muted, modifier = Modifier.padding(bottom = 16.dp))
            }
        }

        val actions = moment?.actions.orEmpty()
        if (actions.isEmpty()) {
            PrimaryButton(text = "Okay", onClick = onDismiss)
        } else {
            PrimaryButton(text = actions.first().label) { onAction(actions.first()) }
            actions.getOrNull(1)?.let { second ->
                QuietButton(text = second.label, modifier = Modifier.padding(top = 10.dp)) { onAction(second) }
            }
        }

        Text(
            if (state.saved) "Saved" else "Save this",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier
                .padding(top = 18.dp)
                .clickable { onAction(MomentAction(if (state.saved) "Saved" else "Save this", "save")) },
        )
    }
}
