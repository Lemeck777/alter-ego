package com.alterego.app.core.content

import com.alterego.app.domain.models.EvidenceLevel
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

/**
 * Parses the bundle that actually ships in assets. This catches a content change that the app
 * could not read, which would otherwise only show up as an empty app on a real device.
 */
class ContentBundleTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    private val bundle: ContentBundle by lazy {
        val file = File("src/main/assets/content/bundle.json")
        assertThat(file.exists()).isTrue()
        json.decodeFromString(ContentBundle.serializer(), file.readText())
    }

    @Test
    fun `the shipped bundle parses`() {
        assertThat(bundle.version).isGreaterThan(0L)
        assertThat(bundle.moments).isNotEmpty()
        assertThat(bundle.personas).isNotEmpty()
        assertThat(bundle.claims).isNotEmpty()
        assertThat(bundle.lessons).isNotEmpty()
        assertThat(bundle.timeline.phases).isNotEmpty()
        assertThat(bundle.interventions).isNotEmpty()
    }

    @Test
    fun `every moment maps to a domain object without losing its lines`() {
        val mapped = bundle.moments.map { it.toEntity().toDomain() }
        assertThat(mapped).hasSize(bundle.moments.size)
        mapped.forEach { moment ->
            assertThat(moment.lines).isNotEmpty()
            assertThat(moment.lines.size).isAtMost(3)
            assertThat(moment.actions).isNotEmpty()
        }
    }

    @Test
    fun `moment ids are unique across every persona`() {
        val ids = bundle.moments.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `every health moment cites a claim that exists`() {
        val claimIds = bundle.claims.map { it.claimId }.toSet()
        val health = bundle.moments.filter { it.evidenceType == "health" }
        assertThat(health).isNotEmpty()
        health.forEach { moment ->
            assertThat(claimIds).contains(moment.source)
        }
    }

    @Test
    fun `no moment claims to measure the users body`() {
        val bannedNumber = Regex("""\d+\s*%|\d{3,}\s*(million)?\s*sperm|sperm count\s*[:=]?\s*\d""", RegexOption.IGNORE_CASE)
        bundle.moments.forEach { moment ->
            val text = moment.lines.joinToString(" ")
            assertThat(bannedNumber.containsMatchIn(text)).isFalse()
        }
    }

    @Test
    fun `every claim carries a source unless it is explicitly tradition`() {
        bundle.claims.forEach { claim ->
            val level = EvidenceLevel.fromId(claim.evidenceLevel)
            if (level != EvidenceLevel.TRADITION) {
                assertThat(claim.sourceUrl).isNotEmpty()
            }
            assertThat(claim.reviewDate).matches("""\d{4}-\d{2}-\d{2}""")
        }
    }

    @Test
    fun `every lesson claim reference resolves`() {
        val claimIds = bundle.claims.map { it.claimId }.toSet()
        bundle.lessons.forEach { lesson ->
            lesson.blocks.filter { it.type == "claim" }.forEach { block ->
                assertThat(claimIds).contains(block.claimId)
            }
        }
    }

    @Test
    fun `the biology timeline covers every day from one onwards`() {
        val phases = bundle.timeline.phases.sortedBy { it.dayFrom }
        assertThat(phases.first().dayFrom).isAtMost(1)
        assertThat(phases.last().dayTo).isNull()
        phases.zipWithNext().forEach { (a, b) ->
            assertThat(b.dayFrom).isEqualTo((a.dayTo ?: b.dayFrom) + 1)
        }
    }

    @Test
    fun `the timeline states plainly that it cannot measure the user`() {
        assertThat(bundle.timeline.disclaimer.lowercase()).contains("semen analysis")
    }

    @Test
    fun `each of the five shipped personas has a full library`() {
        val byPersona = bundle.moments.groupBy { it.persona }
        listOf("sage", "coach", "grace", "sunny", "brother").forEach { persona ->
            assertThat(byPersona[persona]).isNotNull()
            assertThat(byPersona.getValue(persona).size).isAtLeast(50)
        }
    }
}
