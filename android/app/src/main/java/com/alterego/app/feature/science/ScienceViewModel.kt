package com.alterego.app.feature.science

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.data.AppClock
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.models.AgeBand
import com.alterego.app.domain.models.BiologyTimeline
import com.alterego.app.domain.models.EvidenceClaim
import com.alterego.app.domain.models.Lesson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The four sections of the library, in the order they are shown. */
enum class ScienceCategory(val id: String, val heading: String) {
    YOUR_BODY("your_body", "Your Body"),
    RETENTION("retention", "Retention"),
    URGES_HABITS("urges_habits", "Urges & Habits"),
    AGE("age", "Age"),
}

data class ScienceState(
    val lessonsByCategory: Map<String, List<Lesson>> = emptyMap(),
    val claims: Map<String, EvidenceClaim> = emptyMap(),
    val timeline: BiologyTimeline = BiologyTimeline(disclaimer = "", phases = emptyList()),
    /** Day number of the open chapter of the primary commitment. 0 when nothing is being tracked. */
    val currentDay: Int = 0,
    val ageBand: AgeBand? = null,
    val loading: Boolean = true,
) {
    fun lessons(category: ScienceCategory): List<Lesson> = lessonsByCategory[category.id].orEmpty()
}

/**
 * Backs the whole science library: the lesson index, one lesson, and the biology timeline.
 *
 * Nothing here computes a number about the user's body. The only number it produces is the day
 * of their own chapter, which is a fact about their commitment, not about their physiology.
 */
@HiltViewModel
class ScienceViewModel @Inject constructor(
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val prefs: UserPreferencesRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(ScienceState())
    val state: StateFlow<ScienceState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            val lessons = content.lessons()
            val claims = content.claims().associateBy { it.claimId }
            val timeline = content.timeline()
            val chapter = journey.primaryCommitment()?.let { journey.openChapter(it.id) }
            _state.value = ScienceState(
                lessonsByCategory = lessons.groupBy { it.category },
                claims = claims,
                timeline = timeline,
                currentDay = chapter?.dayNumber(clock.now()) ?: 0,
                ageBand = prefs.snapshot().ageBand,
                loading = false,
            )
        }
    }

    fun lessonById(id: String): Lesson? =
        _state.value.lessonsByCategory.values.asSequence().flatten().firstOrNull { it.lessonId == id }

    fun claimById(id: String?): EvidenceClaim? = id?.let { _state.value.claims[it] }
}
