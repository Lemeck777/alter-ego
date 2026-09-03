package com.alterego.app.domain.models

import java.time.Instant
import java.time.LocalTime

data class Persona(
    val id: String,
    val name: String,
    val tagline: String,
    val archetype: String,
    val description: String,
    val defaultTone: String,
    val voiceRules: List<String>,
    val primaryColor: Long,
    val accentColor: Long,
    val backgroundColor: Long,
    val recommendedFor: List<String>,
    val premium: Boolean,
    val isCustom: Boolean = false,
)

data class MomentAction(val label: String, val type: String)

data class Moment(
    val id: String,
    val persona: String,
    val goal: String,
    val category: MomentCategory,
    val tone: String,
    val intensity: Int,
    val ageBands: List<String>,
    val timeContext: TimeContext,
    val trigger: MomentTrigger,
    val lines: List<String>,
    val actions: List<MomentAction>,
    val animation: CharacterState,
    val haptic: HapticPattern,
    val evidenceType: String,
    val source: String?,
    val premium: Boolean,
    /** True for user-authored "Teach me what to say" quotes. Weighted higher by the selector. */
    val isPersonal: Boolean = false,
)

data class QuietHours(val start: LocalTime, val end: LocalTime) {
    /**
     * Handles windows that cross midnight (22:00 -> 07:00).
     *
     * A start equal to the end means the whole day is quiet, not none of it. Staying silent is the
     * safe reading of an ambiguous setting: the alternative would interrupt someone at 3 AM.
     */
    fun contains(time: LocalTime): Boolean = when {
        start == end -> true
        start < end -> !time.isBefore(start) && time.isBefore(end)
        else -> !time.isBefore(start) || time.isBefore(end)
    }

    companion object { val DEFAULT = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0)) }
}

data class Commitment(
    val id: Long,
    val goal: Goal,
    val rule: CommitmentRule,
    val customRule: String?,
    val title: String,
    val createdAt: Instant,
    val isPrimary: Boolean,
    val isActive: Boolean,
)

/** A chapter is a continuous period of keeping a commitment. It ends with a reset, never with "failure". */
data class Chapter(
    val id: Long,
    val commitmentId: Long,
    val number: Int,
    val startedAt: Instant,
    val endedAt: Instant?,
) {
    val isOpen: Boolean get() = endedAt == null
    fun durationMillis(now: Instant): Long = ((endedAt ?: now).toEpochMilli() - startedAt.toEpochMilli()).coerceAtLeast(0)
    fun dayNumber(now: Instant): Int = (durationMillis(now) / MILLIS_PER_DAY).toInt() + 1

    companion object { const val MILLIS_PER_DAY = 86_400_000L }
}

data class ResetEvent(
    val id: Long,
    val commitmentId: Long,
    val chapterId: Long,
    val occurredAt: Instant,
    val context: ResetContext?,
    val note: String?,
    val hourOfDay: Int,
)

data class UrgeEvent(
    val id: Long,
    val startedAt: Instant,
    val initialLevel: UrgeLevel,
    val finalLevel: UrgeLevel?,
    val interventionIds: List<String>,
    val completed: Boolean,
    val hourOfDay: Int,
)

data class Intervention(
    val id: String,
    val title: String,
    val category: String,
    val durationSeconds: Int,
    val defaultEnabled: Boolean,
    val lines: List<String>,
    val personaLines: Map<String, String>,
)

data class EvidenceClaim(
    val claimId: String,
    val claim: String,
    val topic: String,
    val ageMin: Int?,
    val ageMax: Int?,
    val evidenceLevel: EvidenceLevel,
    val direction: String,
    val sourceUrl: String?,
    val sourceTitle: String,
    val publicationYear: Int?,
    val studyType: String,
    val reviewDate: String,
    val medicalReviewer: String,
    val status: String,
)

data class LessonBlock(val type: String, val text: String?, val claimId: String?)

data class Lesson(
    val lessonId: String,
    val category: String,
    val title: String,
    val readSeconds: Int,
    val blocks: List<LessonBlock>,
)

data class TimelinePhase(
    val phaseId: String,
    val dayFrom: Int,
    val dayTo: Int?,
    val title: String,
    val summary: String,
    val claimIds: List<String>,
) {
    fun contains(day: Int): Boolean = day >= dayFrom && (dayTo == null || day <= dayTo)
}

data class BiologyTimeline(val disclaimer: String, val phases: List<TimelinePhase>) {
    fun phaseFor(day: Int): TimelinePhase? = phases.firstOrNull { it.contains(day) } ?: phases.lastOrNull()
}

data class PersonalQuote(val id: Long, val text: String, val createdAt: Instant, val enabled: Boolean)

data class FutureMessage(val id: Long, val text: String, val createdAt: Instant, val deliverAt: Instant, val deliveredAt: Instant?)

data class SavedMoment(val momentId: String, val savedAt: Instant)

/** Reflective, non-competitive statistics for the Journey screen. */
data class JourneyStats(
    val currentChapter: Chapter?,
    val currentDay: Int,
    val chaptersCompleted: Int,
    val committedDaysThisYear: Int,
    val restartsThisYear: Int,
    val alignedPercentThisYear: Int,
    val longestChapterDays: Int,
    val averageChapterDays: Int,
    val lifetimeCommittedDays: Int,
    val lifetimeDays: Int,
    val togetherSince: Instant,
    val daysTogether: Int,
    val mostCommonResetHour: Int?,
    val mostCommonResetContext: ResetContext?,
)

data class LifeTimelineEntry(val year: Int, val text: String, val at: Instant)

data class Entitlement(val isPlus: Boolean, val source: String, val expiresAt: Instant?) {
    companion object { val FREE = Entitlement(false, "free", null) }
}
