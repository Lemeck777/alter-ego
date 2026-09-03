package com.alterego.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alterego.app.core.animation.AlterEgoCharacter
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.QuietButton
import com.alterego.app.core.design.SelectableCard
import com.alterego.app.domain.models.AgeBand
import com.alterego.app.domain.models.CharacterState
import com.alterego.app.domain.models.CommitmentRule
import com.alterego.app.domain.models.Goal
import com.alterego.app.domain.models.ReminderIntensity
import java.time.LocalTime

@Composable
internal fun StepScaffold(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    actionEnabled: Boolean = true,
    onAction: () -> Unit,
    secondary: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalPersonaColors.current
    Column(Modifier.fillMaxSize()) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            modifier = Modifier.padding(top = 24.dp),
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.weight(1f)) { content() }
        secondary?.invoke()
        PrimaryButton(text = actionLabel, enabled = actionEnabled, modifier = Modifier.padding(top = 16.dp), onClick = onAction)
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ALTER EGO", style = MaterialTheme.typography.labelSmall, color = colors.muted)
        Spacer(Modifier.height(40.dp))
        AlterEgoCharacter(state = CharacterState.LOOK, primary = colors.primary, accent = colors.accent, size = 160.dp)
        Spacer(Modifier.height(40.dp))
        Text(
            "Life gets noisy.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            "I'll help you remember who you want to be.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = "Start", onClick = onNext)
    }
}

@Composable
fun GoalsStep(state: OnboardingState, onToggle: (Goal) -> Unit, onNext: () -> Unit) {
    StepScaffold(
        title = "What do you want help with?",
        subtitle = "Pick as many as you like. You can change these later.",
        actionLabel = "Continue",
        actionEnabled = state.selectedGoals.isNotEmpty(),
        onAction = onNext,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Goal.selectable) { goal ->
                SelectableCard(
                    title = goal.label,
                    subtitle = goal.description,
                    selected = goal in state.selectedGoals,
                    onClick = { onToggle(goal) },
                )
            }
        }
    }
}

