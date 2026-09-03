package com.alterego.app.core.analytics

import com.alterego.app.core.database.AnalyticsEventEntity
import com.alterego.app.core.database.CustomContentDao
import com.alterego.app.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy-first analytics. Events are aggregate product signals (retention, open rate, urge completion),
 * never content of notes or quotes. Stored locally; uploaded in batches only if the user keeps analytics on.
 */
interface Analytics {
    fun track(name: String, props: Map<String, String> = emptyMap())
}

@Singleton
class LocalAnalytics @Inject constructor(
    private val dao: CustomContentDao,
    private val prefs: UserPreferencesRepository,
) : Analytics {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    override fun track(name: String, props: Map<String, String>) {
        scope.launch {
            if (!prefs.snapshot().analyticsEnabled) return@launch
            val safeProps = props.filterKeys { it !in FORBIDDEN_KEYS }
            dao.insertAnalytics(AnalyticsEventEntity(name = name, propsJson = json.encodeToString(mapSerializer, safeProps), atMillis = System.currentTimeMillis()))
        }
    }

    companion object {
        /** Defensive: these must never be attached to an analytics event. */
        val FORBIDDEN_KEYS = setOf("note", "quote", "text", "message", "custom_rule")

        // Event names used across the app (kept here so the metrics doc stays in sync).
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val MOMENT_DELIVERED = "moment_delivered"
        const val MOMENT_OPENED = "moment_opened"
        const val URGE_STARTED = "urge_started"
        const val URGE_COMPLETED = "urge_completed"
        const val CHAPTER_RESET = "chapter_reset"
        const val CHAPTER_STARTED = "chapter_started"
        const val NOTIFICATIONS_DENIED = "notifications_denied"
        const val PAYWALL_VIEWED = "paywall_viewed"
        const val TRIAL_STARTED = "trial_started"
        const val PERSONA_CHANGED = "persona_changed"
        const val APP_OPENED = "app_opened"
    }
}
