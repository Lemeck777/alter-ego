package com.alterego.app.feature.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.core.design.SelectableCard
import com.alterego.app.domain.models.CommitmentRule
import com.alterego.app.domain.models.Goal

/**
 * A new commitment, asked in the same order as onboarding: what it's about, then exactly what the
 * rule is. The precise rule only gets asked when the goal actually needs one.
 */
@Composable
fun NewCommitmentScreen(onDone: () -> Unit, viewModel: MeViewModel = hiltViewModel()) {
    val colors = LocalPersonaColors.current

    var goal by remember { mutableStateOf<Goal?>(null) }
    var rule by remember { mutableStateOf<CommitmentRule?>(null) }
    var customRule by remember { mutableStateOf("") }

    val needsRule = goal == Goal.RETENTION || goal == Goal.PORN_AVOIDANCE
    val ready = goal != null &&
        (!needsRule || (rule != null && (rule != CommitmentRule.CUSTOM || customRule.isNotBlank())))

    MeDetailScaffold(
        title = "A new commitment",
        subtitle = "Tell me exactly what you mean. I'll track that and nothing else.",
        onBack = onDone,
    ) {
        SectionLabel("What is it about")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Goal.selectable.forEach { option ->
                SelectableCard(
                    title = option.label,
                    subtitle = option.description,
                    selected = goal == option,
                    onClick = {
                        goal = option
                        if (option != Goal.RETENTION && option != Goal.PORN_AVOIDANCE) {
                            rule = null
                            customRule = ""
                        }
                    },
                )
            }
        }

        if (needsRule) {
            Spacer(Modifier.height(32.dp))
            SectionLabel("The rule")
            Text(
                "These are not the same thing, so pick the one you actually decided on.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CommitmentRule.entries.forEach { option ->
                    SelectableCard(
                        title = option.label,
                        subtitle = option.description,
                        selected = rule == option,
                        onClick = { rule = option },
                    )
                }
            }
            if (rule == CommitmentRule.CUSTOM) {
                OutlinedTextField(
                    value = customRule,
                    onValueChange = { customRule = it },
                    label = { Text("Your rule, in your words") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = "Start this", enabled = ready) {
            val chosenGoal = goal ?: return@PrimaryButton
            val chosenRule = rule ?: CommitmentRule.TRACK_FREQUENCY
            viewModel.createCommitment(chosenGoal, chosenRule, customRule, onDone)
        }
        Text(
            "Everything you write stays on this phone unless you turn on backup yourself.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
