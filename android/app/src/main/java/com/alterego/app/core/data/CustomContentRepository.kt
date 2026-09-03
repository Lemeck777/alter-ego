package com.alterego.app.core.data

import com.alterego.app.core.database.CustomContentDao
import com.alterego.app.core.database.DeliveryDao
import com.alterego.app.core.database.FutureMessageEntity
import com.alterego.app.core.database.LifeTimelineEntity
import com.alterego.app.core.database.MomentDeliveryEntity
import com.alterego.app.core.database.PersonalQuoteEntity
import com.alterego.app.core.database.SavedMomentEntity
import com.alterego.app.core.database.ScheduledReminderEntity
import com.alterego.app.domain.models.CharacterState
import com.alterego.app.domain.models.FutureMessage
import com.alterego.app.domain.models.HapticPattern
import com.alterego.app.domain.models.LifeTimelineEntry
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.MomentAction
import com.alterego.app.domain.models.MomentCategory
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.PersonalQuote
import com.alterego.app.domain.models.SavedMoment
import com.alterego.app.domain.models.TimeContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class ScheduledReminder(val id: Long, val label: String, val hour: Int, val minute: Int, val exact: Boolean, val enabled: Boolean)

@Singleton
class CustomContentRepository @Inject constructor(
    private val dao: CustomContentDao,
    private val deliveryDao: DeliveryDao,
    private val clock: AppClock,
) {
    // Personal quotes: "Teach me what to say"
    fun observeQuotes(): Flow<List<PersonalQuote>> = dao.observeQuotes().map { l -> l.map { PersonalQuote(it.id, it.text, Instant.ofEpochMilli(it.createdAtMillis), it.enabled) } }
    suspend fun addQuote(text: String): Long = dao.insertQuote(PersonalQuoteEntity(text = text.trim(), createdAtMillis = clock.now().toEpochMilli(), enabled = true))
    suspend fun setQuoteEnabled(id: Long, enabled: Boolean) = dao.setQuoteEnabled(id, enabled)
    suspend fun deleteQuote(id: Long) = dao.deleteQuote(id)

    /** Personal quotes become synthetic Moments spoken by the current persona. */
    suspend fun personalQuoteMoments(personaId: String): List<Moment> = dao.enabledQuotes().map { q ->
        Moment(
            id = "personal_${q.id}", persona = personaId, goal = "general", category = MomentCategory.ACCOUNTABILITY, tone = "warm",
            intensity = 3, ageBands = emptyList(), timeContext = TimeContext.ANY, trigger = MomentTrigger.RANDOM, lines = listOf(q.text),
            actions = listOf(MomentAction("I remember", "dismiss"), MomentAction("I need help", "urge_mode")), animation = CharacterState.LOOK,
            haptic = HapticPattern.DOUBLE_TAP, evidenceType = "none", source = "personal", premium = false, isPersonal = true,
        )
    }

    // Future Me
    fun observeFutureMessages(): Flow<List<FutureMessage>> = dao.observeFutureMessages().map { l -> l.map { it.toDomain() } }
    suspend fun addFutureMessage(text: String, deliverAt: Instant): Long = dao.insertFutureMessage(FutureMessageEntity(text = text.trim(), createdAtMillis = clock.now().toEpochMilli(), deliverAtMillis = deliverAt.toEpochMilli(), deliveredAtMillis = null))
    suspend fun dueFutureMessage(): FutureMessage? = dao.dueFutureMessage(clock.now().toEpochMilli())?.toDomain()
    suspend fun markFutureDelivered(id: Long) = dao.markFutureDelivered(id, clock.now().toEpochMilli())
    suspend fun deleteFutureMessage(id: Long) = dao.deleteFutureMessage(id)
    private fun FutureMessageEntity.toDomain() = FutureMessage(id, text, Instant.ofEpochMilli(createdAtMillis), Instant.ofEpochMilli(deliverAtMillis), deliveredAtMillis?.let { Instant.ofEpochMilli(it) })

    // Scheduled reminders ("Remind me exactly at 6:00 to pray")
    fun observeReminders(): Flow<List<ScheduledReminder>> = dao.observeReminders().map { l -> l.map { ScheduledReminder(it.id, it.label, it.hour, it.minute, it.exact, it.enabled) } }
    suspend fun enabledReminders(): List<ScheduledReminder> = dao.enabledReminders().map { ScheduledReminder(it.id, it.label, it.hour, it.minute, it.exact, it.enabled) }
    suspend fun reminder(id: Long): ScheduledReminder? = dao.reminder(id)?.let { ScheduledReminder(it.id, it.label, it.hour, it.minute, it.exact, it.enabled) }
    suspend fun upsertReminder(r: ScheduledReminder): Long = dao.upsertReminder(ScheduledReminderEntity(id = r.id, label = r.label, hour = r.hour, minute = r.minute, exact = r.exact, enabled = r.enabled))
    suspend fun deleteReminder(id: Long) = dao.deleteReminder(id)

    // Deliveries and favourites
    suspend fun recordDelivery(momentId: String, trigger: MomentTrigger): Long = deliveryDao.insert(MomentDeliveryEntity(momentId = momentId, deliveredAtMillis = clock.now().toEpochMilli(), trigger = trigger.id, opened = false, reaction = null))
    suspend fun markOpened(deliveryId: Long) = deliveryDao.markOpened(deliveryId)
    suspend fun setReaction(deliveryId: Long, reaction: String) = deliveryDao.setReaction(deliveryId, reaction)
    suspend fun recentMomentIds(withinMillis: Long): Set<String> = deliveryDao.recentMomentIds(clock.now().toEpochMilli() - withinMillis).toSet()
    suspend fun deliveriesToday(): Int = deliveryDao.countSince(clock.now().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    suspend fun deliveryTotals(): Pair<Int, Int> = deliveryDao.total() to deliveryDao.totalOpened()

    fun observeSaved(): Flow<List<SavedMoment>> = deliveryDao.observeSaved().map { l -> l.map { SavedMoment(it.momentId, Instant.ofEpochMilli(it.savedAtMillis)) } }
    suspend fun save(momentId: String) = deliveryDao.save(SavedMomentEntity(momentId, clock.now().toEpochMilli()))
    suspend fun unsave(momentId: String) = deliveryDao.unsave(momentId)
    suspend fun isSaved(momentId: String): Boolean = deliveryDao.isSaved(momentId) > 0

    // Life timeline
    fun observeLifeTimeline(): Flow<List<LifeTimelineEntry>> = dao.observeLifeTimeline().map { l ->
        l.map { e -> val at = Instant.ofEpochMilli(e.atMillis); LifeTimelineEntry(at.atZone(ZoneId.systemDefault()).year, e.text, at) }
    }
    suspend fun addLifeEvent(text: String) = dao.insertLifeEvent(LifeTimelineEntity(atMillis = clock.now().toEpochMilli(), text = text))

    suspend fun clearAll() { dao.clearQuotes(); dao.clearFutureMessages(); deliveryDao.clear(); deliveryDao.clearSaved(); dao.clearAnalytics(); dao.clearLifeTimeline() }
}
