package com.alterego.app.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.CenteredMessage
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.domain.models.Commitment
import com.alterego.app.feature.root.Destinations

/**
 * The commitments the user is currently keeping. One of them is primary: that is the one the
 * companion talks about by default.
 *
 * [onNavigate] carries the upgrade route when the free tier is already full; the host passes the
 * same navigate lambda it gives [MeScreen].
 */
@Composable
fun CommitmentsScreen(
    onBack: () -> Unit,
    onNewCommitment: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: MeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MeDetailScaffold(
        title = "Commitments",
        subtitle = "What you decided to keep. Pausing one is not failing at it.",
        onBack = onBack,
    ) {
        if (state.commitments.isEmpty()) {
            CenteredMessage("Nothing here yet. Decide on one thing and I'll hold it with you.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.commitments.forEach { commitment ->
                CommitmentCard(
                    commitment = commitment,
                    onSetPrimary = { viewModel.setPrimaryCommitment(commitment.id) },
                    onPause = { viewModel.pauseCommitment(commitment.id) },
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        if (state.canAddCommitment) {
            PrimaryButton(text = "Add commitment", onClick = onNewCommitment)
        } else {
            PrimaryButton(text = "Add commitment (Alter Ego+)") { onNavigate(Destinations.PREMIUM) }
            Spacer(Modifier.height(10.dp))
            Text(
                "One commitment is free, and one is usually the honest number. Alter Ego+ opens the rest.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPersonaColors.current.muted,
            )
        }
    }
}

@Composable
private fun CommitmentCard(
    commitment: Commitment,
    onSetPrimary: () -> Unit,
    onPause: () -> Unit,
) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(
                width = if (commitment.isPrimary) 1.5.dp else 1.dp,
                color = if (commitment.isPrimary) colors.accent else colors.muted.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(commitment.title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        Text(
            commitment.customRule?.takeIf { it.isNotBlank() } ?: commitment.rule.label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (commitment.isPrimary) {
            Text(
                "This is the one I talk about",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!commitment.isPrimary) {
                TextButton(onClick = onSetPrimary) { Text("Make it primary", color = colors.accent) }
            }
            TextButton(onClick = onPause) { Text("Pause", color = colors.muted) }
        }
    }
}
