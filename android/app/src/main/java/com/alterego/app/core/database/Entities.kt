package com.alterego.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tagline: String,
    val archetype: String,
    val description: String,
    val defaultTone: String,
    val voiceRulesJson: String,
    val primaryColor: String,
    val accentColor: String,
    val backgroundColor: String,
    val recommendedForJson: String,
    val premium: Boolean,
    val isCustom: Boolean = false,
)

@Entity(tableName = "moments", indices = [Index("persona"), Index("category"), Index("trigger")])
data class MomentEntity(
    @PrimaryKey val id: String,
    val persona: String,
    val goal: String,
    val category: String,
    val tone: String,
    val intensity: Int,
    val ageBandsJson: String,
    val timeContext: String,
    val trigger: String,
    val linesJson: String,
    val actionsJson: String,
    val animation: String,
    val haptic: String,
    val evidenceType: String,
    val source: String?,
    val premium: Boolean,
)

@Entity(tableName = "interventions")
data class InterventionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val durationSeconds: Int,
    val defaultEnabled: Boolean,
    val linesJson: String,
    val personaLinesJson: String,
)

@Entity(tableName = "claims")
data class ClaimEntity(
    @PrimaryKey val claimId: String,
    val claim: String,
    val topic: String,
    val ageMin: Int?,
    val ageMax: Int?,
    val evidenceLevel: String,
    val direction: String,
    val sourceUrl: String?,
    val sourceTitle: String,
    val publicationYear: Int?,
    val studyType: String,
    val reviewDate: String,
    val medicalReviewer: String,
    val status: String,
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val lessonId: String,
    val category: String,
    val title: String,
    val readSeconds: Int,
    val blocksJson: String,
)

@Entity(tableName = "timeline_phases")
data class TimelinePhaseEntity(
    @PrimaryKey val phaseId: String,
    val sortOrder: Int,
    val dayFrom: Int,
    val dayTo: Int?,
    val title: String,
    val summary: String,
    val claimIdsJson: String,
    val disclaimer: String,
)

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: String,
    val rule: String,
    val customRule: String?,
    val title: String,
    val createdAtMillis: Long,
    val isPrimary: Boolean,
    val isActive: Boolean,
)

@Entity(tableName = "chapters", indices = [Index("commitmentId")])
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commitmentId: Long,
    val number: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
)

@Entity(tableName = "reset_events", indices = [Index("commitmentId")])
data class ResetEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commitmentId: Long,
    val chapterId: Long,
    val occurredAtMillis: Long,
    val context: String?,
    val note: String?,
    val hourOfDay: Int,
)

@Entity(tableName = "urge_events")
data class UrgeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val initialLevel: String,
    val finalLevel: String?,
    val interventionIdsJson: String,
    val completed: Boolean,
    val hourOfDay: Int,
)

@Entity(tableName = "moment_deliveries", indices = [Index("momentId"), Index("deliveredAtMillis")])
data class MomentDeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val momentId: String,
    val deliveredAtMillis: Long,
    val trigger: String,
    val opened: Boolean,
    val reaction: String?,
)

@Entity(tableName = "saved_moments")
data class SavedMomentEntity(@PrimaryKey val momentId: String, val savedAtMillis: Long)

@Entity(tableName = "personal_quotes")
data class PersonalQuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAtMillis: Long,
    val enabled: Boolean,
)

@Entity(tableName = "future_messages")
data class FutureMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAtMillis: Long,
    val deliverAtMillis: Long,
    val deliveredAtMillis: Long?,
)

@Entity(tableName = "scheduled_reminders")
data class ScheduledReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val exact: Boolean,
    val enabled: Boolean,
    val daysMask: Int = 127,
)

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val propsJson: String,
    val atMillis: Long,
    val uploaded: Boolean = false,
)

@Entity(tableName = "life_timeline")
data class LifeTimelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMillis: Long,
    val text: String,
)
