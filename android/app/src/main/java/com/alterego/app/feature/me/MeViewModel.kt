package com.alterego.app.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.billing.EntitlementRepository
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.data.AppClock
import com.alterego.app.core.data.CustomContentRepository
import com.alterego.app.core.data.JourneyRepository
import com.alterego.app.core.data.ScheduledReminder
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.scheduler.MomentScheduler
import com.alterego.app.core.security.AppLockManager
import com.alterego.app.domain.models.AgeBand
import com.alterego.app.domain.models.AppLockMode
import com.alterego.app.domain.models.Chapter
import com.alterego.app.domain.models.Commitment
import com.alterego.app.domain.models.CommitmentRule
import com.alterego.app.domain.models.FutureMessage
import com.alterego.app.domain.models.Goal
import com.alterego.app.domain.models.NotificationPrivacy
import com.alterego.app.domain.models.Persona
import com.alterego.app.domain.models.PersonalQuote
import com.alterego.app.domain.models.QuietHours
import com.alterego.app.domain.models.ReminderIntensity
import com.alterego.app.domain.usecases.JourneyStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Everything the "Me" tab and the screens underneath it need. One state object, one voice. */
data class MeState(
    val persona: Persona? = null,
    val personaName: String = "",
    val personaTagline: String = "",
    val goals: Set<Goal> = emptySet(),
    val ageBand: AgeBand? = null,
    val intensity: ReminderIntensity = ReminderIntensity.BALANCED,
    val quietHours: QuietHours = QuietHours.DEFAULT,
    val notificationPrivacy: NotificationPrivacy = NotificationPrivacy.PRIVATE,
    val appLock: AppLockMode = AppLockMode.NONE,
    val analyticsEnabled: Boolean = true,
    val isPlus: Boolean = false,
    val commitments: List<Commitment> = emptyList(),
    val quotes: List<PersonalQuote> = emptyList(),
    val futureMessages: List<FutureMessage> = emptyList(),
    val reminders: List<ScheduledReminder> = emptyList(),
    val daysTogether: Int = 0,
    val canUseBiometrics: Boolean = false,
    val canScheduleExact: Boolean = true,
) {
    /** Free tier keeps one primary commitment. Everything past that is Alter Ego+. */
    val canAddCommitment: Boolean
        get() = isPlus || commitments.size < EntitlementRepository.FREE_MAX_COMMITMENTS

    val canAddQuote: Boolean
        get() = isPlus || quotes.size < EntitlementRepository.FREE_MAX_PERSONAL_QUOTES
}

