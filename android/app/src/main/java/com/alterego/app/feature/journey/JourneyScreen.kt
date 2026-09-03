package com.alterego.app.feature.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.CenteredMessage
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.QuietButton
import com.alterego.app.core.design.SectionLabel
import com.alterego.app.core.design.StatRow
import com.alterego.app.domain.models.Chapter
import com.alterego.app.domain.models.LifeTimelineEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A full year has to be behind you before the app offers to look back on one. */
private const val DAYS_IN_YEAR = 365

/**
 * Journey is a memory, not a scoreboard.
 *
 * No graphs, no comparison with other people, no streak that can be lost. Ended chapters are
 * described by how far they went, never by how they finished.
 */
@Composable
fun JourneyScreen(
    onOpenAnnualReview: () -> Unit,
    onOpenSaved: () -> Unit,
    viewModel: JourneyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current
    val stats = state.stats

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 24.dp),
    ) {
        item { SectionLabel("Your journey") }

        if (stats == null) {
            item {
                CenteredMessage(
                    if (state.loading) "Gathering your journey." else "Your journey starts with your first commitment.",
                )
            }
            return@LazyColumn
        }

        item {
            Column {
                Text("Current commitment", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                Text(
                    dayCount(stats.currentDay),
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.onBackground,
                    modifier = Modifier.padding(top = 4.dp),
                )
                state.currentCommitment?.let { commitment ->
                    Text(
                        commitment.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
        item { SectionLabel("This year") }
        item { StatRow(value = stats.committedDaysThisYear.toString(), label = "Days committed") }
        item { StatRow(value = stats.restartsThisYear.toString(), label = "Restarts") }
        item { StatRow(value = "${stats.alignedPercentThisYear}%", label = "Days aligned") }

        item { Spacer(Modifier.height(24.dp)) }
        item { StatRow(value = dayCount(stats.longestChapterDays), label = "Longest chapter") }
        item { StatRow(value = formatJourneyDate(state.togetherSince), label = "Together since") }

        if (state.chapters.isNotEmpty()) {
            item { Spacer(Modifier.height(32.dp)) }
            item { SectionLabel("Chapters") }
            items(state.chapters) { chapter -> ChapterRow(chapter = chapter, currentDay = stats.currentDay) }
        }

        item { Spacer(Modifier.height(32.dp)) }
        item {
            // Lifetime perspective. This is why a reset can never zero anything: committed days only
            // ever accumulate, and they are read against the whole time we have been together.
            Text(
                "${stats.lifetimeCommittedDays} committed days out of the last ${stats.lifetimeDays}.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground,
            )
        }

        val headsUp = state.pattern.headsUpText()
        if (state.pattern.isMeaningful && headsUp != null) {
            item { Spacer(Modifier.height(24.dp)) }
            item {
                QuietCard {
                    state.pattern.highRiskHour?.let { hour ->
                        Text(
                            "Most of your chapters end around ${formatHourOfDay(hour)}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBackground,
                        )
                    }
                    Text(
                        headsUp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        if (state.lifeTimeline.isNotEmpty()) {
            item { Spacer(Modifier.height(32.dp)) }
            item { SectionLabel("Your life so far") }
            state.lifeTimeline
                .sortedByDescending { it.at }
                .groupBy { it.year }
                .forEach { (year, entries) ->
                    item { SectionLabel(year.toString()) }
                    items(entries) { entry -> LifeTimelineRow(entry) }
                    item { Spacer(Modifier.height(12.dp)) }
                }
        }

        item { Spacer(Modifier.height(32.dp)) }
        item { QuietButton(text = "Saved moments", onClick = onOpenSaved) }

        // A year of walking together is the only thing that opens the annual review.
        if (stats.daysTogether >= DAYS_IN_YEAR) {
            item { Spacer(Modifier.height(12.dp)) }
            item { QuietButton(text = "Another year together", onClick = onOpenAnnualReview) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, currentDay: Int) {
    val colors = LocalPersonaColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            if (chapter.isOpen) {
                "Chapter ${chapter.number} - ${dayCount(currentDay)}"
            } else {
                // Never "failed" or "broken": a closed chapter is named by how far it went.
                "Chapter ${chapter.number} ended at ${dayCount(chapter.lengthDays(currentDay))}"
            },
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
        )
        chapter.endedAt?.let { endedAt ->
            Text(
                formatJourneyDate(endedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun LifeTimelineRow(entry: LifeTimelineEntry) {
    val colors = LocalPersonaColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(entry.text, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
        Text(
            formatJourneyDate(entry.at),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
internal fun QuietCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalPersonaColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

private val JourneyDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault())

internal fun formatJourneyDate(instant: Instant): String = JourneyDateFormat.format(instant)

internal fun dayCount(days: Int): String = if (days == 1) "1 day" else "$days days"

/** 23 becomes "11 PM". Used for the difficult-window line, which is a report, never a diagnosis. */
internal fun formatHourOfDay(hour: Int): String {
    val normalised = ((hour % 24) + 24) % 24
    val suffix = if (normalised < 12) "AM" else "PM"
    val display = if (normalised % 12 == 0) 12 else normalised % 12
    return "$display $suffix"
}

/** A closed chapter carries its own end, so the argument only matters for the chapter still open. */
private fun Chapter.lengthDays(currentDay: Int): Int =
    if (isOpen) currentDay else (durationMillis(startedAt) / Chapter.MILLIS_PER_DAY).toInt()
