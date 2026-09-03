package com.alterego.app.feature.science

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alterego.app.core.design.CenteredMessage
import com.alterego.app.core.design.EvidenceBadge
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.domain.models.EvidenceClaim
import com.alterego.app.domain.models.LessonBlock

/**
 * One lesson.
 *
 * Citation rendering is the credibility mechanism of this app: every health statement appears
 * inside a claim card with its evidence label, its source and the date it was last reviewed.
 * Text blocks are the author's prose; claim blocks are the receipts.
 */
@Composable
fun LessonScreen(
    lessonId: String,
    onBack: () -> Unit,
    viewModel: ScienceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalPersonaColors.current
    val lesson = remember(state, lessonId) { viewModel.lessonById(lessonId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        ScienceHeader(title = lesson?.title ?: "Lesson", onBack = onBack)

        if (lesson == null) {
            if (!state.loading) CenteredMessage("This lesson isn't available.")
            Spacer(Modifier.height(40.dp))
            return@Column
        }

        Text(
            "About ${lesson.readSeconds} seconds",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(24.dp))

        lesson.blocks.forEach { block ->
            LessonBlockView(block = block, claim = viewModel.claimById(block.claimId))
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Sources are reviewed and can be corrected without an app update. If something here " +
                "looks wrong, it should be reported.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun LessonBlockView(block: LessonBlock, claim: EvidenceClaim?) {
    val colors = LocalPersonaColors.current
    when (block.type) {
        "text" -> Text(
            block.text.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBackground,
        )

        "callout" -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.accent.copy(alpha = 0.12f))
                .border(1.5.dp, colors.accent, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(block.text.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
        }

        "claim" -> if (claim != null) ClaimCard(claim)

        else -> if (!block.text.isNullOrBlank()) {
            Text(block.text.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
        }
    }
}

/** The receipt: what is claimed, how strong the evidence is, who said it and when it was checked. */
@Composable
internal fun ClaimCard(claim: EvidenceClaim) {
    val colors = LocalPersonaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.muted.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(claim.claim, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
        Spacer(Modifier.height(12.dp))
        EvidenceBadge(level = claim.evidenceLevel)
        Spacer(Modifier.height(10.dp))
        SourceLine(claim)
        Text(
            "Reviewed ${claim.reviewDate}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Compact citation used where a full claim card would crowd the page. */
@Composable
internal fun ClaimCitation(claim: EvidenceClaim) {
    Column(Modifier.fillMaxWidth()) {
        EvidenceBadge(level = claim.evidenceLevel)
        Spacer(Modifier.height(8.dp))
        SourceLine(claim)
    }
}

@Composable
private fun SourceLine(claim: EvidenceClaim) {
    val colors = LocalPersonaColors.current
    val uriHandler = LocalUriHandler.current
    val year = claim.publicationYear
    val title = if (year != null) "${claim.sourceTitle} ($year)" else claim.sourceTitle
    val url = claim.sourceUrl

    if (url.isNullOrBlank()) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = colors.muted)
        return
    }
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = colors.accent,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } },
    )
}
