package com.alterego.app.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.data.ScheduledReminder
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel

private const val EXACT_EXPLANATION =
    "Exact alarms are kept for the reminders you asked to be precise. Everything else is flexible " +
        "by up to twenty minutes so your phone can batch it and spend less battery."

/** User-made reminders: "6:00, pray." Precise only where precision was actually requested. */
@Composable
fun RemindersScreen(onBack: () -> Unit, viewModel: MeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    var label by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("6") }
    var minute by remember { mutableStateOf("00") }
    var exact by remember { mutableStateOf(false) }

    val parsedHour = hour.toIntOrNull()
    val parsedMinute = minute.toIntOrNull()
    val validTime = parsedHour != null && parsedHour in 0..23 && parsedMinute != null && parsedMinute in 0..59

    MeDetailScaffold(
        title = "Scheduled reminders",
        subtitle = "The things you want said at a particular time, in your companion's voice.",
        onBack = onBack,
    ) {
        SectionLabel("Add one")
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("What should I remind you of?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = hour,
                onValueChange = { entered -> hour = entered.filter { it.isDigit() }.take(2) },
                label = { Text("Hour") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = minute,
                onValueChange = { entered -> minute = entered.filter { it.isDigit() }.take(2) },
                label = { Text("Minute") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Exactly at this time",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            PersonaSwitch(checked = exact, onCheckedChange = { exact = it })
        }
        PrimaryButton(
            text = "Add reminder",
            enabled = label.isNotBlank() && validTime,
            modifier = Modifier.padding(top = 12.dp),
            onClick = {
                viewModel.upsertReminder(
                    ScheduledReminder(
                        id = 0L,
                        label = label.trim(),
                        hour = parsedHour ?: 0,
                        minute = parsedMinute ?: 0,
                        exact = exact,
                        enabled = true,
                    ),
                )
                label = ""
                exact = false
            },
        )
        Text(
            EXACT_EXPLANATION,
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (!state.canScheduleExact) {
            Text(
                "Android has not granted exact alarms to this app yet, so precise reminders will " +
                    "arrive in a short window instead.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("Yours")
        if (state.reminders.isEmpty()) {
            Text("Nothing scheduled yet.", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.reminders.forEach { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onChange = viewModel::upsertReminder,
                    onDelete = { viewModel.deleteReminder(reminder.id) },
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ScheduledReminder,
    onChange: (ScheduledReminder) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(reminder.label, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        Text(
            formatTime(reminder.hour, reminder.minute),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 2.dp),
        )

        SwitchRow(
            label = "Exactly at this time",
            checked = reminder.exact,
            onCheckedChange = { onChange(reminder.copy(exact = it)) },
        )
        SwitchRow(
            label = "On",
            checked = reminder.enabled,
            onCheckedChange = { onChange(reminder.copy(enabled = it)) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDelete) { Text("Delete", color = colors.muted) }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalPersonaColors.current
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.muted, modifier = Modifier.weight(1f))
        PersonaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PersonaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalPersonaColors.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.background,
            checkedTrackColor = colors.accent,
            uncheckedThumbColor = colors.muted,
            uncheckedTrackColor = colors.surface,
            uncheckedBorderColor = colors.muted.copy(alpha = 0.4f),
        ),
    )
}

private fun formatTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)