@HiltViewModel
class MeViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val customContent: CustomContentRepository,
    private val entitlements: EntitlementRepository,
    private val appLock: AppLockManager,
    private val scheduler: MomentScheduler,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(MeState())
    val state: StateFlow<MeState> = _state.asStateFlow()

    private val zone: ZoneId get() = ZoneId.systemDefault()

    init {
        prefs.preferences
            .onEach { p ->
                val persona = content.persona(p.personaId)
                _state.update {
                    it.copy(
                        persona = persona,
                        personaName = persona?.name ?: "",
                        personaTagline = persona?.tagline ?: "",
                        goals = p.goals.map { id -> Goal.fromId(id) }.toSet(),
                        ageBand = p.ageBand,
                        intensity = p.intensity,
                        quietHours = p.quietHours,
                        notificationPrivacy = p.notificationPrivacy,
                        appLock = p.appLock,
                        analyticsEnabled = p.analyticsEnabled,
                        daysTogether = daysSince(p.installedAtMillis),
                    )
                }
            }
            .launchIn(viewModelScope)

        entitlements.entitlement
            .onEach { e -> _state.update { it.copy(isPlus = e.isPlus) } }
            .launchIn(viewModelScope)

        journey.observeActiveCommitments()
            .onEach { list -> _state.update { it.copy(commitments = list) } }
            .launchIn(viewModelScope)

        customContent.observeQuotes()
            .onEach { list -> _state.update { it.copy(quotes = list) } }
            .launchIn(viewModelScope)

        customContent.observeFutureMessages()
            .onEach { list -> _state.update { it.copy(futureMessages = list) } }
            .launchIn(viewModelScope)

        customContent.observeReminders()
            .onEach { list -> _state.update { it.copy(reminders = list) } }
            .launchIn(viewModelScope)

        _state.update {
            it.copy(canUseBiometrics = appLock.canUseBiometrics(), canScheduleExact = scheduler.canScheduleExact())
        }
    }

    // ---------------------------------------------------------------- settings

    /** Changing how often we speak re-plans the rest of today immediately. */
    fun setIntensity(intensity: ReminderIntensity) {
        viewModelScope.launch {
            prefs.update { it.copy(intensity = intensity) }
            scheduler.planToday()
        }
    }

    fun setQuietHours(start: LocalTime, end: LocalTime) {
        viewModelScope.launch {
            prefs.update { it.copy(quietHours = QuietHours(start, end)) }
            scheduler.planToday()
        }
    }

    fun setNotificationPrivacy(privacy: NotificationPrivacy) {
        viewModelScope.launch { prefs.update { it.copy(notificationPrivacy = privacy) } }
    }

    /**
     * A PIN is only ever stored hashed, inside [AppLockManager]. An invalid PIN silently leaves the
     * previous setting alone rather than half-applying a lock the user cannot open.
     */
    fun setAppLock(mode: AppLockMode, pin: String? = null) {
        viewModelScope.launch {
            when (mode) {
                AppLockMode.PIN -> {
                    val candidate = pin?.trim().orEmpty()
                    if (candidate.length !in PIN_LENGTH || !candidate.all { it.isDigit() }) return@launch
                    appLock.setPin(candidate)
                }
                AppLockMode.NONE, AppLockMode.BIOMETRIC -> appLock.clearPin()
            }
            prefs.update { it.copy(appLock = mode) }
        }
    }

    fun toggleGoal(goal: Goal) {
        viewModelScope.launch {
            prefs.update { p ->
                val next = if (goal.id in p.goals) p.goals - goal.id else p.goals + goal.id
                p.copy(goals = next, faithEnabled = Goal.FAITH.id in next)
            }
        }
    }

    fun setAgeBand(band: AgeBand?) {
        viewModelScope.launch { prefs.update { it.copy(ageBand = band) } }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.update { it.copy(analyticsEnabled = enabled) } }
    }

    // ------------------------------------------------- teach me what to say

    fun addQuote(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || !_state.value.canAddQuote) return
        viewModelScope.launch { customContent.addQuote(trimmed) }
    }

    fun deleteQuote(id: Long) {
        viewModelScope.launch { customContent.deleteQuote(id) }
    }

    fun toggleQuote(id: Long, enabled: Boolean) {
        viewModelScope.launch { customContent.setQuoteEnabled(id, enabled) }
    }

    // ------------------------------------------------------------- future me

    fun addFutureMessage(text: String, deliverAt: Instant) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { customContent.addFutureMessage(trimmed, deliverAt) }
    }

    fun deleteFutureMessage(id: Long) {
        viewModelScope.launch { customContent.deleteFutureMessage(id) }
    }

    /** The delivery-date chips on Future Me. Uses the injected clock so it stays testable. */
    fun futureDate(monthsAhead: Long): Instant =
        clock.now().atZone(zone).plusMonths(monthsAhead).toInstant()

    // ------------------------------------------------------------- reminders

    fun upsertReminder(reminder: ScheduledReminder) {
        viewModelScope.launch {
            val newId = customContent.upsertReminder(reminder)
            val saved = reminder.copy(id = if (reminder.id == 0L) newId else reminder.id)
            scheduler.cancel(saved.id)
            if (saved.enabled) scheduler.schedule(saved)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            scheduler.cancel(id)
            customContent.deleteReminder(id)
        }
    }

    // ----------------------------------------------------------- commitments

    fun setPrimaryCommitment(id: Long) {
        viewModelScope.launch { journey.setPrimary(id) }
    }

    /** Pausing is not failing. The commitment stops asking; its history stays exactly as it was. */
    fun pauseCommitment(id: Long) {
        viewModelScope.launch { journey.deactivate(id) }
    }

    fun createCommitment(goal: Goal, rule: CommitmentRule, customRule: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val trimmedRule = customRule.trim().takeIf { it.isNotBlank() }
            journey.createCommitment(
                goal = goal,
                rule = rule,
                customRule = trimmedRule,
                title = titleFor(goal, rule, trimmedRule),
                primary = journey.activeCount() == 0,
            )
            onDone()
        }
    }

    // ----------------------------------------------------------------- data

    /** Removes chapters, resets, urges and commitments. Preferences and companion stay. */
    fun deleteHistory() {
        viewModelScope.launch {
            journey.clearHistory()
            scheduler.planToday()
        }
    }

    /** Everything: history, personal lines, future messages, reminders, PIN and preferences. */
    fun deleteEverything() {
        viewModelScope.launch {
            _state.value.reminders.forEach { scheduler.cancel(it.id) }
            scheduler.cancelPlannedMoments()
            journey.clearHistory()
            customContent.clearAll()
            appLock.clearPin()
            prefs.clearAll()
        }
    }

    /**
     * A plain-text copy of what the user owns: their chapters, their resets and the reflective
     * numbers behind them. No scores, no comparisons, nothing they did not write.
     */
    suspend fun exportJourney(): String {
        val now = clock.now()
        val p = prefs.snapshot()
        val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(zone)
        val stampFormat = DateTimeFormatter.ofPattern("d MMM yyyy 'at' HH:mm").withZone(zone)
        val togetherSince = if (p.installedAtMillis > 0L) Instant.ofEpochMilli(p.installedAtMillis) else now
        val calculator = JourneyStatsCalculator(zone)

        val out = StringBuilder()
        out.appendLine("Alter Ego — my journey")
        out.appendLine("Exported ${stampFormat.format(now)}")
        out.appendLine()
        out.appendLine("Together since ${dateFormat.format(togetherSince)} (${daysSince(togetherSince.toEpochMilli())} days).")
        out.appendLine()

        val commitments = journey.activeCommitments()
        if (commitments.isEmpty()) {
            out.appendLine("No commitments recorded yet.")
        }
        commitments.forEach { commitment ->
            val chapters = journey.chapters(commitment.id)
            val resets = journey.resets(commitment.id)
            val stats = calculator.compute(chapters, resets, togetherSince, now)

            out.appendLine("COMMITMENT: ${commitment.title}")
            out.appendLine("  Rule: ${commitment.customRule ?: commitment.rule.label}")
            out.appendLine("  Focus: ${commitment.goal.label}")
            out.appendLine("  Started: ${dateFormat.format(commitment.createdAt)}")
            out.appendLine("  Primary: ${if (commitment.isPrimary) "yes" else "no"}")
            out.appendLine()

            out.appendLine("  Chapters")
            if (chapters.isEmpty()) out.appendLine("    (none yet)")
            chapters.sortedBy { it.number }.forEach { chapter ->
                out.appendLine("    ${chapterLine(chapter, now, dateFormat)}")
            }
            out.appendLine()

            out.appendLine("  Resets")
            if (resets.isEmpty()) out.appendLine("    (none recorded)")
            resets.sortedBy { it.occurredAt }.forEach { reset ->
                val context = reset.context?.label ?: "no reason given"
                out.appendLine("    ${dateFormat.format(reset.occurredAt)} — $context")
                reset.note?.takeIf { it.isNotBlank() }?.let { out.appendLine("      \"${it.trim()}\"") }
            }
            out.appendLine()

            out.appendLine("  Where it stands")
            out.appendLine("    Current chapter day: ${stats.currentDay}")
            out.appendLine("    Chapters completed: ${stats.chaptersCompleted}")
            out.appendLine("    Longest chapter: ${stats.longestChapterDays} days")
            out.appendLine("    Average chapter: ${stats.averageChapterDays} days")
            out.appendLine("    Committed days, lifetime: ${stats.lifetimeCommittedDays}")
            out.appendLine("    Committed days this year: ${stats.committedDaysThisYear}")
            out.appendLine("    Fresh starts this year: ${stats.restartsThisYear}")
            out.appendLine()
        }

        val quotes = _state.value.quotes
        out.appendLine("MY OWN WORDS")
        if (quotes.isEmpty()) out.appendLine("  (none yet)")
        quotes.forEach { out.appendLine("  \"${it.text}\"") }
        out.appendLine()

        val futures = _state.value.futureMessages
        out.appendLine("MESSAGES TO MY FUTURE SELF")
        if (futures.isEmpty()) out.appendLine("  (none yet)")
        futures.sortedBy { it.deliverAt }.forEach {
            out.appendLine("  Arrives ${dateFormat.format(it.deliverAt)}: ${it.text}")
        }

        return out.toString()
    }

    // ----------------------------------------------------------------- utils

    private fun chapterLine(chapter: Chapter, now: Instant, format: DateTimeFormatter): String {
        val days = (chapter.durationMillis(now) / Chapter.MILLIS_PER_DAY).toInt()
        val ended = chapter.endedAt?.let { "ended ${format.format(it)}" } ?: "still open"
        return "Chapter ${chapter.number}: started ${format.format(chapter.startedAt)}, $ended ($days days)"
    }

    private fun daysSince(millis: Long): Int {
        if (millis <= 0L) return 0
        return ChronoUnit.DAYS.between(Instant.ofEpochMilli(millis), clock.now()).toInt().coerceAtLeast(0)
    }

    private fun titleFor(goal: Goal, rule: CommitmentRule, customRule: String?): String = when {
        !customRule.isNullOrBlank() -> customRule
        goal == Goal.RETENTION || goal == Goal.PORN_AVOIDANCE -> rule.label
        else -> goal.label
    }

    companion object {
        val PIN_LENGTH = 4..8
    }
}
