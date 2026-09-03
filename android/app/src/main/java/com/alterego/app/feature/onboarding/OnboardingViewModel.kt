package com.alterego.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.scheduler.MomentScheduler
import com.alterego.app.domain.models.AgeBand
import com.alterego.app.domain.models.CommitmentRule
import com.alterego.app.domain.models.Goal
import com.alterego.app.domain.models.Intervention
import com.alterego.app.domain.models.Persona
import com.alterego.app.domain.models.QuietHours
import com.alterego.app.domain.models.ReminderIntensity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

enum class OnboardingStep { WELCOME, GOALS, COMMITMENT_RULE, AGE, PERSONA, INTENSITY, QUIET_HOURS, INTERVENTIONS, NOTIFICATIONS, DONE }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val selectedGoals: Set<Goal> = emptySet(),
    val rule: CommitmentRule? = null,
    val customRule: String = "",
    val ageBand: AgeBand? = null,
    val personaId: String = "sage",
    val personas: List<Persona> = emptyList(),
    val showAllPersonas: Boolean = false,
    val intensity: ReminderIntensity = ReminderIntensity.BALANCED,
    val quietHours: QuietHours = QuietHours.DEFAULT,
    val interventions: List<Intervention> = emptyList(),
    val selectedInterventions: Set<String> = emptySet(),
    val saving: Boolean = false,
) {
    val needsCommitmentRule: Boolean
        get() = Goal.RETENTION in selectedGoals || Goal.PORN_AVOIDANCE in selectedGoals

    /** Three recommended companions first; the full list is one tap away. */
    val visiblePersonas: List<Persona>
        get() {
            if (showAllPersonas) return personas
            val goalIds = selectedGoals.map { it.id }.toSet()
            val ranked = personas.sortedByDescending { p -> p.recommendedFor.count { it in goalIds } }
            return ranked.take(3)
        }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val scheduler: MomentScheduler,
    private val analytics: Analytics,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            val personas = content.observePersonas()
            val interventions = content.interventions()
            _state.value = _state.value.copy(
                interventions = interventions,
                selectedInterventions = interventions.filter { it.defaultEnabled }.map { it.id }.toSet(),
            )
            personas.collect { list -> _state.value = _state.value.copy(personas = list.filter { !it.premium }) }
        }
    }

    fun toggleGoal(goal: Goal) {
        val current = _state.value.selectedGoals
        _state.value = _state.value.copy(
            selectedGoals = if (goal in current) current - goal else current + goal,
        )
    }

    fun setRule(rule: CommitmentRule) { _state.value = _state.value.copy(rule = rule) }
    fun setCustomRule(text: String) { _state.value = _state.value.copy(customRule = text) }
    fun setAgeBand(band: AgeBand?) { _state.value = _state.value.copy(ageBand = band) }
    fun setPersona(id: String) { _state.value = _state.value.copy(personaId = id) }
    fun showAllPersonas() { _state.value = _state.value.copy(showAllPersonas = true) }
    fun setIntensity(intensity: ReminderIntensity) { _state.value = _state.value.copy(intensity = intensity) }
    fun setQuietHours(start: LocalTime, end: LocalTime) { _state.value = _state.value.copy(quietHours = QuietHours(start, end)) }

    fun toggleIntervention(id: String) {
        val current = _state.value.selectedInterventions
        _state.value = _state.value.copy(selectedInterventions = if (id in current) current - id else current + id)
    }

    fun next() {
        val s = _state.value
        val order = OnboardingStep.entries
        var index = order.indexOf(s.step) + 1
        // Only ask about the precise rule when the user chose a goal that needs one.
        if (order[index] == OnboardingStep.COMMITMENT_RULE && !s.needsCommitmentRule) index++
        _state.value = s.copy(step = order[index])
    }

    fun back() {
        val s = _state.value
        val order = OnboardingStep.entries
        var index = (order.indexOf(s.step) - 1).coerceAtLeast(0)
        if (order[index] == OnboardingStep.COMMITMENT_RULE && !s.needsCommitmentRule) index = (index - 1).coerceAtLeast(0)
        _state.value = s.copy(step = order[index])
    }

    /** Saves everything, creates the first commitment and arms today's Moments. */
    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch {
            prefs.update {
                it.copy(
                    onboarded = true,
                    personaId = s.personaId,
                    goals = s.selectedGoals.map { g -> g.id }.toSet(),
                    ageBand = s.ageBand,
                    intensity = s.intensity,
                    quietHours = s.quietHours,
                    enabledInterventions = s.selectedInterventions,
                    faithEnabled = Goal.FAITH in s.selectedGoals,
                    installedAtMillis = if (it.installedAtMillis == 0L) System.currentTimeMillis() else it.installedAtMillis,
                )
            }
            val primaryGoal = s.selectedGoals.firstOrNull() ?: Goal.DISCIPLINE
            val rule = s.rule ?: if (s.needsCommitmentRule) CommitmentRule.NO_EJACULATION else CommitmentRule.TRACK_FREQUENCY
            journey.createCommitment(
                goal = primaryGoal,
                rule = rule,
                customRule = s.customRule.takeIf { it.isNotBlank() },
                title = titleFor(primaryGoal, rule, s.customRule),
                primary = true,
            )
            scheduler.planToday()
            analytics.track(
                LocalAnalytics.ONBOARDING_COMPLETED,
                mapOf("goals" to s.selectedGoals.joinToString(",") { it.id }, "persona" to s.personaId, "intensity" to s.intensity.id),
            )
            onDone()
        }
    }

    fun onNotificationsDenied() = analytics.track(LocalAnalytics.NOTIFICATIONS_DENIED)

    private fun titleFor(goal: Goal, rule: CommitmentRule, customRule: String): String = when {
        customRule.isNotBlank() -> customRule.trim()
        goal == Goal.RETENTION || goal == Goal.PORN_AVOIDANCE -> rule.label
        else -> goal.label
    }
}
