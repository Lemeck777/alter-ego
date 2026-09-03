package com.alterego.app.feature.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.QuietButton
import com.alterego.app.core.design.SectionLabel
import kotlinx.coroutines.launch

private const val LOCAL_FIRST_EXPLANATION =
    "Your commitment history, your age band, the urges you logged, the notes you wrote after a " +
        "reset and the lines you taught me all live in this app, on this phone. They are not " +
        "uploaded, not sold and not readable by us. Nothing leaves this device unless you turn on " +
        "backup yourself."

/** Plain language about where the data lives, and three doors out of it. */
@Composable
fun PrivacyScreen(onBack: () -> Unit, viewModel: MeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var confirmDeleteHistory by remember { mutableStateOf(false) }
    var confirmDeleteEverything by remember { mutableStateOf(false) }
    var confirmExport by remember { mutableStateOf(false) }
    var exported by remember { mutableStateOf<String?>(null) }

    MeDetailScaffold(title = "Privacy and data", onBack = onBack) {
        Text(LOCAL_FIRST_EXPLANATION, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)

        Spacer(Modifier.height(16.dp))
        Text(
            "Notifications carry as little as you asked them to. Personal quotes and reset notes are " +
                "never attached to anything we measure.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )

        Spacer(Modifier.height(32.dp))
        SectionLabel("Anonymous usage data")
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Share anonymous usage", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                Text(
                    "Counts only: how often the app is opened, whether a reminder was useful. Never your words.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(
                checked = state.analyticsEnabled,
                onCheckedChange = viewModel::setAnalyticsEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.muted,
                    uncheckedTrackColor = colors.surface,
                    uncheckedBorderColor = colors.muted.copy(alpha = 0.4f),
                ),
            )
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("Your data")
        QuietButton(text = "Export my journey") { confirmExport = true }
        Spacer(Modifier.height(10.dp))
        QuietButton(text = "Delete my history") { confirmDeleteHistory = true }
        Spacer(Modifier.height(10.dp))
        QuietButton(text = "Delete everything") { confirmDeleteEverything = true }
    }

    if (confirmExport) {
        ConfirmDialog(
            title = "Export my journey",
            body = "This builds a plain-text copy of your chapters, resets and reflections so you can keep it somewhere of your own.",
            confirmLabel = "Build it",
            onConfirm = {
                confirmExport = false
                scope.launch { exported = viewModel.exportJourney() }
            },
            onDismiss = { confirmExport = false },
        )
    }

    if (confirmDeleteHistory) {
        ConfirmDialog(
            title = "Delete my history",
            body = "Your chapters, resets, urge logs and commitments are removed from this phone. Your companion and your settings stay. This cannot be undone.",
            confirmLabel = "Delete history",
            onConfirm = { confirmDeleteHistory = false; viewModel.deleteHistory() },
            onDismiss = { confirmDeleteHistory = false },
        )
    }

    if (confirmDeleteEverything) {
        ConfirmDialog(
            title = "Delete everything",
            body = "Everything goes: history, the lines you taught me, messages to your future self, reminders, your PIN and every setting. The app starts over. This cannot be undone.",
            confirmLabel = "Delete everything",
            onConfirm = { confirmDeleteEverything = false; viewModel.deleteEverything(); onBack() },
            onDismiss = { confirmDeleteEverything = false },
        )
    }

    exported?.let { text ->
        AlertDialog(
            onDismissRequest = { exported = null },
            containerColor = colors.surface,
            titleContentColor = colors.onBackground,
            textContentColor = colors.muted,
            title = { Text("Your journey", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
                }
            },
            confirmButton = {
                TextButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                    Text("Copy to clipboard", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { exported = null }) { Text("Close", color = colors.muted) }
            },
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalPersonaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onBackground,
        textContentColor = colors.muted,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep it", color = colors.muted) } },
    )
}
