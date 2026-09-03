package com.alterego.app.feature.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.data.AppClock
import com.alterego.app.core.data.CustomContentRepository
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.models.Chapter
import com.alterego.app.domain.models.Commitment
import com.alterego.app.domain.models.JourneyStats
import com.alterego.app.domain.models.LifeTimelineEntry
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.ResetEvent
import com.alterego.app.domain.usecases.JourneyStatsCalculator
import com.alterego.app.domain.usecases.ResetPatternAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Everything the Journey surface needs, in one reflective snapshot.
 *
 * [stats] is null only while the first load is in flight; it is never "reset" to zero by a restart.
 */
data class JourneyState(
    val loading: Boolean = true,
    val currentCommitment: Commitment? = null,
    val currentChapter: Chapter? = null,
    val stats: JourneyStats? = null,
    val chapters: List<Chapter> = emptyList(),
    val resets: List<ResetEvent> = emptyList(),
    val pattern: ResetPatternAnalyzer.Pattern = ResetPatternAnalyzer.Pattern(null, 0.0, null, 0.0, 0),
    val lifeTimeline: List<LifeTimelineEntry> = emptyList(),
    val savedMoments: List<Moment> = emptyList(),
    val togetherSince: Instant = Instant.EPOCH,
    val personaName: String = "",
)

/** The commitment-scoped half of the state, so the three flows below it can be swapped as one. */
private data class CommitmentSnapshot(
    val commitment: Commitment?,
    val chapters: List<Chapter>,
    val resets: List<ResetEvent>,
)

@HiltViewModel
class JourneyViewModel @Inject constructor(
    private val journey: JourneyRepository,
    private val customContent: CustomContentRepository,
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val statsCalculator = JourneyStatsCalculator()
    private val patternAnalyzer = ResetPatternAnalyzer()

    private val _state = MutableStateFlow(JourneyState())
    val state: StateFlow<JourneyState> = _state.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val commitmentSnapshot: Flow<CommitmentSnapshot> =
        journey.observePrimaryCommitment().flatMapLatest { commitment ->
            if (commitment == null) {
                flowOf(CommitmentSnapshot(null, emptyList(), emptyList()))
            } else {
                combine(
                    journey.observeChapters(commitment.id),
                    journey.observeResets(commitment.id),
                ) { chapters, resets ->
                    CommitmentSnapshot(commitment, chapters.sortedBy { it.number }, resets)
                }
            }
        }

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            combine(
                commitmentSnapshot,
                customContent.observeLifeTimeline(),
                customContent.observeSaved(),
                prefs.preferences,
            ) { snapshot, timeline, saved, preferences ->
                val now = clock.now()
                val togetherSince = togetherSince(preferences.installedAtMillis, snapshot.chapters, now)
                JourneyState(
                    loading = false,
                    currentCommitment = snapshot.commitment,
                    currentChapter = snapshot.chapters.firstOrNull { it.isOpen },
                    stats = statsCalculator.compute(
                        chapters = snapshot.chapters,
                        resets = snapshot.resets,
                        togetherSince = togetherSince,
                        now = now,
                    ),
                    chapters = snapshot.chapters,
                    resets = snapshot.resets,
                    pattern = patternAnalyzer.analyze(snapshot.resets),
                    lifeTimeline = timeline,
                    savedMoments = saved.mapNotNull { content.moment(it.momentId) },
                    togetherSince = togetherSince,
                    personaName = content.persona(preferences.personaId)?.name.orEmpty(),
                )
            }.collect { _state.value = it }
        }
    }

    /**
     * The install date is the anchor for "together since". If it was never written (upgrade from a
     * build before it existed) we fall back to the first chapter rather than showing 1970.
     */
    private fun togetherSince(installedAtMillis: Long, chapters: List<Chapter>, now: Instant): Instant =
        if (installedAtMillis > 0L) {
            Instant.ofEpochMilli(installedAtMillis)
        } else {
            chapters.minByOrNull { it.startedAt }?.startedAt ?: now
        }
}
