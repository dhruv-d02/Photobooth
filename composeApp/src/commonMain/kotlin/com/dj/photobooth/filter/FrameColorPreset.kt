package com.dj.photobooth.filter

import com.dj.photobooth.theme.PhotoboothColors

/**
 * Frame-color presets - the strip/grid background choices (design/handoff/README.md's
 * Design Tokens § Frame options). This is Phase 2 work, not Phase 0 scaffolding (CLAUDE.md's
 * build sequence), which is why it wasn't defined back when theme/Color.kt was first
 * written - see that file's comment for the historical note.
 */
enum class FrameColorPreset(
    val background: androidx.compose.ui.graphics.Color,
    val text: androidx.compose.ui.graphics.Color,
    val dim: androidx.compose.ui.graphics.Color,
) {
    Paper(PhotoboothColors.Paper, PhotoboothColors.TextPrimary, PhotoboothColors.TextMuted),
    Steel(PhotoboothColors.AccentDeeper, PhotoboothColors.Paper, PhotoboothColors.AccentTintStrong),
    Ink(PhotoboothColors.Ink, PhotoboothColors.Paper, PhotoboothColors.OnInkSecondaryText),
    Sky(PhotoboothColors.AccentTintMedium, PhotoboothColors.DarkSurface, PhotoboothColors.AccentPressed),
}

/** Strip = classic vertical photobooth strip (1 column). Grid = 2x2. Both ship in v1. */
enum class StripLayout { Strip, Grid }
