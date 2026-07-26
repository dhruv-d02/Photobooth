package com.dj.photobooth.nav.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.CornerTicks

/**
 * Placeholder for the real "Strip Preview & Customize" screen (design/handoff/README.md § 3).
 * The real screen (film-treatment/frame-color/layout chip rows, RESHOOT/per-cell RETAKE, SAVE
 * PNG) is Phase 3 scope being built in parallel on `feature/export-history` and does not exist
 * in this worktree yet.
 *
 * Deliberately kept in `nav.placeholder`, NOT in a `preview` package - avoids colliding with
 * whatever package path the parallel branch's real Strip Preview screen ends up in. This
 * placeholder exists only so [com.dj.photobooth.nav.Route.Preview] compiles and is
 * reachable/testable end-to-end right now (Capture -> Preview once a session completes, per
 * architecture.md's nav diagram). Swapping it for the real screen is a one-line change in
 * [com.dj.photobooth.nav.PhotoboothNavHost]'s `composable(Route.Preview.route)` block.
 *
 * [onBackToBooth] stands in for the real screen's eventual SAVE PNG action ("Preview
 * -->|SAVE PNG| Booth" in architecture.md's nav diagram) - wired to a plain button here so the
 * full Landing -> Capture -> Preview -> Booth loop is testable end-to-end even before the real
 * Preview screen exists.
 */
@Composable
fun PlaceholderPreviewScreen(onBackToBooth: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.Ground)
            .padding(PhotoboothSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        CornerTicks {
            Column(
                modifier = Modifier
                    .background(PhotoboothColors.Paper)
                    .border(1.dp, PhotoboothColors.HairlineOnLight)
                    .padding(PhotoboothSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PhotoboothSpacing.lg),
            ) {
                Text(
                    text = "STRIP PREVIEW · COMING SOON",
                    style = PhotoboothType.meta11,
                    color = PhotoboothColors.TextMuted,
                )
                Button(
                    onClick = onBackToBooth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhotoboothColors.Accent,
                        contentColor = PhotoboothColors.Paper,
                    ),
                ) {
                    Text("BACK TO BOOTH", style = PhotoboothType.heading12)
                }
            }
        }
    }
}
