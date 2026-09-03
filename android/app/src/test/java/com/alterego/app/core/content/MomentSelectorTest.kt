package com.alterego.app.core.content

import com.alterego.app.domain.models.CharacterState
import com.alterego.app.domain.models.HapticPattern
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.MomentAction
import com.alterego.app.domain.models.MomentCategory
import com.alterego.app.domain.models.MomentTrigger
import com.alterego.app.domain.models.TimeContext
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class MomentSelectorTest {

    private fun moment(
        id: String,
        persona: String = "sage",
        goal: String = "general",
        category: MomentCategory = MomentCategory.ACCOUNTABILITY,
        ageBands: List<String> = emptyList(),
        timeContext: TimeContext = TimeContext.ANY,
        trigger: MomentTrigger = MomentTrigger.RANDOM,
        premium: Boolean = false,
        intensity: Int = 2,
    ) = Moment(
        id = id, persona = persona, goal = goal, category = category, tone = "reflective", intensity = intensity,
        ageBands = ageBands, timeContext = timeContext, trigger = trigger, lines = listOf("line"),
        actions = listOf(MomentAction("Okay", "dismiss")), animation = CharacterState.LOOK, haptic = HapticPattern.SOFT,
        evidenceType = "none", source = null, premium = premium,
    )

    private fun request(
        personaId: String = "sage",
        goals: Set<String> = setOf("retention"),
        ageBand: String? = null,
        timeContext: TimeContext = TimeContext.AFTERNOON,
        trigger: MomentTrigger = MomentTrigger.RANDOM,
        isPlus: Boolean = false,
        faithEnabled: Boolean = false,
        recent: Set<String> = emptySet(),
        personalQuotes: List<Moment> = emptyList(),
        highRisk: Boolean = false,
    ) = MomentSelector.Request(
        personaId = personaId, goals = goals, ageBand = ageBand, timeContext = timeContext, trigger = trigger,
        isPlus = isPlus, faithEnabled = faithEnabled, recentMomentIds = recent, personalQuotes = personalQuotes,
        isHighRiskWindow = highRisk,
    )

    private val selector = MomentSelector(Random(42))

    @Test
    fun `only the chosen persona speaks`() {
        val library = listOf(moment("a", persona = "sage"), moment("b", persona = "coach"))
        val eligible = selector.eligible(library, request(personaId = "coach"))
        assertThat(eligible.map { it.id }).containsExactly("b")
    }

    @Test
    fun `premium moments are hidden from free users`() {
        val library = listOf(moment("free"), moment("paid", premium = true))
        assertThat(selector.eligible(library, request()).map { it.id }).containsExactly("free")
        assertThat(selector.eligible(library, request(isPlus = true)).map { it.id }).containsExactly("free", "paid")
    }

    @Test
    fun `goal specific moments require that goal`() {
        val library = listOf(moment("general"), moment("faithy", goal = "faith"))
        assertThat(selector.eligible(library, request(goals = setOf("retention"))).map { it.id }).containsExactly("general")
        assertThat(selector.eligible(library, request(goals = setOf("faith"))).map { it.id }).containsExactly("general", "faithy")
    }

    @Test
    fun `age targeted moments only reach that band`() {
        val library = listOf(moment("young", ageBands = listOf("18-24")), moment("everyone"))
        assertThat(selector.eligible(library, request(ageBand = "40-44")).map { it.id }).containsExactly("everyone")
        assertThat(selector.eligible(library, request(ageBand = "18-24")).map { it.id }).containsExactly("young", "everyone")
    }

    @Test
    fun `recently delivered moments are skipped when alternatives exist`() {
        val library = listOf(moment("a"), moment("b"))
        val eligible = selector.eligible(library, request(recent = setOf("a")))
        assertThat(eligible.map { it.id }).containsExactly("b")
    }

    @Test
    fun `recency is relaxed rather than returning nothing`() {
        val library = listOf(moment("a"))
        val picked = selector.select(library, request(recent = setOf("a")))
        assertThat(picked?.id).isEqualTo("a")
    }

    @Test
    fun `urge trigger only selects urge content`() {
        val library = listOf(
            moment("normal"),
            moment("urgent", category = MomentCategory.URGE_MANAGEMENT, trigger = MomentTrigger.URGE),
        )
        val eligible = selector.eligible(library, request(trigger = MomentTrigger.URGE))
        assertThat(eligible.map { it.id }).containsExactly("urgent")
    }

    @Test
    fun `reset and welcome back content never appears in ordinary rotation`() {
        val library = listOf(
            moment("normal"),
            moment("reset", category = MomentCategory.RESET_RECOVERY, trigger = MomentTrigger.RESET),
            moment("welcome", category = MomentCategory.WELCOME_BACK, trigger = MomentTrigger.WELCOME_BACK),
        )
        val eligible = selector.eligible(library, request(trigger = MomentTrigger.RANDOM))
        assertThat(eligible.map { it.id }).containsExactly("normal")
    }

    @Test
    fun `late night content is reserved for late night or a known risk window`() {
        val library = listOf(moment("late", timeContext = TimeContext.LATE_NIGHT))
        assertThat(selector.eligible(library, request(timeContext = TimeContext.AFTERNOON))).isEmpty()
        assertThat(selector.eligible(library, request(timeContext = TimeContext.LATE_NIGHT))).hasSize(1)
        assertThat(selector.eligible(library, request(timeContext = TimeContext.AFTERNOON, highRisk = true))).hasSize(1)
    }

    @Test
    fun `faith content is held back unless the user asked for faith`() {
        val library = listOf(moment("prayer", category = MomentCategory.FAITH, goal = "faith"))
        assertThat(selector.eligible(library, request(goals = setOf("faith"), faithEnabled = false))).isEmpty()
        assertThat(selector.eligible(library, request(goals = setOf("faith"), faithEnabled = true))).hasSize(1)
    }

    @Test
    fun `the users own words are weighted above curated content`() {
        val personal = moment("personal_1").copy(isPersonal = true)
        assertThat(MomentSelector.PERSONAL_QUOTE_WEIGHT).isGreaterThan(selector.weight(moment("a"), request()))
        val picked = selector.select(emptyList(), request(personalQuotes = listOf(personal)))
        assertThat(picked?.id).isEqualTo("personal_1")
    }

    @Test
    fun `accountability outweighs humour but humour still appears`() {
        val accountability = selector.weight(moment("a", category = MomentCategory.ACCOUNTABILITY), request())
        val humour = selector.weight(moment("b", category = MomentCategory.HUMOR), request())
        assertThat(accountability).isGreaterThan(humour)
        assertThat(humour).isGreaterThan(0.0)
    }

    @Test
    fun `an empty library selects nothing rather than crashing`() {
        assertThat(selector.select(emptyList(), request())).isNull()
    }
}
