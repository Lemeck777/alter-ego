package com.alterego.app.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire + asset format produced by scripts/sync-content.mjs. The same bundle is bundled in
 * assets and served by GET /v1/content/bundle so remote science updates need no app release.
 */
@Serializable
data class ContentBundle(
    val version: Long,
    @SerialName("generated_at") val generatedAt: String,
    val personas: List<PersonaDto> = emptyList(),
    val moments: List<MomentDto> = emptyList(),
    val interventions: List<InterventionDto> = emptyList(),
    val claims: List<ClaimDto> = emptyList(),
    val lessons: List<LessonDto> = emptyList(),
    val timeline: TimelineDto,
)

@Serializable
data class PaletteDto(val primary: String, val accent: String, val background: String)

@Serializable
data class PersonaDto(
    val id: String,
    val name: String,
    val tagline: String,
    val archetype: String,
    val description: String,
    @SerialName("default_tone") val defaultTone: String,
    @SerialName("voice_rules") val voiceRules: List<String> = emptyList(),
    val palette: PaletteDto,
    @SerialName("recommended_for") val recommendedFor: List<String> = emptyList(),
    @SerialName("animation_set") val animationSet: String = id,
    val premium: Boolean = false,
)

@Serializable
data class MomentActionDto(val label: String, val type: String)

@Serializable
data class MomentDto(
    val id: String,
    val persona: String,
    val goal: String,
    val category: String,
    val tone: String,
    val intensity: Int,
    @SerialName("age_bands") val ageBands: List<String> = emptyList(),
    @SerialName("time_context") val timeContext: String = "any",
    val trigger: String = "random",
    val lines: List<String>,
    val actions: List<MomentActionDto> = emptyList(),
    val animation: String = "look",
    val haptic: String = "soft",
    @SerialName("evidence_type") val evidenceType: String = "none",
    val source: String? = null,
    @SerialName("safety_level") val safetyLevel: String = "safe",
    val premium: Boolean = false,
)

@Serializable
data class InterventionDto(
    val id: String,
    val title: String,
    val category: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("default_enabled") val defaultEnabled: Boolean = true,
    val lines: List<String> = emptyList(),
    @SerialName("persona_lines") val personaLines: Map<String, String> = emptyMap(),
)

@Serializable
data class ClaimDto(
    @SerialName("claim_id") val claimId: String,
    val claim: String,
    val topic: String,
    @SerialName("age_min") val ageMin: Int? = null,
    @SerialName("age_max") val ageMax: Int? = null,
    @SerialName("evidence_level") val evidenceLevel: String,
    val direction: String = "",
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("source_title") val sourceTitle: String = "",
    @SerialName("publication_year") val publicationYear: Int? = null,
    @SerialName("study_type") val studyType: String = "",
    @SerialName("review_date") val reviewDate: String = "",
    @SerialName("medical_reviewer") val medicalReviewer: String = "",
    @SerialName("review_note") val reviewNote: String? = null,
    val status: String = "active",
)

@Serializable
data class LessonBlockDto(val type: String, val text: String? = null, @SerialName("claim_id") val claimId: String? = null)

@Serializable
data class LessonDto(
    @SerialName("lesson_id") val lessonId: String,
    val category: String,
    val title: String,
    @SerialName("read_seconds") val readSeconds: Int = 60,
    val blocks: List<LessonBlockDto> = emptyList(),
)

@Serializable
data class TimelinePhaseDto(
    @SerialName("phase_id") val phaseId: String,
    @SerialName("day_from") val dayFrom: Int,
    @SerialName("day_to") val dayTo: Int? = null,
    val title: String,
    val summary: String,
    @SerialName("claim_ids") val claimIds: List<String> = emptyList(),
)

@Serializable
data class TimelineDto(val version: Int = 1, val disclaimer: String = "", val phases: List<TimelinePhaseDto> = emptyList())
