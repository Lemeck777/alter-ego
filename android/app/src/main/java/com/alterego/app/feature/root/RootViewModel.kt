package com.alterego.app.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.billing.EntitlementRepository
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.data.AppClock
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.security.AppLockManager
import com.alterego.app.domain.models.AppLockMode
import com.alterego.app.domain.models.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class RootState(
    val onboarded: Boolean = false,
    val persona: Persona? = null,
    val isPlus: Boolean = false,
    val locked: Boolean = false,
    val lockMode: AppLockMode = AppLockMode.NONE,
    val anniversaryYear: Int? = null,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val content: ContentRepository,
    private val entitlements: EntitlementRepository,
    private val appLock: AppLockManager,
    private val analytics: Analytics,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(RootState())
    val state: StateFlow<RootState> = _state.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch { content.ensureLoaded() }
        combine(prefs.preferences, entitlements.entitlement) { p, e -> p to e }
            .onEach { (p, e) ->
                val persona = content.persona(p.personaId)
                val needsLock = p.appLock != AppLockMode.NONE && !appLock.isUnlockedThisSession
                _state.value = RootState(
                    onboarded = p.onboarded,
                    persona = persona,
                    isPlus = e.isPlus,
                    locked = needsLock,
                    lockMode = p.appLock,
                    anniversaryYear = anniversaryYear(p.installedAtMillis, p.lastAnniversaryYear),
                )
                _isReady.value = true
            }
            .launchIn(viewModelScope)
    }

    /**
     * Returns the anniversary number when a full year has passed since install and we have not yet
     * marked it. This is what turns the app from a tracker into something that has been with you.
     */
    private fun anniversaryYear(installedAtMillis: Long, lastMarked: Int): Int? {
        if (installedAtMillis == 0L) return null
        val installed = java.time.Instant.ofEpochMilli(installedAtMillis).atZone(ZoneId.systemDefault())
        val years = java.time.temporal.ChronoUnit.YEARS.between(installed, clock.now().atZone(ZoneId.systemDefault())).toInt()
        return if (years >= 1 && years > lastMarked) years else null
    }

    fun acknowledgeAnniversary(year: Int) {
        viewModelScope.launch { prefs.update { it.copy(lastAnniversaryYear = year) } }
    }

    fun unlock() {
        appLock.isUnlockedThisSession = true
        _state.value = _state.value.copy(locked = false)
    }

    fun verifyPin(pin: String): Boolean = appLock.verifyPin(pin).also { if (it) unlock() }

    fun onAppOpened() {
        viewModelScope.launch {
            prefs.update { it.copy(lastOpenedAtMillis = clock.now().toEpochMilli()) }
            analytics.track(LocalAnalytics.APP_OPENED)
        }
    }
}
