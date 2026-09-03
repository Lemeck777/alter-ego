package com.alterego.app.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.domain.models.FutureMessage
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The four horizons offered as chips. One year is the default, because it is long enough to hurt. */
private val DELIVERY_OPTIONS: List<Pair<String, Long>> = listOf(
    "6 months" to 6L,
    "1 year" to 12L,
    "2 years" to 24L,
    "5 years" to 60L,
)

private const val DEFAULT_MONTHS = 12L

/** Write to yourself. Text only, delivered once, on a date you choose. */
@Composable
fun FutureMeScreen(onBack: () -> Unit, viewModel: MeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneId.systemDefault()) }

    var draft by remember { mutableStateOf("") }
    var months by remember { mutableStateOf(DEFAULT_MONTHS) }
    val deliverAt = viewModel.futureDate(months)

    MeDetailScaffold(
        title = "Future Me",
        subtitle = "Write something to the person you'll be. I'll keep it and hand it over on the day.",
        onBack = onBack,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Dear future me") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Deliver in")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DELIVERY_OPTIONS.forEach { (label, value) ->
                Chip(label = label, selected = months == value) { months = value }
            }
        }
        Text(
            "Arrives ${dateFormat.format(deliverAt)}.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 12.dp),
        )

        PrimaryButton(
            text = "Seal it",
            enabled = draft.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp),
            onClick = { viewModel.addFutureMessage(draft, deliverAt); draft = "" },
        )

        Spacer(Modifier.height(32.dp))
        SectionLabel("Waiting for you")
        if (state.futureMessages.isEmpty()) {
            Text(
                "Nothing sealed yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.futureMessages.sortedBy { it.deliverAt }.forEach { message ->
                FutureMessageCard(
                    message = message,
                    dateText = dateFormat.format(message.deliverAt),
                    onDelete = { viewModel.deleteFutureMessage(message.id) },
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.accent else colors.muted,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.accent.copy(alpha = 0.16f) else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.muted.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun FutureMessageCard(message: FutureMessage, dateText: String, onDelete: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (message.deliveredAt != null) "Delivered $dateText" else "Arrives $dateText",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDelete) { Text("Delete", color = colors.muted) }
        }
        Text(
            message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
