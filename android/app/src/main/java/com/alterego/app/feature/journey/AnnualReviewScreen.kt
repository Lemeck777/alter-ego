package com.alterego.app.feature.journey

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.animation.AlterEgoCharacter
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.core.design.StatRow
import com.alterego.app.domain.models.CharacterState

/**
 * The anniversary. A year of company, read back quietly, and then handed forward as a question
 * rather than a score.
 */
@Composable
fun AnnualReviewScreen(onClose: () -> Unit, viewModel: JourneyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current
    val stats = state.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        AlterEgoCharacter(
            state = CharacterState.CELEBRATE,
            primary = colors.primary,
            accent = colors.accent,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Another Year Together",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel("This is what it looked like")
            if (stats != null) {
                StatRow(value = dayCount(stats.daysTogether), label = "Together")
                StatRow(value = dayCount(stats.lifetimeCommittedDays), label = "Commitments kept")
                StatRow(value = stats.chaptersCompleted.toString(), label = "Chapters completed")
                StatRow(value = dayCount(stats.longestChapterDays), label = "Longest chapter")
            }
            StatRow(value = mostDifficultPeriod(state), label = "Most difficult period")
            StatRow(
                value = state.personaName.ifBlank { "Your Alter Ego" },
                label = "Favourite Alter Ego",
            )
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "Who do you want to become this year?",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))
        PrimaryButton(text = "Close", onClick = onClose)
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * We only name a difficult window when the user's own resets actually cluster into one. Below that
 * threshold we say so plainly instead of inventing a pattern.
 */
private fun mostDifficultPeriod(state: JourneyState): String {
    val hour = state.pattern.highRiskHour
    return if (state.pattern.isMeaningful && hour != null) "around ${formatHourOfDay(hour)}" else "Not enough data yet"
}
