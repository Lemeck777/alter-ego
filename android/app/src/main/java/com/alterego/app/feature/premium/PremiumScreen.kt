package com.alterego.app.feature.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.billing.BillingManager
import com.alterego.app.core.billing.EntitlementRepository
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel
import com.android.billingclient.api.ProductDetails

/**
 * The paywall.
 *
 * Deliberately calm and complete: the free tier is listed in full first, including the entire
 * science library, so nobody upgrades because they were made to feel the app was broken without it.
 */
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val colors = LocalPersonaColors.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isPlus by viewModel.isPlus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "BACK",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp, horizontal = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("Alter Ego+", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Text(
            if (isPlus) {
                "You're on Alter Ego+. Thank you."
            } else {
                "The app works without this. Plus gives it more room."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(28.dp))
        FeatureList(
            heading = "Free, always",
            items = listOf(
                "1 Alter Ego",
                "1 commitment",
                "${EntitlementRepository.FREE_MAX_MOMENTS_PER_DAY} Moments a day",
                "The urge button",
                "Basic Journey",
                "The whole science library",
                "Quiet hours",
            ),
        )

        Spacer(Modifier.height(24.dp))
        FeatureList(
            heading = "Alter Ego+ adds",
            highlighted = true,
            items = listOf(
                "Unlimited commitments and personas",
                "Smart timing",
                "Custom Alter Ego",
                "Advanced insights",
                "Long-term analytics",
                "Premium character packs",
                "Custom interventions",
                "Cloud backup",
                "More Moments a day",
            ),
        )

        if (!isPlus) {
            Spacer(Modifier.height(28.dp))
            SectionLabel("Plans")
            if (products.isEmpty()) {
                Text(
                    "Subscriptions aren't available on this device yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    products.sortedBy { it.productId }.forEach { product ->
                        PlanRow(
                            product = product,
                            enabled = activity != null,
                            onClick = { activity?.let { viewModel.purchase(it, product.productId) } },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "New subscriptions start with a ${EntitlementRepository.TRIAL_DAYS}-day free trial " +
                        "where Google Play offers one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Billing is handled by Google Play. You can cancel any time in your Google Play " +
                "subscriptions, and you keep Plus until the period you paid for ends.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun FeatureList(heading: String, items: List<String>, highlighted: Boolean = false) {
    val colors = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (highlighted) colors.accent.copy(alpha = 0.12f) else colors.surface)
            .border(
                width = if (highlighted) 1.5.dp else 1.dp,
                color = if (highlighted) colors.accent else colors.muted.copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        SectionLabel(heading)
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text("—", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                Text(item, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
            }
        }
    }
}

@Composable
private fun PlanRow(product: ProductDetails, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.muted.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(product.planLabel(), style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        val price = product.recurringPrice()
        if (price != null) {
            Text(price, style = MaterialTheme.typography.bodyLarge, color = colors.accent, modifier = Modifier.padding(top = 4.dp))
        }
        if (product.hasFreeTrial()) {
            Text(
                "Starts with a free trial",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(text = "Continue", enabled = enabled, onClick = onClick)
    }
}

private fun ProductDetails.planLabel(): String = when (productId) {
    BillingManager.PRODUCT_MONTHLY -> "Monthly"
    BillingManager.PRODUCT_YEARLY -> "Yearly"
    else -> name.ifBlank { productId }
}

/** The first phase Play actually charges for. A zero-price phase is the trial, not the price. */
private fun ProductDetails.recurringPrice(): String? {
    val phases = subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList.orEmpty()
    return phases.firstOrNull { it.priceAmountMicros > 0L }?.formattedPrice
        ?: phases.lastOrNull()?.formattedPrice
}

private fun ProductDetails.hasFreeTrial(): Boolean =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList.orEmpty()
        .any { it.priceAmountMicros == 0L }

/** Compose hands us a themed ContextWrapper, and Play billing needs the real Activity. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
