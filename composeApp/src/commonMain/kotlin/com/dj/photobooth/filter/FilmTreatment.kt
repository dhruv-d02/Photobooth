package com.dj.photobooth.filter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import com.dj.photobooth.filter.ColorMatrixOps.then

/**
 * The "flicket" presets - the first 5 (None/Disposable/Sunkissed/Cyber/Dreamy) are from
 * design/handoff/README.md § "Design Tokens" / dc.html's `FILMS` table, CSS filter equivalents
 * transcribed onto [ColorMatrixOps]'s primitives; [BlackAndWhite] is a maintainer addition on
 * top of that fixed set. None of them use a duotone overlay (that was an Industry-era-only
 * concept), so [duotoneOverlay] stays unused but is kept on the type in case a future preset
 * needs it. The Customize screen's flicket row iterates [entries] directly, so adding a preset
 * here is enough to surface it in the UI - no screen-level change needed.
 */
enum class FilmTreatment(
    val code: String,
    val displayName: String,
    val colorMatrix: ColorMatrix,
    val duotoneOverlay: Color? = null,
    val duotoneOverlayAlpha: Float = 0f,
) {
    None(
        code = "F00",
        displayName = "no filter",
        colorMatrix = ColorMatrix(),
    ),
    Disposable(
        code = "F01",
        displayName = "disposable",
        colorMatrix = ColorMatrixOps.saturate(1.5f)
            .then(ColorMatrixOps.contrast(1.15f))
            .then(ColorMatrixOps.brightness(1.05f)),
    ),
    Sunkissed(
        code = "F02",
        displayName = "sunkissed",
        colorMatrix = ColorMatrixOps.sepia(0.45f)
            .then(ColorMatrixOps.saturate(1.3f))
            .then(ColorMatrixOps.brightness(1.08f))
            .then(ColorMatrixOps.hueRotateDegrees(-6f)),
    ),
    Cyber(
        code = "F03",
        displayName = "cyber",
        colorMatrix = ColorMatrixOps.saturate(1.6f)
            .then(ColorMatrixOps.hueRotateDegrees(175f))
            .then(ColorMatrixOps.contrast(1.1f)),
    ),
    Dreamy(
        code = "F04",
        displayName = "dreamy",
        colorMatrix = ColorMatrixOps.brightness(1.15f)
            .then(ColorMatrixOps.saturate(0.75f))
            .then(ColorMatrixOps.contrast(0.92f)),
    ),
    BlackAndWhite(
        code = "F05",
        displayName = "black & white",
        colorMatrix = ColorMatrixOps.grayscale(1f)
            .then(ColorMatrixOps.contrast(1.12f)),
    ),
}
