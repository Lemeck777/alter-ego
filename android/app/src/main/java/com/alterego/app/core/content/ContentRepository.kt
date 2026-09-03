package com.alterego.app.core.content

import android.content.Context
import com.alterego.app.core.database.ContentDao
import com.alterego.app.core.database.PersonaEntity
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.models.BiologyTimeline
import com.alterego.app.domain.models.EvidenceClaim
import com.alterego.app.domain.models.Intervention
import com.alterego.app.domain.models.Lesson
import com.alterego.app.domain.models.Moment
import com.alterego.app.domain.models.Persona
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of curated content. Boots from the bundled asset, then accepts remote bundles
 * from the backend so scientific claims can be corrected without an app release.
 */
@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ContentDao,
    private val prefs: UserPreferencesRepository,
) {
    private val mutex = Mutex()

    /** Idempotent: loads the bundled asset the first time or whenever the asset version is newer than what is installed. */
    suspend fun ensureLoaded() = mutex.withLock {
        val bundle = readBundledAsset()
        val installed = prefs.snapshot().contentVersion
        if (installed >= bundle.version && dao.momentCount() > 0) return@withLock
        install(bundle)
    }

    suspend fun install(bundle: ContentBundle) = withContext(Dispatchers.IO) {
        dao.replaceAll(
            personas = bundle.personas.map { it.toEntity() },
            moments = bundle.moments.filter { it.safetyLevel == "safe" }.map { it.toEntity() },
            interventions = bundle.interventions.map { it.toEntity() },
            claims = bundle.claims.map { it.toEntity() },
            lessons = bundle.lessons.map { it.toEntity() },
            timeline = bundle.timeline.toEntities(),
        )
        prefs.update { it.copy(contentVersion = bundle.version) }
    }

    suspend fun readBundledAsset(): ContentBundle = withContext(Dispatchers.IO) {
        context.assets.open(ASSET_PATH).bufferedReader().use { ContentJson.json.decodeFromString(ContentBundle.serializer(), it.readText()) }
    }

    fun observePersonas(): Flow<List<Persona>> = dao.observePersonas().map { list -> list.map { it.toDomain() } }
    suspend fun persona(id: String): Persona? = dao.persona(id)?.toDomain()
    suspend fun upsertCustomPersona(persona: Persona) = dao.upsertPersona(
        PersonaEntity(
            id = persona.id, name = persona.name, tagline = persona.tagline, archetype = persona.archetype,
            description = persona.description, defaultTone = persona.defaultTone,
            voiceRulesJson = ContentJson.encodeStrings(persona.voiceRules),
            primaryColor = hex(persona.primaryColor), accentColor = hex(persona.accentColor), backgroundColor = hex(persona.backgroundColor),
            recommendedForJson = ContentJson.encodeStrings(persona.recommendedFor), premium = false, isCustom = true,
        ),
    )
    suspend fun deleteCustomPersona(id: String) = dao.deleteCustomPersona(id)

    suspend fun allMoments(): List<Moment> = dao.allMoments().map { it.toDomain() }
    suspend fun moment(id: String): Moment? = dao.moment(id)?.toDomain()
    suspend fun interventions(): List<Intervention> = dao.interventions().map { it.toDomain() }
    suspend fun claims(): List<EvidenceClaim> = dao.claims().map { it.toDomain() }
    suspend fun claim(id: String): EvidenceClaim? = dao.claim(id)?.toDomain()
    suspend fun lessons(): List<Lesson> = dao.lessons().map { it.toDomain() }
    suspend fun lesson(id: String): Lesson? = dao.lesson(id)?.toDomain()
    suspend fun timeline(): BiologyTimeline {
        val phases = dao.timeline()
        return BiologyTimeline(disclaimer = phases.firstOrNull()?.disclaimer ?: "", phases = phases.map { it.toDomain() })
    }

    private fun hex(color: Long): String = "#" + (color and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

    companion object { const val ASSET_PATH = "content/bundle.json" }
}
