package com.alterego.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPersonas(items: List<PersonaEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMoments(items: List<MomentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertInterventions(items: List<InterventionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertClaims(items: List<ClaimEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLessons(items: List<LessonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTimeline(items: List<TimelinePhaseEntity>)

    @Query("DELETE FROM personas WHERE isCustom = 0") suspend fun clearBuiltInPersonas()
    @Query("DELETE FROM moments") suspend fun clearMoments()
    @Query("DELETE FROM interventions") suspend fun clearInterventions()
    @Query("DELETE FROM claims") suspend fun clearClaims()
    @Query("DELETE FROM lessons") suspend fun clearLessons()
    @Query("DELETE FROM timeline_phases") suspend fun clearTimeline()

    @Transaction
    suspend fun replaceAll(
        personas: List<PersonaEntity>,
        moments: List<MomentEntity>,
        interventions: List<InterventionEntity>,
        claims: List<ClaimEntity>,
        lessons: List<LessonEntity>,
        timeline: List<TimelinePhaseEntity>,
    ) {
        clearBuiltInPersonas(); clearMoments(); clearInterventions(); clearClaims(); clearLessons(); clearTimeline()
        insertPersonas(personas); insertMoments(moments); insertInterventions(interventions)
        insertClaims(claims); insertLessons(lessons); insertTimeline(timeline)
    }

    @Query("SELECT * FROM personas ORDER BY isCustom, premium, name") fun observePersonas(): Flow<List<PersonaEntity>>
    @Query("SELECT * FROM personas WHERE id = :id") suspend fun persona(id: String): PersonaEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPersona(item: PersonaEntity)
    @Query("DELETE FROM personas WHERE id = :id AND isCustom = 1") suspend fun deleteCustomPersona(id: String)

    @Query("SELECT * FROM moments") suspend fun allMoments(): List<MomentEntity>
    @Query("SELECT * FROM moments WHERE id = :id") suspend fun moment(id: String): MomentEntity?
    @Query("SELECT COUNT(*) FROM moments") suspend fun momentCount(): Int

    @Query("SELECT * FROM interventions") suspend fun interventions(): List<InterventionEntity>
    @Query("SELECT * FROM claims WHERE status = 'active'") suspend fun claims(): List<ClaimEntity>
    @Query("SELECT * FROM claims WHERE claimId = :id") suspend fun claim(id: String): ClaimEntity?
    @Query("SELECT * FROM lessons") suspend fun lessons(): List<LessonEntity>
    @Query("SELECT * FROM lessons WHERE lessonId = :id") suspend fun lesson(id: String): LessonEntity?
    @Query("SELECT * FROM timeline_phases ORDER BY sortOrder") suspend fun timeline(): List<TimelinePhaseEntity>
}

@Dao
interface DeliveryDao {
    @Insert suspend fun insert(item: MomentDeliveryEntity): Long
    @Query("UPDATE moment_deliveries SET opened = 1 WHERE id = :id") suspend fun markOpened(id: Long)
    @Query("UPDATE moment_deliveries SET reaction = :reaction WHERE id = :id") suspend fun setReaction(id: Long, reaction: String)
    @Query("SELECT momentId FROM moment_deliveries WHERE deliveredAtMillis > :sinceMillis") suspend fun recentMomentIds(sinceMillis: Long): List<String>
    @Query("SELECT COUNT(*) FROM moment_deliveries WHERE deliveredAtMillis > :sinceMillis") suspend fun countSince(sinceMillis: Long): Int
    @Query("SELECT * FROM moment_deliveries ORDER BY deliveredAtMillis DESC LIMIT :limit") suspend fun latest(limit: Int): List<MomentDeliveryEntity>
    @Query("SELECT COUNT(*) FROM moment_deliveries") suspend fun total(): Int
    @Query("SELECT COUNT(*) FROM moment_deliveries WHERE opened = 1") suspend fun totalOpened(): Int
    @Query("DELETE FROM moment_deliveries") suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(item: SavedMomentEntity)
    @Query("DELETE FROM saved_moments WHERE momentId = :momentId") suspend fun unsave(momentId: String)
    @Query("SELECT * FROM saved_moments ORDER BY savedAtMillis DESC") fun observeSaved(): Flow<List<SavedMomentEntity>>
    @Query("SELECT COUNT(*) FROM saved_moments WHERE momentId = :momentId") suspend fun isSaved(momentId: String): Int
    @Query("DELETE FROM saved_moments") suspend fun clearSaved()
}

@Dao
interface CustomContentDao {
    @Insert suspend fun insertQuote(item: PersonalQuoteEntity): Long
    @Query("UPDATE personal_quotes SET enabled = :enabled WHERE id = :id") suspend fun setQuoteEnabled(id: Long, enabled: Boolean)
    @Query("DELETE FROM personal_quotes WHERE id = :id") suspend fun deleteQuote(id: Long)
    @Query("SELECT * FROM personal_quotes ORDER BY createdAtMillis DESC") fun observeQuotes(): Flow<List<PersonalQuoteEntity>>
    @Query("SELECT * FROM personal_quotes WHERE enabled = 1") suspend fun enabledQuotes(): List<PersonalQuoteEntity>
    @Query("DELETE FROM personal_quotes") suspend fun clearQuotes()

    @Insert suspend fun insertFutureMessage(item: FutureMessageEntity): Long
    @Query("SELECT * FROM future_messages ORDER BY deliverAtMillis") fun observeFutureMessages(): Flow<List<FutureMessageEntity>>
    @Query("SELECT * FROM future_messages WHERE deliveredAtMillis IS NULL AND deliverAtMillis <= :nowMillis ORDER BY deliverAtMillis LIMIT 1") suspend fun dueFutureMessage(nowMillis: Long): FutureMessageEntity?
    @Query("UPDATE future_messages SET deliveredAtMillis = :at WHERE id = :id") suspend fun markFutureDelivered(id: Long, at: Long)
    @Query("DELETE FROM future_messages WHERE id = :id") suspend fun deleteFutureMessage(id: Long)
    @Query("DELETE FROM future_messages") suspend fun clearFutureMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertReminder(item: ScheduledReminderEntity): Long
    @Query("DELETE FROM scheduled_reminders WHERE id = :id") suspend fun deleteReminder(id: Long)
    @Query("SELECT * FROM scheduled_reminders ORDER BY hour, minute") fun observeReminders(): Flow<List<ScheduledReminderEntity>>
    @Query("SELECT * FROM scheduled_reminders WHERE enabled = 1") suspend fun enabledReminders(): List<ScheduledReminderEntity>
    @Query("SELECT * FROM scheduled_reminders WHERE id = :id") suspend fun reminder(id: Long): ScheduledReminderEntity?

    @Insert suspend fun insertAnalytics(item: AnalyticsEventEntity)
    @Query("SELECT * FROM analytics_events WHERE uploaded = 0 ORDER BY atMillis LIMIT :limit") suspend fun pendingAnalytics(limit: Int): List<AnalyticsEventEntity>
    @Query("UPDATE analytics_events SET uploaded = 1 WHERE id IN (:ids)") suspend fun markUploaded(ids: List<Long>)
    @Query("DELETE FROM analytics_events WHERE uploaded = 1 AND atMillis < :beforeMillis") suspend fun pruneAnalytics(beforeMillis: Long)
    @Query("DELETE FROM analytics_events") suspend fun clearAnalytics()

    @Insert suspend fun insertLifeEvent(item: LifeTimelineEntity)
    @Query("SELECT * FROM life_timeline ORDER BY atMillis") fun observeLifeTimeline(): Flow<List<LifeTimelineEntity>>
    @Query("DELETE FROM life_timeline") suspend fun clearLifeTimeline()
}
