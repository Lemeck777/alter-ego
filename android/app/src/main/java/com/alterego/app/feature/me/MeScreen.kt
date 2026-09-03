package com.alterego.app.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.animation.AlterEgoCharacter
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.domain.models.CharacterState
import com.alterego.app.feature.root.Destinations

/**
 * The "Me" tab. Not a settings dump: it opens with the companion, says how long you have been
 * walking together in plain language, and only then offers the doors.
 */
@Composable
fun MeScreen(onNavigate: (String) -> Unit, viewModel: MeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(28.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onNavigate(Destinations.ALTER_EGO_PICKER) }
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AlterEgoCharacter(
                state = CharacterState.SMILE,
                primary = colors.primary,
                accent = colors.accent,
                size = 150.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                state.personaName.ifBlank { "Your Alter Ego" },
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            if (state.personaTagline.isNotBlank()) {
                Text(
                    state.personaTagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(
                togetherLine(state.daysTogether),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Yours")
        MeRow("Your Alter Ego") { onNavigate(Destinations.ALTER_EGO_PICKER) }
        MeRow("Commitments") { onNavigate(Destinations.COMMITMENTS) }
        MeRow("Teach me what to say") { onNavigate(Destinations.QUOTES) }
        MeRow("Future Me") { onNavigate(Destinations.FUTURE_ME) }
        MeRow("Scheduled reminders") { onNavigate(Destinations.REMINDERS) }

        Spacer(Modifier.height(24.dp))
        SectionLabel("The app")
        MeRow("Learn") { onNavigate(Destinations.SCIENCE) }
        MeRow("Settings") { onNavigate(Destinations.SETTINGS) }
        MeRow("Privacy and data") { onNavigate(Destinations.PRIVACY) }
        MeRow("Alter Ego+", trailing = if (state.isPlus) "Active" else null) { onNavigate(Destinations.PREMIUM) }

        Spacer(Modifier.height(40.dp))
    }
}

/** Never a streak. Time together, said the way a person would say it. */
private fun togetherLine(days: Int): String = when {
    days <= 0 -> "Day one."
    days == 1 -> "We've been doing life together for a day."
    else -> "We've been doing life together for $days days."
}

@Composable
internal fun MeRow(
    label: String,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    val colors = LocalPersonaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelSmall, color = colors.accent)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
    }
}

/**
 * Shared frame for every screen underneath Me: one back arrow, one title, then the content.
 * Deliberately not a TopAppBar, so the surface stays as quiet as the rest of the app.
 */
@Composable
internal fun MeDetailScaffold(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPersonaColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 24.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            content()
            Spacer(Modifier.height(48.dp))
        }
    }
}
