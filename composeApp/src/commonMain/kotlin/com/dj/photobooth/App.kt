package com.dj.photobooth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothTheme
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.CornerTicks

/**
 * Phase 0 root composable: a deliberately minimal placeholder, not the real Landing
 * screen (design/handoff/README.md § 1, variant 1a "Spec sheet"). Its job is only to
 * prove the KMP + Compose Multiplatform scaffold, the design-token theme, and the
 * CornerTicks motif all actually work end-to-end on a real device before any screen
 * work starts in Phase 1+. Building the real Landing screen here would jump ahead of
 * the phased build sequence in CLAUDE.md.
 */
@Composable
fun App() {
    PhotoboothTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PhotoboothColors.Ground),
            contentAlignment = Alignment.Center,
        ) {
            // The inner content padding must NOT be part of the modifier chain passed to
            // CornerTicks: CornerTicks aligns its "+" marks to that Box's own bounds, so
            // any padding baked into this chain shrinks those bounds and pulls the marks
            // back inside the visible border instead of landing outside it. The border/
            // background belong here (they define the box CornerTicks decorates); the
            // content padding moves onto the Text itself, below, where it only affects
            // the text's position, not where the corner ticks are anchored.
            CornerTicks(
                modifier = Modifier
                    .padding(PhotoboothSpacing.xl)
                    .border(1.dp, PhotoboothColors.Accent)
                    .background(PhotoboothColors.Paper),
            ) {
                Text(
                    text = "PHOTOBOOTH — SCAFFOLD OK",
                    style = PhotoboothType.heading12,
                    color = PhotoboothColors.Accent,
                    modifier = Modifier.padding(PhotoboothSpacing.lgLarge),
                )
            }
        }
    }
}