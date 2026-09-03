package com.alterego.app.feature.alterego

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.animation.AlterEgoCharacter
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.domain.models.CharacterState
import com.alterego.app.domain.models.Persona

/**
 * Who's coming with you.
 *
 * Every companion is drawn in their own colours here rather than the app's current theme, so the
 * choice is visible before it is made. Premium companions are shown with a lock instead of being
 * hidden, because a paywall you cannot see is a paywall you cannot trust.
 */
@Composable
fun AlterEgoPickerScreen(
    onBack: () -> Unit,
    onCreateCustom: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: AlterEgoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        AlterEgoHeader(title = "Your Alter Ego", onBack = onBack)
        Text(
            "This is the voice you'll hear. You can swap any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        val free = state.personas.filter { !it.premium && !it.isCustom }
        val premium = state.personas.filter { it.premium }
        val custom = state.customPersonas

        if (free.isNotEmpty()) {
            SectionLabel("Included")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                free.forEach { persona ->
                    PersonaCard(
                        persona = persona,
                        selected = persona.id == state.selectedId,
                        locked = false,
                        onClick = { viewModel.select(persona.id) },
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (custom.isNotEmpty()) {
            SectionLabel("Yours")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                custom.forEach { persona ->
                    PersonaCard(
                        persona = persona,
                        selected = persona.id == state.selectedId,
                        locked = false,
                        onClick = { viewModel.select(persona.id) },
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (premium.isNotEmpty()) {
            SectionLabel("Alter Ego+")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                premium.forEach { persona ->
                    val locked = !state.isPlus
                    PersonaCard(
                        persona = persona,
                        selected = persona.id == state.selectedId,
                        locked = locked,
                        onClick = { if (locked) onUpgrade() else viewModel.select(persona.id) },
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        CreateOwnRow(
            locked = !state.isPlus,
            onClick = { if (state.isPlus) onCreateCustom() else onUpgrade() },
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
internal fun AlterEgoHeader(title: String, onBack: (() -> Unit)?) {
    val colors = LocalPersonaColors.current
    Column {
        Spacer(Modifier.height(16.dp))
        if (onBack != null) {
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
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
    }
}

@Composable
private fun PersonaCard(persona: Persona, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.muted.copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PersonaPortrait(persona = persona, size = 76.dp)

        Column(Modifier.weight(1f)) {
            Text(persona.name, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            Text(
                persona.archetype.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (persona.tagline.isNotBlank()) {
                Text(
                    persona.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        when {
            locked -> Icon(Icons.Outlined.Lock, contentDescription = "Alter Ego+", tint = colors.muted)
            selected -> Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = colors.accent)
            else -> Unit
        }
    }
}

/** The companion drawn in their own palette, not the app's current one. */
@Composable
internal fun PersonaPortrait(persona: Persona, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(persona.backgroundColor)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AlterEgoCharacter(
            state = CharacterState.IDLE,
            primary = Color(persona.primaryColor),
            accent = Color(persona.accentColor),
            size = size,
        )
    }
}

@Composable
private fun CreateOwnRow(locked: Boolean, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Create my own Alter Ego", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            Text(
                if (locked) "Part of Alter Ego+" else "Your name, your words, your colours.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (locked) Icon(Icons.Outlined.Lock, contentDescription = "Alter Ego+", tint = colors.muted)
    }
}
