package com.alterego.app.core.content

import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.MomentCategory
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.TimeContext
import kotlin.random.Random

/**
 * Pure, testable selection engine. Picks the next Moment from the library given who the user is,
 * what they asked for, what time it is and what they have already seen.
 *
 * Design rules (see docs/ARCHITECTURE.md):
 *  - Not every Moment is about retention. Category weights keep the mix human.
 *  - Personal quotes ("Teach me what to say") are weighted highest.
 *  - Never repeats a Moment delivered in the recency window when alternatives exist.
 *  - Premium Moments are excluded for free users; the library is still large enough.
 */
class MomentSelector(private val random: Random = Random.Default) {

    data class Request(
        val personaId: String,
        val goals: Set<String>,
        val ageBand: String?,
        val timeContext: TimeContext,
        val trigger: MomentTrigger,
        val isPlus: Boolean,
        val faithEnabled: Boolean,
        val recentMomentIds: Set<String>,
        val personalQuotes: List<Moment> = emptyList(),
        val maxIntensity: Int = 5,
        val isHighRiskWindow: Boolean = false,
    )

    fun select(library: List<Moment>, request: Request): Moment? {
        val candidates = eligible(library, request)
        val pool = candidates.ifEmpty { eligible(library, request.copy(recentMomentIds = emptySet())) }
        if (pool.isEmpty() && request.personalQuotes.isEmpty()) return null
        val weighted = pool.map { it to weight(it, request) } + request.personalQuotes.map { it to PERSONAL_QUOTE_WEIGHT }
        return pickWeighted(weighted)
    }

    fun eligible(library: List<Moment>, request: Request): List<Moment> = library.filter { m ->
        (m.persona == request.personaId || m.persona == "any") &&
            (!m.premium || request.isPlus) &&
            m.intensity <= request.maxIntensity &&
            m.id !in request.recentMomentIds &&
            goalMatches(m, request) &&
            ageMatches(m, request.ageBand) &&
            timeMatches(m, request) &&
            triggerMatches(m, request) &&
            (m.category != MomentCategory.FAITH || request.faithEnabled || m.persona == "grace")
    }

    private fun goalMatches(m: Moment, r: Request): Boolean = m.goal == "general" || m.goal in r.goals

    private fun ageMatches(m: Moment, band: String?): Boolean = m.ageBands.isEmpty() || (band != null && band in m.ageBands)

    private fun timeMatches(m: Moment, r: Request): Boolean = when (m.timeContext) {
        TimeContext.ANY -> true
        TimeContext.LATE_NIGHT -> r.timeContext == TimeContext.LATE_NIGHT || r.isHighRiskWindow
        else -> m.timeContext == r.timeContext
    }

    private fun triggerMatches(m: Moment, r: Request): Boolean = when (r.trigger) {
        MomentTrigger.URGE -> m.trigger == MomentTrigger.URGE || m.category == MomentCategory.URGE_MANAGEMENT
        MomentTrigger.RESET -> m.trigger == MomentTrigger.RESET || m.category == MomentCategory.RESET_RECOVERY
        MomentTrigger.WELCOME_BACK -> m.trigger == MomentTrigger.WELCOME_BACK || m.category == MomentCategory.WELCOME_BACK
        MomentTrigger.HIGH_RISK_WINDOW -> m.trigger == MomentTrigger.HIGH_RISK_WINDOW || m.category == MomentCategory.LATE_NIGHT || m.category == MomentCategory.ACCOUNTABILITY
        MomentTrigger.ANNIVERSARY -> m.trigger == MomentTrigger.ANNIVERSARY || m.category == MomentCategory.ANNIVERSARY || m.category == MomentCategory.PERSPECTIVE
        MomentTrigger.SCHEDULED, MomentTrigger.ACCOUNTABILITY, MomentTrigger.SMART, MomentTrigger.RANDOM ->
            m.trigger !in setOf(MomentTrigger.URGE, MomentTrigger.RESET, MomentTrigger.WELCOME_BACK, MomentTrigger.ANNIVERSARY)
    }

    /** Category weights approximate the target mix: 30% accountability, 20% humor, 15% perspective, 10% health, 10% faith, 10% wellbeing, 5% random. */
    fun weight(m: Moment, r: Request): Double {
        val base = when (m.category) {
            MomentCategory.ACCOUNTABILITY -> 30.0
            MomentCategory.HUMOR -> 20.0
            MomentCategory.PERSPECTIVE -> 15.0
            MomentCategory.HEALTH -> 10.0
            MomentCategory.FAITH -> if (r.faithEnabled) 10.0 else 2.0
            MomentCategory.WELLBEING -> 10.0
            MomentCategory.RANDOM -> 5.0
            MomentCategory.LATE_NIGHT -> if (r.isHighRiskWindow) 40.0 else 8.0
            MomentCategory.URGE_MANAGEMENT, MomentCategory.RESET_RECOVERY, MomentCategory.WELCOME_BACK, MomentCategory.ANNIVERSARY -> 25.0
            MomentCategory.MORNING, MomentCategory.EVENING -> 12.0
        }
        val goalBoost = if (m.goal != "general" && m.goal in r.goals) 1.4 else 1.0
        val timeBoost = if (m.timeContext != TimeContext.ANY) 1.3 else 1.0
        return base * goalBoost * timeBoost
    }

    private fun pickWeighted(items: List<Pair<Moment, Double>>): Moment? {
        if (items.isEmpty()) return null
        val total = items.sumOf { it.second }
        var roll = random.nextDouble() * total
        for ((moment, w) in items) { roll -= w; if (roll <= 0) return moment }
        return items.last().first
    }

    companion object { const val PERSONAL_QUOTE_WEIGHT = 45.0 }
}
