package com.alterego.app.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alterego.app.domain.models.EvidenceLevel

/** The one primary button shape used everywhere, so the app reads as a single voice. */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.muted.copy(alpha = 0.5f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
    }
}

/** A selectable card used in onboarding, goal pickers and settings. */
@Composable
fun SelectableCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalPersonaColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.16f) else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.muted.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Small, quiet statistic used on Journey. Never competitive, never a scoreboard. */
@Composable
fun StatRow(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalPersonaColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.accent, modifier = Modifier.padding(end = 4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}

/** The evidence label. Every health statement in the app carries one of these. */
@Composable
fun EvidenceBadge(level: EvidenceLevel, modifier: Modifier = Modifier) {
    val colors = LocalPersonaColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(level.emoji, style = MaterialTheme.typography.labelSmall)
        Text(level.label, style = MaterialTheme.typography.labelSmall, color = colors.muted)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalPersonaColors.current.muted,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

@Composable
fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = LocalPersonaColors.current.muted,
            textAlign = TextAlign.Center,
        )
    }
}
