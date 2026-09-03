package com.alterego.app.core.data

import com.alterego.app.core.content.ContentJson
import com.alterego.app.core.database.ChapterEntity
import com.alterego.app.core.database.CommitmentEntity
import com.alterego.app.core.database.CustomContentDao
import com.alterego.app.core.database.JourneyDao
import com.alterego.app.core.database.LifeTimelineEntity
import com.alterego.app.core.database.ResetEventEntity
import com.alterego.app.core.database.UrgeEventEntity
import com.alterego.app.domain.models.Chapter
import com.alterego.app.domain.models.Commitment
import com.alterego.app.domain.models.CommitmentRule
import com.alterego.app.domain.models.Goal
import com.alterego.app.domain.models.ResetContext
import com.alterego.app.domain.models.ResetEvent
import com.alterego.app.domain.models.UrgeEvent
import com.alterego.app.domain.models.UrgeLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

fun CommitmentEntity.toDomain() = Commitment(
    id = id,
    goal = Goal.fromId(goal),
    rule = CommitmentRule.fromId(rule),
    customRule = customRule,
    title = title,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    isPrimary = isPrimary,
    isActive = isActive,
)

fun ChapterEntity.toDomain() = Chapter(
    id = id,
    commitmentId = commitmentId,
    number = number,
    startedAt = Instant.ofEpochMilli(startedAtMillis),
    endedAt = endedAtMillis?.let { Instant.ofEpochMilli(it) },
)

fun ResetEventEntity.toDomain() = ResetEvent(
    id = id,
    commitmentId = commitmentId,
    chapterId = chapterId,
    occurredAt = Instant.ofEpochMilli(occurredAtMillis),
    context = ResetContext.fromId(context),
    note = note,
    hourOfDay = hourOfDay,
)

fun UrgeEventEntity.toDomain() = UrgeEvent(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAtMillis),
    initialLevel = UrgeLevel.fromId(initialLevel),
    finalLevel = finalLevel?.let { UrgeLevel.fromId(it) },
    interventionIds = ContentJson.decodeStrings(interventionIdsJson),
    completed = completed,
    hourOfDay = hourOfDay,
)

/**
 * Owns commitments, chapters, resets and urges.
 *
 * Product rule enforced here: a reset never deletes history. It closes the current chapter and
 * opens the next one, so lifetime committed days only ever grow.
 */
