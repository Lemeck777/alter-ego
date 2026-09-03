package com.alterego.app.core.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alterego.app.core.network.ContentApi
import com.alterego.app.core.network.PurchaseVerificationRequest
import com.alterego.app.domain.models.Entitlement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.entitlementStore by preferencesDataStore(name = "entitlements")

/**
 * Source of truth for Alter Ego+ access.
 *
 * Deliberately optimistic: a purchase grants access immediately on-device, and the server check
 * only ever confirms. A user who paid never loses their companion because a request failed.
 */
@Singleton
class EntitlementRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ContentApi,
) {
    private object Keys {
        val IS_PLUS = booleanPreferencesKey("is_plus")
        val SOURCE = stringPreferencesKey("source")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val TRIAL_STARTED_AT = longPreferencesKey("trial_started_at")
    }

    val entitlement: Flow<Entitlement> = context.entitlementStore.data.map { p ->
        Entitlement(
            isPlus = p[Keys.IS_PLUS] ?: false,
            source = p[Keys.SOURCE] ?: "free",
            expiresAt = p[Keys.EXPIRES_AT]?.let { Instant.ofEpochMilli(it) },
        )
    }

    suspend fun snapshot(): Entitlement = entitlement.first()

    suspend fun isPlus(): Boolean = snapshot().isPlus

    suspend fun setLocalPlus(isPlus: Boolean, source: String = "play") {
        context.entitlementStore.edit { p ->
            p[Keys.IS_PLUS] = isPlus
            p[Keys.SOURCE] = if (isPlus) source else "free"
        }
    }

    suspend fun verifyWithServer(productId: String, purchaseToken: String, packageName: String) {
        runCatching {
            val response = api.verifyPurchase(PurchaseVerificationRequest(productId, purchaseToken, packageName))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                context.entitlementStore.edit { p ->
                    p[Keys.IS_PLUS] = body.isPlus
                    p[Keys.SOURCE] = body.source
                    body.expiresAt?.let { iso -> p[Keys.EXPIRES_AT] = Instant.parse(iso).toEpochMilli() }
                }
            }
        }
        // A failed verification never revokes access; the next refresh will settle it.
    }

    suspend fun markTrialStarted() {
        context.entitlementStore.edit { it[Keys.TRIAL_STARTED_AT] = System.currentTimeMillis() }
    }

    suspend fun trialStartedAt(): Instant? =
        context.entitlementStore.data.first()[Keys.TRIAL_STARTED_AT]?.let { Instant.ofEpochMilli(it) }

    companion object {
        /** Free tier limits, enforced in one place so the paywall never leaks into feature code. */
        const val FREE_MAX_COMMITMENTS = 1
        const val FREE_MAX_MOMENTS_PER_DAY = 2
        const val FREE_MAX_PERSONAL_QUOTES = 3
        const val TRIAL_DAYS = 7
    }
}
