package com.alterego.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alterego.app.domain.models.AgeBand
import com.alterego.app.domain.models.AppLockMode
import com.alterego.app.domain.models.NotificationPrivacy
import com.alterego.app.domain.models.QuietHours
import com.alterego.app.domain.models.ReminderIntensity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/** Everything the user has told us about themselves. Local-first; nothing here leaves the device unless backup is enabled. */
data class UserPreferences(
    val onboarded: Boolean = false,
    val personaId: String = "sage",
    val goals: Set<String> = emptySet(),
    val ageBand: AgeBand? = null,
    val intensity: ReminderIntensity = ReminderIntensity.BALANCED,
    val customMomentsPerDay: Int = 3,
    val quietHours: QuietHours = QuietHours.DEFAULT,
    val notificationPrivacy: NotificationPrivacy = NotificationPrivacy.PRIVATE,
    val appLock: AppLockMode = AppLockMode.NONE,
    val enabledInterventions: Set<String> = emptySet(),
    val installedAtMillis: Long = 0L,
    val contentVersion: Long = 0L,
    val lastPlannedDay: String = "",
    val lastOpenedAtMillis: Long = 0L,
    val displayName: String = "",
    val faithEnabled: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val smartSensingEnabled: Boolean = false,
    val lastAnniversaryYear: Int = 0,
    val userId: String = "",
    val authToken: String = "",
    val cloudBackupEnabled: Boolean = false,
)

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PERSONA = stringPreferencesKey("persona_id")
        val GOALS = stringSetPreferencesKey("goals")
        val AGE_BAND = stringPreferencesKey("age_band")
        val INTENSITY = stringPreferencesKey("intensity")
        val CUSTOM_PER_DAY = intPreferencesKey("custom_per_day")
        val QUIET_START = intPreferencesKey("quiet_start_min")
        val QUIET_END = intPreferencesKey("quiet_end_min")
        val PRIVACY = stringPreferencesKey("notification_privacy")
        val APP_LOCK = stringPreferencesKey("app_lock")
        val INTERVENTIONS = stringSetPreferencesKey("interventions")
        val INSTALLED_AT = longPreferencesKey("installed_at")
        val CONTENT_VERSION = longPreferencesKey("content_version")
        val LAST_PLANNED_DAY = stringPreferencesKey("last_planned_day")
        val LAST_OPENED_AT = longPreferencesKey("last_opened_at")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val FAITH = booleanPreferencesKey("faith_enabled")
        val ANALYTICS = booleanPreferencesKey("analytics_enabled")
        val SMART_SENSING = booleanPreferencesKey("smart_sensing")
        val LAST_ANNIVERSARY_YEAR = intPreferencesKey("last_anniversary_year")
        val USER_ID = stringPreferencesKey("user_id")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val CLOUD_BACKUP = booleanPreferencesKey("cloud_backup")
    }

    val preferences: Flow<UserPreferences> = context.userPrefsStore.data.map { p ->
        UserPreferences(
            onboarded = p[Keys.ONBOARDED] ?: false,
            personaId = p[Keys.PERSONA] ?: "sage",
            goals = p[Keys.GOALS] ?: emptySet(),
            ageBand = AgeBand.fromId(p[Keys.AGE_BAND]),
            intensity = ReminderIntensity.fromId(p[Keys.INTENSITY]),
            customMomentsPerDay = p[Keys.CUSTOM_PER_DAY] ?: 3,
            quietHours = QuietHours(
                LocalTime.ofSecondOfDay(((p[Keys.QUIET_START] ?: (22 * 60)) * 60).toLong()),
                LocalTime.ofSecondOfDay(((p[Keys.QUIET_END] ?: (7 * 60)) * 60).toLong()),
            ),
            notificationPrivacy = NotificationPrivacy.fromId(p[Keys.PRIVACY]),
            appLock = AppLockMode.fromId(p[Keys.APP_LOCK]),
            enabledInterventions = p[Keys.INTERVENTIONS] ?: emptySet(),
            installedAtMillis = p[Keys.INSTALLED_AT] ?: 0L,
            contentVersion = p[Keys.CONTENT_VERSION] ?: 0L,
            lastPlannedDay = p[Keys.LAST_PLANNED_DAY] ?: "",
            lastOpenedAtMillis = p[Keys.LAST_OPENED_AT] ?: 0L,
            displayName = p[Keys.DISPLAY_NAME] ?: "",
            faithEnabled = p[Keys.FAITH] ?: false,
            analyticsEnabled = p[Keys.ANALYTICS] ?: true,
            smartSensingEnabled = p[Keys.SMART_SENSING] ?: false,
            lastAnniversaryYear = p[Keys.LAST_ANNIVERSARY_YEAR] ?: 0,
            userId = p[Keys.USER_ID] ?: "",
            authToken = p[Keys.AUTH_TOKEN] ?: "",
            cloudBackupEnabled = p[Keys.CLOUD_BACKUP] ?: false,
        )
    }

    suspend fun snapshot(): UserPreferences = preferences.first()

    suspend fun update(block: suspend (UserPreferences) -> UserPreferences) {
        val next = block(snapshot())
        context.userPrefsStore.edit { p ->
            p[Keys.ONBOARDED] = next.onboarded
            p[Keys.PERSONA] = next.personaId
            p[Keys.GOALS] = next.goals
            next.ageBand?.let { p[Keys.AGE_BAND] = it.id } ?: p.remove(Keys.AGE_BAND)
            p[Keys.INTENSITY] = next.intensity.id
            p[Keys.CUSTOM_PER_DAY] = next.customMomentsPerDay
            p[Keys.QUIET_START] = next.quietHours.start.toSecondOfDay() / 60
            p[Keys.QUIET_END] = next.quietHours.end.toSecondOfDay() / 60
            p[Keys.PRIVACY] = next.notificationPrivacy.id
            p[Keys.APP_LOCK] = next.appLock.id
            p[Keys.INTERVENTIONS] = next.enabledInterventions
            p[Keys.INSTALLED_AT] = next.installedAtMillis
            p[Keys.CONTENT_VERSION] = next.contentVersion
            p[Keys.LAST_PLANNED_DAY] = next.lastPlannedDay
            p[Keys.LAST_OPENED_AT] = next.lastOpenedAtMillis
            p[Keys.DISPLAY_NAME] = next.displayName
            p[Keys.FAITH] = next.faithEnabled
            p[Keys.ANALYTICS] = next.analyticsEnabled
            p[Keys.SMART_SENSING] = next.smartSensingEnabled
            p[Keys.LAST_ANNIVERSARY_YEAR] = next.lastAnniversaryYear
            p[Keys.USER_ID] = next.userId
            p[Keys.AUTH_TOKEN] = next.authToken
            p[Keys.CLOUD_BACKUP] = next.cloudBackupEnabled
        }
    }

    suspend fun clearAll() { context.userPrefsStore.edit { it.clear() } }
}
