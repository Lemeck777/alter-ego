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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.core.design.SelectableCard
import com.alterego.app.domain.models.AgeBand
import com.alterego.app.domain.models.AppLockMode
import com.alterego.app.domain.models.Goal
import com.alterego.app.domain.models.NotificationPrivacy
import com.alterego.app.domain.models.ReminderIntensity
import java.time.LocalTime

private val QUIET_PRESETS: List<Pair<String, Pair<LocalTime, LocalTime>>> = listOf(
    "10 PM to 7 AM" to (LocalTime.of(22, 0) to LocalTime.of(7, 0)),
    "11 PM to 6 AM" to (LocalTime.of(23, 0) to LocalTime.of(6, 0)),
    "9 PM to 8 AM" to (LocalTime.of(21, 0) to LocalTime.of(8, 0)),
    "Midnight to 6 AM" to (LocalTime.of(0, 0) to LocalTime.of(6, 0)),
)

/** Everything that changes how the companion behaves. Changes apply the moment they are made. */
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: MeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    var lockChoice by remember { mutableStateOf(state.appLock) }
    var pin by remember { mutableStateOf("") }
    LaunchedEffect(state.appLock) { lockChoice = state.appLock }

    MeDetailScaffold(title = "Settings", onBack = onBack) {
        SectionLabel("How often I check in")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReminderIntensity.entries.forEach { intensity ->
                SelectableCard(
                    title = intensity.label,
                    subtitle = intensity.subtitle,
                    selected = state.intensity == intensity,
                    onClick = { viewModel.setIntensity(intensity) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("Quiet hours")
        Text(
            "I will never speak during these hours.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QUIET_PRESETS.forEach { (label, range) ->
                SelectableCard(
                    title = label,
                    selected = state.quietHours.start == range.first && state.quietHours.end == range.second,
                    onClick = { viewModel.setQuietHours(range.first, range.second) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("What the lock screen shows")
        Text(
            "This is what someone else would see if they glanced at your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NotificationPrivacy.entries.forEach { privacy ->
                SelectableCard(
                    title = privacy.label,
                    subtitle = privacy.example,
                    selected = state.notificationPrivacy == privacy,
                    onClick = { viewModel.setNotificationPrivacy(privacy) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("App lock")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectableCard(
                title = "None",
                subtitle = "Open the app straight away.",
                selected = lockChoice == AppLockMode.NONE,
                onClick = { lockChoice = AppLockMode.NONE; pin = ""; viewModel.setAppLock(AppLockMode.NONE) },
            )
            SelectableCard(
                title = "PIN",
                subtitle = "4 to 8 digits, stored hashed on this device.",
                selected = lockChoice == AppLockMode.PIN,
                onClick = { lockChoice = AppLockMode.PIN },
            )
            SelectableCard(
                title = "Biometric",
                subtitle = if (state.canUseBiometrics) {
                    "Use your fingerprint or face."
                } else {
                    "Not available on this device yet."
                },
                selected = lockChoice == AppLockMode.BIOMETRIC,
                onClick = { if (state.canUseBiometrics) { lockChoice = AppLockMode.BIOMETRIC; pin = ""; viewModel.setAppLock(AppLockMode.BIOMETRIC) } },
            )
        }

        if (lockChoice == AppLockMode.PIN) {
            OutlinedTextField(
                value = pin,
                onValueChange = { entered -> pin = entered.filter { it.isDigit() }.take(8) },
                label = { Text("Choose a PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            PrimaryButton(
                text = "Save PIN",
                enabled = pin.length >= 4,
                modifier = Modifier.padding(top = 12.dp),
                onClick = { viewModel.setAppLock(AppLockMode.PIN, pin); pin = "" },
            )
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("What you want help with")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Goal.selectable.forEach { goal ->
                SelectableCard(
                    title = goal.label,
                    subtitle = goal.description,
                    selected = goal in state.goals,
                    onClick = { viewModel.toggleGoal(goal) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("Age band")
        Text(
            "Only used to pick age-appropriate education. A band is enough; I never need your date of birth.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AgeBand.entries.forEach { band ->
                SelectableCard(
                    title = band.label,
                    selected = state.ageBand == band,
                    onClick = { viewModel.setAgeBand(band) },
                )
            }
            SelectableCard(
                title = "Rather not say",
                selected = state.ageBand == null,
                onClick = { viewModel.setAgeBand(null) },
            )
        }
    }
}
