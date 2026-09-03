package com.alterego.app.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.billing.EntitlementRepository
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.domain.models.PersonalQuote
import com.alterego.app.feature.root.Destinations

/**
 * "Teach me what to say." The user's own sentences, said back to them in the companion's voice.
 * These outrank everything written for them, so the screen keeps the promise plainly.
 */
@Composable
fun QuotesScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: MeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current
    var draft by remember { mutableStateOf("") }

    MeDetailScaffold(
        title = "Teach me what to say",
        subtitle = "Whatever you write here I'll say back to you more often than anything else in the app.",
        onBack = onBack,
    ) {
        if (state.canAddQuote) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("In your own words") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Teach me this",
                enabled = draft.isNotBlank(),
                modifier = Modifier.padding(top = 12.dp),
                onClick = { viewModel.addQuote(draft); draft = "" },
            )
        } else {
            UpsellRow(
                text = "You've taught me ${EntitlementRepository.FREE_MAX_PERSONAL_QUOTES} lines. " +
                    "Alter Ego+ lets you teach me as many as you want.",
                onClick = { onNavigate(Destinations.PREMIUM) },
            )
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("Your lines")
        if (state.quotes.isEmpty()) {
            Text(
                "Nothing yet. The best ones are usually the sentence you'd want to hear at 11pm.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.quotes.forEach { quote ->
                QuoteRow(
                    quote = quote,
                    onToggle = { enabled -> viewModel.toggleQuote(quote.id, enabled) },
                    onDelete = { viewModel.deleteQuote(quote.id) },
                )
            }
        }
    }
}

@Composable
private fun QuoteRow(quote: PersonalQuote, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                quote.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (quote.enabled) colors.onBackground else colors.muted,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = quote.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.muted,
                    uncheckedTrackColor = colors.surface,
                    uncheckedBorderColor = colors.muted.copy(alpha = 0.4f),
                ),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDelete) { Text("Delete", color = colors.muted) }
        }
    }
}

@Composable
private fun UpsellRow(text: String, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
        Text(
            "See Alter Ego+",
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
