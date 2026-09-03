package com.alterego.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {
    // Commitments
    @Insert suspend fun insertCommitment(item: CommitmentEntity): Long
    @Update suspend fun updateCommitment(item: CommitmentEntity)
    @Query("SELECT * FROM commitments WHERE isActive = 1 ORDER BY isPrimary DESC, createdAtMillis") fun observeActiveCommitments(): Flow<List<CommitmentEntity>>
    @Query("SELECT * FROM commitments WHERE isActive = 1 ORDER BY isPrimary DESC, createdAtMillis") suspend fun activeCommitments(): List<CommitmentEntity>
    @Query("SELECT * FROM commitments WHERE isPrimary = 1 AND isActive = 1 LIMIT 1") suspend fun primaryCommitment(): CommitmentEntity?
    @Query("SELECT * FROM commitments WHERE isPrimary = 1 AND isActive = 1 LIMIT 1") fun observePrimaryCommitment(): Flow<CommitmentEntity?>
    @Query("SELECT * FROM commitments WHERE id = :id") suspend fun commitment(id: Long): CommitmentEntity?
    @Query("UPDATE commitments SET isPrimary = 0") suspend fun clearPrimary()
    @Query("UPDATE commitments SET isActive = 0 WHERE id = :id") suspend fun deactivate(id: Long)
    @Query("SELECT COUNT(*) FROM commitments WHERE isActive = 1") suspend fun activeCount(): Int

    // Chapters
    @Insert suspend fun insertChapter(item: ChapterEntity): Long
    @Update suspend fun updateChapter(item: ChapterEntity)
    @Query("SELECT * FROM chapters WHERE commitmentId = :commitmentId AND endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1") suspend fun openChapter(commitmentId: Long): ChapterEntity?
    @Query("SELECT * FROM chapters WHERE commitmentId = :commitmentId AND endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1") fun observeOpenChapter(commitmentId: Long): Flow<ChapterEntity?>
    @Query("SELECT * FROM chapters WHERE commitmentId = :commitmentId ORDER BY number") suspend fun chapters(commitmentId: Long): List<ChapterEntity>
    @Query("SELECT * FROM chapters WHERE commitmentId = :commitmentId ORDER BY number DESC") fun observeChapters(commitmentId: Long): Flow<List<ChapterEntity>>
    @Query("SELECT COALESCE(MAX(number), 0) FROM chapters WHERE commitmentId = :commitmentId") suspend fun lastChapterNumber(commitmentId: Long): Int

    // Resets
    @Insert suspend fun insertReset(item: ResetEventEntity): Long
    @Query("SELECT * FROM reset_events WHERE commitmentId = :commitmentId ORDER BY occurredAtMillis DESC") suspend fun resets(commitmentId: Long): List<ResetEventEntity>
    @Query("UPDATE reset_events SET context = :context, note = :note WHERE id = :id") suspend fun updateResetReflection(id: Long, context: String?, note: String?)
    @Query("SELECT * FROM reset_events WHERE commitmentId = :commitmentId ORDER BY occurredAtMillis DESC") fun observeResets(commitmentId: Long): Flow<List<ResetEventEntity>>
    @Query("SELECT * FROM reset_events ORDER BY occurredAtMillis DESC") suspend fun allResets(): List<ResetEventEntity>

    // Urges
    @Insert suspend fun insertUrge(item: UrgeEventEntity): Long
    @Update suspend fun updateUrge(item: UrgeEventEntity)
    @Query("SELECT * FROM urge_events WHERE id = :id") suspend fun urge(id: Long): UrgeEventEntity?
    @Query("SELECT * FROM urge_events ORDER BY startedAtMillis DESC") suspend fun allUrges(): List<UrgeEventEntity>
    @Query("SELECT COUNT(*) FROM urge_events WHERE completed = 1") suspend fun completedUrges(): Int
    @Query("SELECT COUNT(*) FROM urge_events") suspend fun totalUrges(): Int

    /** Ends the open chapter (if any), records the reset and opens the next chapter atomically. */
    @Transaction
    suspend fun resetAndStartNextChapter(commitmentId: Long, nowMillis: Long, context: String?, note: String?, hourOfDay: Int): ChapterEntity {
        val open = openChapter(commitmentId)
        val endedId = if (open != null) {
            updateChapter(open.copy(endedAtMillis = nowMillis)); open.id
        } else 0L
        insertReset(ResetEventEntity(commitmentId = commitmentId, chapterId = endedId, occurredAtMillis = nowMillis, context = context, note = note, hourOfDay = hourOfDay))
        val next = ChapterEntity(commitmentId = commitmentId, number = lastChapterNumber(commitmentId) + 1, startedAtMillis = nowMillis, endedAtMillis = null)
        val id = insertChapter(next)
        return next.copy(id = id)
    }

    @Query("DELETE FROM reset_events") suspend fun clearResets()
    @Query("DELETE FROM chapters") suspend fun clearChapters()
    @Query("DELETE FROM commitments") suspend fun clearCommitments()
    @Query("DELETE FROM urge_events") suspend fun clearUrges()
}
