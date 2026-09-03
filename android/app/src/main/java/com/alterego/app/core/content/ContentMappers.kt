package com.alterego.app.core.content

import com.alterego.app.core.database.ClaimEntity
import com.alterego.app.core.database.InterventionEntity
import com.alterego.app.core.database.LessonEntity
import com.alterego.app.core.database.MomentEntity
import com.alterego.app.core.database.PersonaEntity
import com.alterego.app.core.database.TimelinePhaseEntity
import com.alterego.app.domain.models.CharacterState
import com.alterego.app.domain.models.EvidenceClaim
import com.alterego.app.domain.models.EvidenceLevel
import com.alterego.app.domain.models.HapticPattern
import com.alterego.app.domain.models.Intervention
import com.alterego.app.domain.models.Lesson
import com.alterego.app.domain.models.LessonBlock
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.MomentAction
import com.alterego.app.domain.models.MomentCategory
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.Persona
import com.alterego.app.domain.models.TimeContext
import com.alterego.app.domain.models.TimelinePhase
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object ContentJson {
    val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    private val stringList = ListSerializer(String.serializer())
    private val stringMap = MapSerializer(String.serializer(), String.serializer())

    fun encodeStrings(list: List<String>): String = json.encodeToString(stringList, list)
    fun decodeStrings(raw: String): List<String> = runCatching { json.decodeFromString(stringList, raw) }.getOrDefault(emptyList())
    fun encodeMap(map: Map<String, String>): String = json.encodeToString(stringMap, map)
    fun decodeMap(raw: String): Map<String, String> = runCatching { json.decodeFromString(stringMap, raw) }.getOrDefault(emptyMap())
    fun encodeActions(list: List<MomentActionDto>): String = json.encodeToString(ListSerializer(MomentActionDto.serializer()), list)
    fun decodeActions(raw: String): List<MomentActionDto> = runCatching { json.decodeFromString(ListSerializer(MomentActionDto.serializer()), raw) }.getOrDefault(emptyList())
    fun encodeBlocks(list: List<LessonBlockDto>): String = json.encodeToString(ListSerializer(LessonBlockDto.serializer()), list)
    fun decodeBlocks(raw: String): List<LessonBlockDto> = runCatching { json.decodeFromString(ListSerializer(LessonBlockDto.serializer()), raw) }.getOrDefault(emptyList())

    fun parseColor(hex: String): Long = runCatching { 0xFF000000L or hex.removePrefix("#").toLong(16) }.getOrDefault(0xFF3E5C76L)
}

fun PersonaDto.toEntity() = PersonaEntity(
    id = id, name = name, tagline = tagline, archetype = archetype, description = description, defaultTone = defaultTone,
    voiceRulesJson = ContentJson.encodeStrings(voiceRules), primaryColor = palette.primary, accentColor = palette.accent,
    backgroundColor = palette.background, recommendedForJson = ContentJson.encodeStrings(recommendedFor), premium = premium,
)

fun PersonaEntity.toDomain() = Persona(
    id = id, name = name, tagline = tagline, archetype = archetype, description = description, defaultTone = defaultTone,
    voiceRules = ContentJson.decodeStrings(voiceRulesJson), primaryColor = ContentJson.parseColor(primaryColor),
    accentColor = ContentJson.parseColor(accentColor), backgroundColor = ContentJson.parseColor(backgroundColor),
    recommendedFor = ContentJson.decodeStrings(recommendedForJson), premium = premium, isCustom = isCustom,
)

fun MomentDto.toEntity() = MomentEntity(
    id = id, persona = persona, goal = goal, category = category, tone = tone, intensity = intensity,
    ageBandsJson = ContentJson.encodeStrings(ageBands), timeContext = timeContext, trigger = trigger,
    linesJson = ContentJson.encodeStrings(lines), actionsJson = ContentJson.encodeActions(actions), animation = animation,
    haptic = haptic, evidenceType = evidenceType, source = source, premium = premium,
)

fun MomentEntity.toDomain() = Moment(
    id = id, persona = persona, goal = goal, category = MomentCategory.fromId(category), tone = tone, intensity = intensity,
    ageBands = ContentJson.decodeStrings(ageBandsJson), timeContext = TimeContext.fromId(timeContext),
    trigger = MomentTrigger.fromId(trigger), lines = ContentJson.decodeStrings(linesJson),
    actions = ContentJson.decodeActions(actionsJson).map { MomentAction(it.label, it.type) },
    animation = CharacterState.fromId(animation), haptic = HapticPattern.fromId(haptic), evidenceType = evidenceType,
    source = source, premium = premium,
)

fun InterventionDto.toEntity() = InterventionEntity(
    id = id, title = title, category = category, durationSeconds = durationSeconds, defaultEnabled = defaultEnabled,
    linesJson = ContentJson.encodeStrings(lines), personaLinesJson = ContentJson.encodeMap(personaLines),
)

fun InterventionEntity.toDomain() = Intervention(
    id = id, title = title, category = category, durationSeconds = durationSeconds, defaultEnabled = defaultEnabled,
    lines = ContentJson.decodeStrings(linesJson), personaLines = ContentJson.decodeMap(personaLinesJson),
)

fun ClaimDto.toEntity() = ClaimEntity(
    claimId = claimId, claim = claim, topic = topic, ageMin = ageMin, ageMax = ageMax, evidenceLevel = evidenceLevel,
    direction = direction, sourceUrl = sourceUrl, sourceTitle = sourceTitle, publicationYear = publicationYear,
    studyType = studyType, reviewDate = reviewDate, medicalReviewer = medicalReviewer, status = status,
)

fun ClaimEntity.toDomain() = EvidenceClaim(
    claimId = claimId, claim = claim, topic = topic, ageMin = ageMin, ageMax = ageMax,
    evidenceLevel = EvidenceLevel.fromId(evidenceLevel), direction = direction, sourceUrl = sourceUrl,
    sourceTitle = sourceTitle, publicationYear = publicationYear, studyType = studyType, reviewDate = reviewDate,
    medicalReviewer = medicalReviewer, status = status,
)

fun LessonDto.toEntity() = LessonEntity(lessonId = lessonId, category = category, title = title, readSeconds = readSeconds, blocksJson = ContentJson.encodeBlocks(blocks))

fun LessonEntity.toDomain() = Lesson(
    lessonId = lessonId, category = category, title = title, readSeconds = readSeconds,
    blocks = ContentJson.decodeBlocks(blocksJson).map { LessonBlock(it.type, it.text, it.claimId) },
)

fun TimelineDto.toEntities(): List<TimelinePhaseEntity> = phases.mapIndexed { i, p ->
    TimelinePhaseEntity(phaseId = p.phaseId, sortOrder = i, dayFrom = p.dayFrom, dayTo = p.dayTo, title = p.title,
        summary = p.summary, claimIdsJson = ContentJson.encodeStrings(p.claimIds), disclaimer = disclaimer)
}

fun TimelinePhaseEntity.toDomain() = TimelinePhase(phaseId = phaseId, dayFrom = dayFrom, dayTo = dayTo, title = title, summary = summary, claimIds = ContentJson.decodeStrings(claimIdsJson))
