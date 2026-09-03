package com.alterego.app.feature.moment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.animation.Haptics
import com.alterego.app.core.billing.EntitlementRepository
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.content.MomentSelector
import com.alterego.app.core.data.AppClock
import com.alterego.app.core.data.CustomContentRepository
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.MomentAction
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.Persona
import com.alterego.app.domain.models.TimeContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class MomentUiState(
    val moment: Moment? = null,
    val persona: Persona? = null,
    val saved: Boolean = false,
    val inUrgeMode: Boolean = false,
    val loading: Boolean = true,
)

@HiltViewModel
class MomentViewModel @Inject constructor(
    private val content: ContentRepository,
    private val customContent: CustomContentRepository,
    private val journey: JourneyRepository,
    private val prefs: UserPreferencesRepository,
    private val entitlements: EntitlementRepository,
    private val haptics: Haptics,
    private val analytics: Analytics,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(MomentUiState())
    val state: StateFlow<MomentUiState> = _state.asStateFlow()

    private var deliveryId: Long = -1L

    fun load(momentId: String?, deliveryId: Long, startInUrgeMode: Boolean) {
        this.deliveryId = deliveryId
        viewModelScope.launch {
            content.ensureLoaded()
            val p = prefs.snapshot()
            val persona = content.persona(p.personaId)
            // A moment id that starts with "personal_" is one of the user's own lines.
            val moment = when {
                momentId == null -> selectFallback()
                momentId.startsWith("personal_") ->
                    customContent.personalQuoteMoments(p.personaId).firstOrNull { it.id == momentId } ?: selectFallback()
                else -> content.moment(momentId) ?: selectFallback()
            }
            _state.value = MomentUiState(
                moment = moment,
                persona = persona,
                saved = moment?.let { customContent.isSaved(it.id) } ?: false,
                inUrgeMode = startInUrgeMode,
                loading = false,
            )
            moment?.let { haptics.play(it.haptic) }
            if (deliveryId >= 0) {
                customContent.markOpened(deliveryId)
                analytics.track(LocalAnalytics.MOMENT_OPENED, mapOf("category" to (moment?.category?.id ?: "unknown")))
            }
        }
    }

    private suspend fun selectFallback(): Moment? {
        val p = prefs.snapshot()
        val hour = clock.now().atZone(ZoneId.systemDefault()).hour
        return MomentSelector().select(
            library = content.allMoments(),
            request = MomentSelector.Request(
                personaId = p.personaId,
                goals = p.goals,
                ageBand = p.ageBand?.id,
                timeContext = TimeContext.forHour(hour),
                trigger = MomentTrigger.RANDOM,
                isPlus = entitlements.isPlus(),
                faithEnabled = p.faithEnabled,
                recentMomentIds = customContent.recentMomentIds(24L * 60 * 60 * 1000),
                personalQuotes = customContent.personalQuoteMoments(p.personaId),
            ),
        )
    }

    fun onAction(action: MomentAction, onFinish: () -> Unit) {
        viewModelScope.launch {
            when (action.type) {
                "urge_mode", "breathe" -> _state.value = _state.value.copy(inUrgeMode = true)
                "save" -> toggleSave()
                "snooze" -> { record("snooze"); onFinish() }
                else -> { record("good"); onFinish() }
            }
        }
    }

    fun toggleSave() {
        val moment = _state.value.moment ?: return
        viewModelScope.launch {
            if (_state.value.saved) customContent.unsave(moment.id) else customContent.save(moment.id)
            _state.value = _state.value.copy(saved = !_state.value.saved)
        }
    }

    fun dismiss() {
        viewModelScope.launch { record("dismiss") }
    }

    fun exitUrgeMode() { _state.value = _state.value.copy(inUrgeMode = false) }

    private suspend fun record(reaction: String) {
        if (deliveryId >= 0) customContent.setReaction(deliveryId, reaction)
        analytics.track("moment_reaction", mapOf("reaction" to reaction, "surface" to "screen"))
    }
}
