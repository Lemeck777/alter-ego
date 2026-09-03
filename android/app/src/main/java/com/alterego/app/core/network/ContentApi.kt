package com.alterego.app.core.network

import com.alterego.app.core.content.ContentBundle
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class EntitlementResponse(val isPlus: Boolean, val source: String, val expiresAt: String? = null)

@Serializable
data class PurchaseVerificationRequest(val productId: String, val purchaseToken: String, val packageName: String)

@Serializable
data class AnalyticsEventPayload(val name: String, val props: Map<String, String>, val at: Long)

@Serializable
data class AnalyticsBatchRequest(val installId: String, val events: List<AnalyticsEventPayload>)

@Serializable
data class AckResponse(val ok: Boolean)

interface ContentApi {
    /** Returns 304 when the caller already has the current bundle. */
    @GET("v1/content/bundle")
    suspend fun bundle(@Query("since") since: Long): Response<ContentBundle>

    @POST("v1/billing/verify")
    suspend fun verifyPurchase(@Body body: PurchaseVerificationRequest): Response<EntitlementResponse>

    @GET("v1/entitlements/me")
    suspend fun entitlement(): Response<EntitlementResponse>

    @POST("v1/analytics/batch")
    suspend fun uploadAnalytics(@Body body: AnalyticsBatchRequest): Response<AckResponse>
}