@Composable
fun CommitmentRuleStep(
    state: OnboardingState,
    onSelect: (CommitmentRule) -> Unit,
    onCustomRule: (String) -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalPersonaColors.current
    StepScaffold(
        title = "What's your commitment?",
        subtitle = "These are not the same thing, so tell me exactly what you mean. I'll track that and nothing else.",
        actionLabel = "Continue",
        actionEnabled = state.rule != null && (state.rule != CommitmentRule.CUSTOM || state.customRule.isNotBlank()),
        onAction = onNext,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CommitmentRule.entries.forEach { rule ->
                SelectableCard(
                    title = rule.label,
                    subtitle = rule.description,
                    selected = state.rule == rule,
                    onClick = { onSelect(rule) },
                )
            }
            if (state.rule == CommitmentRule.CUSTOM) {
                OutlinedTextField(
                    value = state.customRule,
                    onValueChange = onCustomRule,
                    label = { Text("Your rule, in your words") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            Text(
                "Everything you write stays on this phone unless you turn on backup yourself.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
fun AgeStep(state: OnboardingState, onSelect: (AgeBand?) -> Unit, onNext: () -> Unit) {
    val colors = LocalPersonaColors.current
    StepScaffold(
        title = "How old are you?",
        subtitle = "Only to personalise age-related education. A band is enough; I never need your date of birth.",
        actionLabel = "Continue",
        onAction = onNext,
        secondary = { QuietButton(text = "Skip this") { onSelect(null); onNext() } },
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AgeBand.entries) { band ->
                SelectableCard(title = band.label, selected = state.ageBand == band, onClick = { onSelect(band) })
            }
            item {
                Text(
                    "This stays on your device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
fun PersonaStep(state: OnboardingState, onSelect: (String) -> Unit, onShowAll: () -> Unit, onNext: () -> Unit) {
    val colors = LocalPersonaColors.current
    StepScaffold(
        title = "Who's coming with you?",
        subtitle = "This is the voice you'll hear. You can swap any time.",
        actionLabel = "Continue",
        onAction = onNext,
        secondary = { if (!state.showAllPersonas) QuietButton(text = "See everyone", onClick = onShowAll) },
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.visiblePersonas) { persona ->
                val selected = state.personaId == persona.id
                Column {
                    SelectableCard(
                        title = persona.name,
                        subtitle = persona.tagline,
                        selected = selected,
                        onClick = { onSelect(persona.id) },
                    )
                    if (selected) {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AlterEgoCharacter(
                                state = CharacterState.SMILE,
                                primary = Color(persona.primaryColor),
                                accent = Color(persona.accentColor),
                                size = 120.dp,
                            )
                            Text(
                                persona.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntensityStep(state: OnboardingState, onSelect: (ReminderIntensity) -> Unit, onNext: () -> Unit) {
    StepScaffold(
        title = "How often should I check in?",
        subtitle = "Fewer, better interruptions beat constant noise.",
        actionLabel = "Continue",
        onAction = onNext,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReminderIntensity.entries.forEach { intensity ->
                SelectableCard(
                    title = intensity.label,
                    subtitle = intensity.subtitle,
                    selected = state.intensity == intensity,
                    onClick = { onSelect(intensity) },
                )
            }
        }
    }
}

@Composable
fun QuietHoursStep(state: OnboardingState, onChange: (LocalTime, LocalTime) -> Unit, onNext: () -> Unit) {
    val colors = LocalPersonaColors.current
    StepScaffold(
        title = "When should I leave you alone?",
        subtitle = "I will never speak during these hours.",
        actionLabel = "Continue",
        onAction = onNext,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QUIET_PRESETS.forEach { (label, range) ->
                SelectableCard(
                    title = label,
                    selected = state.quietHours.start == range.first && state.quietHours.end == range.second,
                    onClick = { onChange(range.first, range.second) },
                )
            }
            Text(
                "You can fine-tune this later in Me.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private val QUIET_PRESETS: List<Pair<String, Pair<LocalTime, LocalTime>>> = listOf(
    "10 PM to 7 AM" to (LocalTime.of(22, 0) to LocalTime.of(7, 0)),
    "11 PM to 6 AM" to (LocalTime.of(23, 0) to LocalTime.of(6, 0)),
    "9 PM to 8 AM" to (LocalTime.of(21, 0) to LocalTime.of(8, 0)),
    "Midnight to 6 AM" to (LocalTime.of(0, 0) to LocalTime.of(6, 0)),
)

@Composable
fun InterventionsStep(state: OnboardingState, onToggle: (String) -> Unit, onNext: () -> Unit) {
    StepScaffold(
        title = "When an urge hits, what helps?",
        subtitle = "Choose now, while it's easy. In the moment, I'll just tell you what to do.",
        actionLabel = "Continue",
        actionEnabled = state.selectedInterventions.isNotEmpty(),
        onAction = onNext,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.interventions) { intervention ->
                SelectableCard(
                    title = intervention.title,
                    subtitle = intervention.lines.firstOrNull(),
                    selected = intervention.id in state.selectedInterventions,
                    onClick = { onToggle(intervention.id) },
                )
            }
        }
    }
}

@Composable
fun NotificationsStep(state: OnboardingState, onAllow: () -> Unit, onSkip: () -> Unit) {
    val colors = LocalPersonaColors.current
    val persona = state.personas.firstOrNull { it.id == state.personaId }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlterEgoCharacter(
            state = CharacterState.LOOK,
            primary = Color(persona?.primaryColor ?: colors.primary.value.toLong()),
            accent = Color(persona?.accentColor ?: colors.accent.value.toLong()),
            size = 150.dp,
        )
        Spacer(Modifier.height(36.dp))
        Text("One last thing.", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground, textAlign = TextAlign.Center)
        Text(
            "I can't remind you if Android doesn't let me speak to you.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = "Allow reminders", onClick = onAllow)
        QuietButton(text = "Not now", modifier = Modifier.padding(top = 10.dp), onClick = onSkip)
    }
}

@Composable
fun FirstMomentStep(state: OnboardingState, onDone: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlterEgoCharacter(state = CharacterState.NOD, primary = colors.primary, accent = colors.accent, size = 150.dp)
        Spacer(Modifier.height(36.dp))
        Text("That's it.", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Text(
            "Go live. I'll find you later.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.muted,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = "Close", onClick = onDone)
    }
}
