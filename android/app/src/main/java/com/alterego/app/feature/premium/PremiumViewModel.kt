package com.alterego.app.feature.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.billing.BillingManager
import com.alterego.app.core.billing.EntitlementRepository
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The paywall's state.
 *
 * Products come straight from Play, so the screen can only ever show a price Play gave it.
 * If Play has nothing to sell here, the screen says so rather than pretending.
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val entitlements: EntitlementRepository,
    private val billing: BillingManager,
    private val analytics: Analytics,
) : ViewModel() {

    val products: StateFlow<List<ProductDetails>> = billing.products
    val connected: StateFlow<Boolean> = billing.connected

    val isPlus: StateFlow<Boolean> = entitlements.entitlement
        .map { it.isPlus }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        billing.connect()
        analytics.track(LocalAnalytics.PAYWALL_VIEWED)
    }

    fun purchase(activity: android.app.Activity, productId: String) {
        val details = products.value.firstOrNull { it.productId == productId } ?: return
        billing.launchPurchase(activity, details)
    }
}
