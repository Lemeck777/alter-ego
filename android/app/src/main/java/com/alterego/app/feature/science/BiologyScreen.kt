package com.alterego.app.feature.science

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.domain.models.EvidenceClaim
import com.alterego.app.domain.models.TimelinePhase

/**
 * The biology timeline.
 *
 * PRODUCT RULE, enforced by this file: nothing on this screen is an estimate about the user's
 * body. No sperm count, no testosterone level, no fertility score, no percentage. It describes
 * what research says in general, phase by phase, and it says plainly that the only way to know
 * anything about your own semen quality is a laboratory semen analysis.
 */
@Composable
fun BiologyScreen(
    onBack: () -> Unit,
    viewModel: ScienceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current
    val timeline = state.timeline
    val currentPhase = if (state.currentDay > 0) timeline.phaseFor(state.currentDay) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        ScienceHeader(title = if (state.currentDay > 0) "Day ${state.currentDay}" else "The biology", onBack = onBack)

        Text(
            currentPhase?.title ?: "What research says, phase by phase",
            style = MaterialTheme.typography.titleLarge,
            color = colors.accent,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (state.currentDay <= 0) {
            Text(
                "You don't have an open chapter, so nothing below is marked as \"where you are\".",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        DisclaimerCard(text = timeline.disclaimer)

        Spacer(Modifier.height(28.dp))
        SectionLabel("The phases")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            timeline.phases.forEach { phase ->
                PhaseCard(
                    phase = phase,
                    isCurrent = currentPhase != null && phase.phaseId == currentPhase.phaseId,
                    claims = phase.claimIds.mapNotNull { viewModel.claimById(it) },
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "This app will never show you an estimated sperm count, a testosterone level, a " +
                "fertility score or a percentage about your body, because none of those can be " +
                "calculated from a day counter. Individual semen quality can only be measured by " +
                "a laboratory semen analysis. If you want a real picture of your reproductive " +
                "health, ask a clinician for one.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DisclaimerCard(text: String) {
    val colors = LocalPersonaColors.current
    if (text.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.accent.copy(alpha = 0.12f))
            .border(1.5.dp, colors.accent, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text("READ THIS FIRST", style = MaterialTheme.typography.labelSmall, color = colors.accent)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
    }
}

@Composable
private fun PhaseCard(phase: TimelinePhase, isCurrent: Boolean, claims: List<EvidenceClaim>) {
    val colors = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isCurrent) colors.accent.copy(alpha = 0.16f) else colors.surface)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) colors.accent else colors.muted.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            phase.dayRangeLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) colors.accent else colors.muted,
        )
        Spacer(Modifier.height(6.dp))
        Text(phase.title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        if (isCurrent) {
            Text(
                "Where you are now",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(phase.summary, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)

        if (claims.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                claims.forEach { ClaimCitation(it) }
            }
        }
    }
}

/** "Day 1", "Days 2-3", "Days 4-7", "Beyond 7 days". */
internal fun TimelinePhase.dayRangeLabel(): String {
    val from = dayFrom.coerceAtLeast(1)
    val to = dayTo ?: return "Beyond ${(dayFrom - 1).coerceAtLeast(1)} days"
    return if (to <= from) "Day $from" else "Days $from-$to"
}
