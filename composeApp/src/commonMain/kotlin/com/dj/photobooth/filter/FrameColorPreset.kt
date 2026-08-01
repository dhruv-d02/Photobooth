package com.dj.photobooth.filter

import androidx.compose.ui.graphics.Color
import com.dj.photobooth.theme.PhotoboothColors

/**
 * Frame-color presets - the strip/grid mount background choices (design/handoff/README.md's
 * Design Tokens § "Frame color presets", confirmed against the dc.html prototype's `FRAMES`
 * table). [dim] is the mount's footer dashed-rule color (the dc.html's `rule` field) - a
 * translucent tint of whichever of [text]'s two contrast families reads against [background].
 */
enum class FrameColorPreset(
    val background: Color,
    val text: Color,
    val dim: Color,
) {
    Butter(Color(0xFFFFC53D), PhotoboothColors.Ink, PhotoboothColors.Ink.copy(alpha = 0.3f)),
    Bubblegum(Color(0xFFFF6FBB), PhotoboothColors.Cream, PhotoboothColors.Cream.copy(alpha = 0.4f)),
    Grape(PhotoboothColors.Purple, PhotoboothColors.Cream, PhotoboothColors.Cream.copy(alpha = 0.4f)),
    Spearmint(Color(0xFF7FEBD1), PhotoboothColors.Ink, PhotoboothColors.Ink.copy(alpha = 0.3f)),
}

/** Strip = classic vertical photobooth strip (1 column). Grid = 2x2. Both ship in v1. */
enum class StripLayout { Strip, Grid }
