package com.alterego.app.feature.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.content.MomentSelector
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.models.Commitment
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.Persona
import com.alterego.app.domain.models.ResetContext
import com.alterego.app.domain.models.TimeContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ResetStage { CONFIRM, ACKNOWLEDGE, REFLECT, NEXT_CHAPTER }

data class ResetState(
    val stage: ResetStage = ResetStage.CONFIRM,
    val persona: Persona? = null,
    val commitment: Commitment? = null,
    val acknowledgement: Moment? = null,
    val selectedContext: ResetContext? = null,
    val newChapterNumber: Int = 0,
)

/**
 * The reset flow.
 *
 * Product rule, enforced here and in the copy: no red, no broken character, no "failure". A chapter
 * ended. The reflection is optional and never more than three taps.
 */
@HiltViewModel
class ResetViewModel @Inject constructor(
    private val journey: JourneyRepository,
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val analytics: Analytics,
) : ViewModel() {

    private val _state = MutableStateFlow(ResetState())
    val state: StateFlow<ResetState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            val p = prefs.snapshot()
            _state.value = _state.value.copy(
                persona = content.persona(p.personaId),
                commitment = journey.primaryCommitment(),
            )
        }
    }

    fun confirmReset() {
        viewModelScope.launch {
            val commitment = _state.value.commitment ?: return@launch
            val chapter = journey.logReset(commitment.id, context = null, note = null)
            val p = prefs.snapshot()
            val acknowledgement = MomentSelector().select(
                library = content.allMoments(),
                request = MomentSelector.Request(
                    personaId = p.personaId,
                    goals = p.goals,
                    ageBand = p.ageBand?.id,
                    timeContext = TimeContext.ANY,
                    trigger = MomentTrigger.RESET,
                    isPlus = true,
                    faithEnabled = p.faithEnabled,
                    recentMomentIds = emptySet(),
                ),
            )
            analytics.track(LocalAnalytics.CHAPTER_RESET)
            analytics.track(LocalAnalytics.CHAPTER_STARTED, mapOf("chapter" to chapter.number.toString()))
            _state.value = _state.value.copy(
                stage = ResetStage.ACKNOWLEDGE,
                acknowledgement = acknowledgement,
                newChapterNumber = chapter.number,
            )
        }
    }

    fun openReflection() { _state.value = _state.value.copy(stage = ResetStage.REFLECT) }

    /** Attaches the optional reflection to the reset we just recorded. */
    fun saveReflection(context: ResetContext) {
        viewModelScope.launch {
            val commitment = _state.value.commitment ?: return@launch
            // The reset row already exists; record the context on the most recent one.
            journey.resets(commitment.id).firstOrNull()?.let { journey.attachReflection(it.id, context, null) }
            analytics.track("reset_context", mapOf("context" to context.id))
            _state.value = _state.value.copy(selectedContext = context, stage = ResetStage.NEXT_CHAPTER)
        }
    }

    fun skipReflection() { _state.value = _state.value.copy(stage = ResetStage.NEXT_CHAPTER) }
}
