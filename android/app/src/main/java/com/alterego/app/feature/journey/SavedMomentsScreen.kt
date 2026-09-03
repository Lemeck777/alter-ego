package com.alterego.app.feature.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.CenteredMessage
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.QuietButton
import com.alterego.app.core.design.SectionLabel

/** The words the user chose to keep. Nothing here is ranked, dated or scored. */
@Composable
fun SavedMomentsScreen(onBack: () -> Unit, viewModel: JourneyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        SectionLabel("Saved moments")

        if (state.savedMoments.isEmpty()) {
            CenteredMessage("Nothing saved yet.", Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(state.savedMoments) { moment ->
                    QuietCard(Modifier.padding(vertical = 8.dp)) {
                        Text(
                            moment.lines.joinToString(" "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBackground,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        QuietButton(text = "Back", onClick = onBack)
    }
}
