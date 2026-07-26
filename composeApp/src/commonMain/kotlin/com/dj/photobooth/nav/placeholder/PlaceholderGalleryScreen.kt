package com.dj.photobooth.nav.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
 * Placeholder for the real "Past strips" gallery (design/handoff/README.md § 4). The real
 * screen (grid of saved strips backed by Room, per architecture.md's HistoryEntry table) is
 * Phase 3 scope being built in parallel on `feature/export-history` and does not exist in
 * this worktree yet.
 *
 * Deliberately kept in `nav.placeholder`, NOT in a `gallery` package - the parallel branch
 * owns `com.dj.photobooth.gallery` (its real GalleryScreen/repo/DAO live there), so putting a
 * placeholder in that same package/path would collide on merge. This placeholder exists only
 * so the Strips tab's route in [com.dj.photobooth.nav.PhotoboothNavHost] compiles and is
 * reachable/testable end-to-end right now. Swapping it for the real GalleryScreen later is a
 * one-line change in that NavHost's `composable(Route.Strips.route)` block.
 */
@Composable
fun PlaceholderGalleryScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.Ground)
            .padding(PhotoboothSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        CornerTicks {
            Box(
                modifier = Modifier
                    .border(1.dp, PhotoboothColors.HairlineOnLight)
                    .padding(PhotoboothSpacing.xxl),
            ) {
                Text(
                    text = "GALLERY · COMING SOON",
                    style = PhotoboothType.meta11,
                    color = PhotoboothColors.TextMuted,
                )
            }
        }
    }
}