@Singleton
class JourneyRepository @Inject constructor(
    private val dao: JourneyDao,
    private val customDao: CustomContentDao,
    private val clock: AppClock,
) {
    fun observeActiveCommitments(): Flow<List<Commitment>> =
        dao.observeActiveCommitments().map { list -> list.map { it.toDomain() } }

    fun observePrimaryCommitment(): Flow<Commitment?> =
        dao.observePrimaryCommitment().map { it?.toDomain() }

    suspend fun primaryCommitment(): Commitment? = dao.primaryCommitment()?.toDomain()

    suspend fun activeCommitments(): List<Commitment> = dao.activeCommitments().map { it.toDomain() }

    suspend fun activeCount(): Int = dao.activeCount()

    suspend fun commitment(id: Long): Commitment? = dao.commitment(id)?.toDomain()

    /** Creates a commitment and opens Chapter 1 immediately, so the timer starts the moment they commit. */
    suspend fun createCommitment(
        goal: Goal,
        rule: CommitmentRule,
        customRule: String?,
        title: String,
        primary: Boolean,
    ): Commitment {
        val now = clock.now()
        if (primary) dao.clearPrimary()
        val id = dao.insertCommitment(
            CommitmentEntity(
                goal = goal.id,
                rule = rule.id,
                customRule = customRule,
                title = title,
                createdAtMillis = now.toEpochMilli(),
                isPrimary = primary,
                isActive = true,
            ),
        )
        dao.insertChapter(
            ChapterEntity(commitmentId = id, number = 1, startedAtMillis = now.toEpochMilli(), endedAtMillis = null),
        )
        customDao.insertLifeEvent(LifeTimelineEntity(atMillis = now.toEpochMilli(), text = "Started commitment: $title"))
        return checkNotNull(dao.commitment(id)) { "Commitment row missing right after insert" }.toDomain()
    }

    suspend fun setPrimary(id: Long) {
        dao.clearPrimary()
        dao.commitment(id)?.let { dao.updateCommitment(it.copy(isPrimary = true)) }
    }

    suspend fun deactivate(id: Long) {
        val existing = dao.commitment(id)
        dao.deactivate(id)
        existing?.let {
            customDao.insertLifeEvent(
                LifeTimelineEntity(atMillis = clock.now().toEpochMilli(), text = "Paused commitment: ${it.title}"),
            )
        }
    }

    fun observeOpenChapter(commitmentId: Long): Flow<Chapter?> =
        dao.observeOpenChapter(commitmentId).map { it?.toDomain() }

    suspend fun openChapter(commitmentId: Long): Chapter? = dao.openChapter(commitmentId)?.toDomain()

    suspend fun chapters(commitmentId: Long): List<Chapter> = dao.chapters(commitmentId).map { it.toDomain() }

    fun observeChapters(commitmentId: Long): Flow<List<Chapter>> =
        dao.observeChapters(commitmentId).map { list -> list.map { it.toDomain() } }

    /**
     * "I reset." Closes the current chapter, records the optional reflection and opens the next chapter.
     * Nothing is deleted and nothing is scored.
     */
    suspend fun logReset(commitmentId: Long, context: ResetContext?, note: String?): Chapter {
        val now = clock.now()
        val hour = now.atZone(ZoneId.systemDefault()).hour
        val next = dao.resetAndStartNextChapter(
            commitmentId = commitmentId,
            nowMillis = now.toEpochMilli(),
            context = context?.id,
            note = note,
            hourOfDay = hour,
        )
        return next.toDomain()
    }

    suspend fun resets(commitmentId: Long): List<ResetEvent> = dao.resets(commitmentId).map { it.toDomain() }

    fun observeResets(commitmentId: Long): Flow<List<ResetEvent>> =
        dao.observeResets(commitmentId).map { list -> list.map { it.toDomain() } }

    suspend fun allResets(): List<ResetEvent> = dao.allResets().map { it.toDomain() }

    /** The reflection is optional and arrives after the reset row already exists. */
    suspend fun attachReflection(resetId: Long, context: ResetContext?, note: String?) =
        dao.updateResetReflection(resetId, context?.id, note)

    suspend fun startUrge(level: UrgeLevel): Long {
        val now = clock.now()
        return dao.insertUrge(
            UrgeEventEntity(
                startedAtMillis = now.toEpochMilli(),
                initialLevel = level.id,
                finalLevel = null,
                interventionIdsJson = "[]",
                completed = false,
                hourOfDay = now.atZone(ZoneId.systemDefault()).hour,
            ),
        )
    }

    suspend fun updateUrge(id: Long, finalLevel: UrgeLevel?, interventionIds: List<String>, completed: Boolean) {
        dao.urge(id)?.let {
            dao.updateUrge(
                it.copy(
                    finalLevel = finalLevel?.id,
                    interventionIdsJson = ContentJson.encodeStrings(interventionIds),
                    completed = completed,
                ),
            )
        }
    }

    /** Returns completed-to-total urge interventions, used by Journey and by product metrics. */
    suspend fun urgeStats(): Pair<Int, Int> = dao.completedUrges() to dao.totalUrges()

    suspend fun allUrges(): List<UrgeEvent> = dao.allUrges().map { it.toDomain() }

    suspend fun clearHistory() {
        dao.clearResets()
        dao.clearChapters()
        dao.clearUrges()
        dao.clearCommitments()
        customDao.clearLifeTimeline()
    }
}
