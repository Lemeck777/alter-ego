package com.alterego.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PersonaEntity::class, MomentEntity::class, InterventionEntity::class, ClaimEntity::class, LessonEntity::class,
        TimelinePhaseEntity::class, CommitmentEntity::class, ChapterEntity::class, ResetEventEntity::class,
        UrgeEventEntity::class, MomentDeliveryEntity::class, SavedMomentEntity::class, PersonalQuoteEntity::class,
        FutureMessageEntity::class, ScheduledReminderEntity::class, AnalyticsEventEntity::class, LifeTimelineEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun journeyDao(): JourneyDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun customContentDao(): CustomContentDao

    companion object { const val NAME = "alterego.db" }
}
