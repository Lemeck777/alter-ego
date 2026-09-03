package com.alterego.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.content.MomentSelector
import com.alterego.app.core.data.AppClock
import com.alterego.app.core.data.CustomContentRepository
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.billing.EntitlementRepository
import com.alterego.app.domain.models.BiologyTimeline
import com.alterego.app.domain.models.Chapter
import com.alterego.app.domain.models.Commitment
import com.alterego.app.domain.models.CommitmentRule
import com.alterego.app.domain.models.Goal
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.Persona
import com.alterego.app.domain.models.TimeContext
import com.alterego.app.domain.models.TimelinePhase
import com.alterego.app.domain.usecases.ResetPatternAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class TodayState(
    val persona: Persona? = null,
    val commitment: Commitment? = null,
    val chapter: Chapter? = null,
    val dayNumber: Int = 0,
    val elapsedText: String = "",
    val supportLine: String = "",
    val todaysFocus: Moment? = null,
    val biologyPhase: TimelinePhase? = null,
    val showBiology: Boolean = false,
    val headsUp: String? = null,
    val loading: Boolean = true,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val customContent: CustomContentRepository,
    private val entitlements: EntitlementRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayState())
    val state: StateFlow<TodayState> = _state.asStateFlow()

    private var timeline: BiologyTimeline? = null

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            timeline = content.timeline()
            observe()
            tickElapsed()
        }
    }

    @Suppress("OPT_IN_USAGE")
    private fun observe() {
        combine(
            prefs.preferences,
            journey.observePrimaryCommitment(),
        ) { p, commitment -> p to commitment }
            .flatMapLatest { (p, commitment) ->
                val chapters = commitment?.let { journey.observeOpenChapter(it.id) } ?: flowOf(null)
                combine(chapters, flowOf(p to commitment)) { chapter, pair -> Triple(pair.first, pair.second, chapter) }
            }
            .onEach { (p, commitment, chapter) ->
                val persona = content.persona(p.personaId)
                val now = clock.now()
                val day = chapter?.dayNumber(now) ?: 0
                // Biology is only shown for commitments where it is actually relevant.
                val showBiology = commitment?.rule in setOf(CommitmentRule.NO_EJACULATION, CommitmentRule.NO_MASTURBATION) ||
                    commitment?.goal == Goal.RETENTION
                val pattern = ResetPatternAnalyzer().analyze(journey.allResets())
                val hour = now.atZone(ZoneId.systemDefault()).hour
                val riskHour = pattern.highRiskHour
                val nearRisk = pattern.isMeaningful && riskHour != null && ((riskHour - hour + 24) % 24) <= 2

                _state.value = _state.value.copy(
                    persona = persona,
                    commitment = commitment,
                    chapter = chapter,
                    dayNumber = day,
                    supportLine = supportLine(commitment, chapter, now),
                    todaysFocus = pickFocus(p.personaId, p.goals, p.faithEnabled, hour),
                    biologyPhase = if (showBiology) timeline?.phaseFor(day) else null,
                    showBiology = showBiology,
                    headsUp = if (nearRisk) pattern.headsUpText() else null,
                    elapsedText = elapsedText(chapter, now),
                    loading = false,
                )
            }
            .launchIn(viewModelScope)
    }

    /** Updates the live "16d 14h 22m" line without redrawing the rest of the screen. */
    private fun tickElapsed() {
        viewModelScope.launch {
            while (true) {
                val chapter = _state.value.chapter
                if (chapter != null) {
                    _state.value = _state.value.copy(elapsedText = elapsedText(chapter, clock.now()))
                }
                delay(30_000)
            }
        }
    }

    private fun elapsedText(chapter: Chapter?, now: Instant): String {
        if (chapter == null) return ""
        val duration = Duration.ofMillis(chapter.durationMillis(now))
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private fun supportLine(commitment: Commitment?, chapter: Chapter?, now: Instant): String = when {
        commitment == null -> "Set a commitment when you're ready."
        chapter == null -> "Start a chapter whenever you like."
        chapter.dayNumber(now) == 1 -> "Chapter ${chapter.number} starts today."
        else -> "You're still keeping your commitment."
    }

    private suspend fun pickFocus(personaId: String, goals: Set<String>, faithEnabled: Boolean, hour: Int): Moment? {
        val p = prefs.snapshot()
        return MomentSelector().select(
            library = content.allMoments(),
            request = MomentSelector.Request(
                personaId = personaId,
                goals = goals,
                ageBand = p.ageBand?.id,
                timeContext = TimeContext.forHour(hour),
                trigger = MomentTrigger.RANDOM,
                isPlus = entitlements.isPlus(),
                faithEnabled = faithEnabled,
                recentMomentIds = emptySet(),
                personalQuotes = customContent.personalQuoteMoments(personaId),
            ),
        )
    }

    fun refreshFocus() {
        viewModelScope.launch {
            val p = prefs.snapshot()
            val hour = clock.now().atZone(ZoneId.systemDefault()).hour
            _state.value = _state.value.copy(todaysFocus = pickFocus(p.personaId, p.goals, p.faithEnabled, hour))
        }
    }
}
