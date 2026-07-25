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
            CornerTicks(
                modifier = Modifier
                    .padding(PhotoboothSpacing.xl)
                    .border(1.dp, PhotoboothColors.Accent)
                    .background(PhotoboothColors.Paper)
                    .padding(PhotoboothSpacing.lgLarge),
            ) {
                Text(
                    text = "PHOTOBOOTH — SCAFFOLD OK",
                    style = PhotoboothType.heading12,
                    color = PhotoboothColors.Accent,
                )
            }
        }
    }
}