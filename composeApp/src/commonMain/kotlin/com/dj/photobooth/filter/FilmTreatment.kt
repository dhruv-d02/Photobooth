package com.dj.photobooth.filter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import com.dj.photobooth.filter.ColorMatrixOps.then

/**
 * The 5 fixed "flicket" presets from design/handoff/README.md § "Design Tokens" / dc.html's
 * `FILMS` table - CSS filter equivalents transcribed onto [ColorMatrixOps]'s primitives. None
 * of the five use a duotone overlay (that was an Industry-era-only concept), so
 * [duotoneOverlay] stays unused but is kept on the type in case a future preset needs it.
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
}
