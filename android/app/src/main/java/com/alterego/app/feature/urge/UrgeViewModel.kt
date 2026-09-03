package com.alterego.app.feature.urge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.animation.Haptics
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.notifications.MomentNotifier
import com.alterego.app.domain.models.HapticPattern
import com.alterego.app.domain.models.Intervention
import com.alterego.app.domain.models.Persona
import com.alterego.app.domain.models.UrgeLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UrgeStage { OPENING, INTERVENTION, TIMER, CHECK_IN, CLOSING }

data class UrgeState(
    val stage: UrgeStage = UrgeStage.OPENING,
    val persona: Persona? = null,
    val personaLine: String = "",
    val interventions: List<Intervention> = emptyList(),
    val currentIntervention: Intervention? = null,
    val secondsRemaining: Int = TEN_MINUTES_SECONDS,
    val usedInterventionIds: List<String> = emptyList(),
    val finalLevel: UrgeLevel? = null,
    val closingLine: String = "",
) {
    val timerText: String
        get() = "%02d:%02d".format(secondsRemaining / 60, secondsRemaining % 60)

    companion object { const val TEN_MINUTES_SECONDS = 600 }
}

@HiltViewModel
class UrgeViewModel @Inject constructor(
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val prefs: UserPreferencesRepository,
    private val notifier: MomentNotifier,
    private val haptics: Haptics,
    private val analytics: Analytics,
) : ViewModel() {

    private val _state = MutableStateFlow(UrgeState())
    val state: StateFlow<UrgeState> = _state.asStateFlow()

    private var urgeId: Long = -1L
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            val p = prefs.snapshot()
            val persona = content.persona(p.personaId)
            val all = content.interventions()
            val enabled = all.filter { it.id in p.enabledInterventions }.ifEmpty { all.filter { it.defaultEnabled } }
            _state.value = _state.value.copy(
                persona = persona,
                interventions = enabled,
                personaLine = "Don't decide anything yet.",
            )
            urgeId = journey.startUrge(UrgeLevel.MEDIUM)
            analytics.track(LocalAnalytics.URGE_STARTED)
            haptics.play(HapticPattern.HEARTBEAT)
        }
    }

    fun chooseIntervention(intervention: Intervention) {
        val personaId = _state.value.persona?.id
        _state.value = _state.value.copy(
            stage = UrgeStage.INTERVENTION,
            currentIntervention = intervention,
            personaLine = intervention.personaLines[personaId] ?: intervention.lines.firstOrNull().orEmpty(),
            usedInterventionIds = _state.value.usedInterventionIds + intervention.id,
        )
    }

    fun showInterventions() { _state.value = _state.value.copy(stage = UrgeStage.OPENING) }

    /** "Give me ten minutes." The ask is never "fight this forever". */
    fun startTimer() {
        if (timerJob != null) return
        _state.value = _state.value.copy(stage = UrgeStage.TIMER, secondsRemaining = UrgeState.TEN_MINUTES_SECONDS)
        timerJob = viewModelScope.launch {
            while (_state.value.secondsRemaining > 0) {
                delay(1000)
                val remaining = _state.value.secondsRemaining - 1
                _state.value = _state.value.copy(secondsRemaining = remaining)
                if (remaining % 30 == 0) notifier.notifyUrgeTimer(_state.value.timerText)
            }
            notifier.cancelUrgeTimer()
            haptics.play(HapticPattern.SOFT)
            _state.value = _state.value.copy(stage = UrgeStage.CHECK_IN)
        }
    }

    fun skipToCheckIn() {
        timerJob?.cancel()
        timerJob = null
        notifier.cancelUrgeTimer()
        _state.value = _state.value.copy(stage = UrgeStage.CHECK_IN)
    }

    fun reportLevel(level: UrgeLevel) {
        viewModelScope.launch {
            _state.value = _state.value.copy(finalLevel = level)
            when (level) {
                UrgeLevel.HIGH -> {
                    // Still strong: offer another intervention rather than declaring victory.
                    _state.value = _state.value.copy(
                        stage = UrgeStage.OPENING,
                        personaLine = "Still strong. That's normal. Let's change something else.",
                    )
                }
                else -> {
                    journey.updateUrge(urgeId, level, _state.value.usedInterventionIds, completed = true)
                    analytics.track(
                        LocalAnalytics.URGE_COMPLETED,
                        mapOf("final_level" to level.id, "interventions" to _state.value.usedInterventionIds.size.toString()),
                    )
                    _state.value = _state.value.copy(
                        stage = UrgeStage.CLOSING,
                        closingLine = if (level == UrgeLevel.LOW) "Good. Go do something else." else "It's passing. Stay busy for a bit.",
                    )
                }
            }
        }
    }

    fun abandon() {
        viewModelScope.launch {
            timerJob?.cancel()
            notifier.cancelUrgeTimer()
            journey.updateUrge(urgeId, _state.value.finalLevel, _state.value.usedInterventionIds, completed = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        notifier.cancelUrgeTimer()
    }
}
