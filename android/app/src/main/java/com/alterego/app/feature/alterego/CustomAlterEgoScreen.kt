package com.alterego.app.feature.alterego

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alterego.app.core.animation.AlterEgoCharacter
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.domain.models.CharacterState

/** A small, deliberate palette. Six choices is enough to feel personal without producing an unreadable app. */
internal data class PresetColor(val label: String, val argb: Long)

internal val PRESET_COLORS: List<PresetColor> = listOf(
    PresetColor("Slate blue", 0xFF3E5C76L),
    PresetColor("Gold", 0xFFC9A227L),
    PresetColor("Midnight", 0xFF0F1B2BL),
    PresetColor("Plum", 0xFF6B4E71L),
    PresetColor("Pine", 0xFF2E7D6BL),
    PresetColor("Bone", 0xFFF2EFE9L),
)

internal val TONES: List<String> = listOf("gentle", "direct", "playful", "reflective", "warm")

/**
 * Build your own companion.
 *
 * The preview sits above the form and updates as the colours change, so the choice is made by
 * looking at the result rather than by imagining it.
 */
@Composable
fun CustomAlterEgoScreen(
    onDone: () -> Unit,
    viewModel: AlterEgoViewModel = hiltViewModel(),
) {
    val colors = LocalPersonaColors.current

    var name by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(TONES.first()) }
    var primary by remember { mutableLongStateOf(PRESET_COLORS[0].argb) }
    var accent by remember { mutableLongStateOf(PRESET_COLORS[1].argb) }
    var background by remember { mutableLongStateOf(PRESET_COLORS[2].argb) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        AlterEgoHeader(title = "Create my own", onBack = onDone)
        Spacer(Modifier.height(20.dp))

        Preview(primary = primary, accent = accent, background = background, name = name, tagline = tagline)

        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tagline,
            onValueChange = { tagline = it },
            label = { Text("Tagline") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Who are they?") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel("Tone")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            TONES.forEach { option ->
                ToneChip(label = option, selected = tone == option, onClick = { tone = option })
            }
        }

        Spacer(Modifier.height(28.dp))
        ColorRow(label = "Primary", selected = primary, onSelect = { primary = it })
        Spacer(Modifier.height(20.dp))
        ColorRow(label = "Accent", selected = accent, onSelect = { accent = it })
        Spacer(Modifier.height(20.dp))
        ColorRow(label = "Background", selected = background, onSelect = { background = it })

        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = "Save this Alter Ego", enabled = name.isNotBlank()) {
            viewModel.saveCustomPersona(
                name = name,
                tagline = tagline,
                description = description,
                tone = tone,
                primaryColor = primary,
                accentColor = accent,
                backgroundColor = background,
            )
            onDone()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Your companion stays on this device.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Preview(primary: Long, accent: Long, background: Long, name: String, tagline: String) {
    val backgroundColor = Color(background)
    val onBackground = if (backgroundColor.previewLuminance() > 0.55f) Color(0xFF17171A) else Color(0xFFF2EFE9)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlterEgoCharacter(
            state = CharacterState.IDLE,
            primary = Color(primary),
            accent = Color(accent),
            size = 140.dp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            name.ifBlank { "Your Alter Ego" },
            style = MaterialTheme.typography.titleLarge,
            color = onBackground,
        )
        if (tagline.isNotBlank()) {
            Text(
                tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

@Composable
private fun ToneChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.accent.copy(alpha = 0.18f) else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.muted.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.accent else colors.muted,
        )
    }
}

@Composable
private fun ColorRow(label: String, selected: Long, onSelect: (Long) -> Unit) {
    val colors = LocalPersonaColors.current
    Column {
        SectionLabel(label)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            PRESET_COLORS.forEach { preset ->
                val isSelected = preset.argb == selected
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(preset.argb))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) colors.accent else colors.muted.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .clickable { onSelect(preset.argb) },
                )
            }
        }
    }
}

private fun Color.previewLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
