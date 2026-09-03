package com.alterego.app.feature.science

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.domain.models.Lesson

/**
 * The science library.
 *
 * Everything here is curated content from the bundle; the screen never composes a health claim of
 * its own. A lesson is a minute or so of reading, and each claim inside it carries its evidence
 * label and its source.
 */
@Composable
fun ScienceScreen(
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onOpenBiology: () -> Unit,
    viewModel: ScienceViewModel = hiltViewModel(),
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
        ScienceHeader(title = "Learn", onBack = onBack)

        Text(
            "Straight answers with the evidence attached. No estimates about your body.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        BiologyRow(currentDay = state.currentDay, onClick = onOpenBiology)

        ScienceCategory.entries.forEach { category ->
            val lessons = state.lessons(category)
            if (lessons.isEmpty()) return@forEach
            Spacer(Modifier.height(28.dp))
            SectionLabel(category.heading)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lessons.forEach { lesson ->
                    LessonRow(lesson = lesson, onClick = { onOpenLesson(lesson.lessonId) })
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Nothing here is medical advice. Individual semen quality can only be measured by a " +
                "laboratory semen analysis.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
internal fun ScienceHeader(title: String, onBack: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column {
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
        Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
    }
}

@Composable
private fun BiologyRow(currentDay: Int, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.accent.copy(alpha = 0.14f))
            .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("The biology timeline", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            Text(
                if (currentDay > 0) {
                    "Day $currentDay of your chapter, and what research says about that window."
                } else {
                    "What research says about ejaculatory abstinence, phase by phase."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, onClick: () -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.muted.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(lesson.title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        Text(
            "About ${lesson.readSeconds} seconds",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
