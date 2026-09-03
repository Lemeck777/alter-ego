package com.alterego.app.feature.alterego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alterego.app.core.billing.EntitlementRepository
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.domain.models.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlterEgoState(
    val personas: List<Persona> = emptyList(),
    val selectedId: String = DEFAULT_PERSONA_ID,
    val isPlus: Boolean = false,
) {
    val selected: Persona? get() = personas.firstOrNull { it.id == selectedId }
    val customPersonas: List<Persona> get() = personas.filter { it.isCustom }
}

const val DEFAULT_PERSONA_ID = "sage"

/**
 * Choosing a companion changes the colour of the whole app, so this is the single place that
 * writes personaId. Premium personas are shown, never hidden: the user should be able to see
 * who they could walk with before deciding whether that is worth paying for.
 */
@HiltViewModel
class AlterEgoViewModel @Inject constructor(
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val entitlements: EntitlementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AlterEgoState())
    val state: StateFlow<AlterEgoState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            combine(
                content.observePersonas(),
                prefs.preferences,
                entitlements.entitlement,
            ) { personas, preferences, entitlement ->
                AlterEgoState(
                    personas = personas,
                    selectedId = preferences.personaId,
                    isPlus = entitlement.isPlus,
                )
            }.collect { _state.value = it }
        }
    }

    fun select(id: String) {
        viewModelScope.launch { prefs.update { it.copy(personaId = id) } }
    }

    /** Builds and stores a user-authored companion, then makes it the active one. */
    fun saveCustomPersona(
        name: String,
        tagline: String,
        description: String,
        tone: String,
        primaryColor: Long,
        accentColor: Long,
        backgroundColor: Long,
    ) {
        val id = "custom_${System.currentTimeMillis()}"
        val persona = Persona(
            id = id,
            name = name.trim().ifBlank { "My Alter Ego" },
            tagline = tagline.trim(),
            archetype = "Your own",
            description = description.trim(),
            defaultTone = tone,
            voiceRules = emptyList(),
            primaryColor = primaryColor,
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            recommendedFor = emptyList(),
            premium = false,
            isCustom = true,
        )
        viewModelScope.launch {
            content.upsertCustomPersona(persona)
            prefs.update { it.copy(personaId = id) }
        }
    }

    fun deleteCustom(id: String) {
        viewModelScope.launch {
            content.deleteCustomPersona(id)
            if (prefs.snapshot().personaId == id) prefs.update { it.copy(personaId = DEFAULT_PERSONA_ID) }
        }
    }
}
