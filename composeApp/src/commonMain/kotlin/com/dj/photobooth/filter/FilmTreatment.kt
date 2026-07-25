package com.dj.photobooth.filter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import com.dj.photobooth.filter.ColorMatrixOps.then
import com.dj.photobooth.theme.PhotoboothColors

/**
 * The 5 fixed film-treatment presets from design/handoff/README.md § "Film treatments (CSS
 * filter equivalents)". Not user-creatable (Phase 2 scope per CLAUDE.md's build sequence),
 * just this fixed set. [duotoneOverlay] is only non-null for F04 - the design specifies it
 * as a *separate* Multiply-blend overlay on top of the grayscale result, not something a
 * single ColorMatrix can express (a color matrix is a linear transform per-pixel; a blend
 * mode composites two images together).
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
        displayName = "None",
        colorMatrix = ColorMatrix(), // Compose's default ColorMatrix() constructor is the identity matrix
    ),
    BlackAndWhite(
        code = "F01",
        displayName = "Black & White",
        colorMatrix = ColorMatrixOps.grayscale(1f).then(ColorMatrixOps.contrast(1.12f)),
    ),
    Sepia(
        code = "F02",
        displayName = "Sepia",
        colorMatrix = ColorMatrixOps.sepia(0.72f)
            .then(ColorMatrixOps.contrast(1.05f))
            .then(ColorMatrixOps.saturate(1.1f)),
    ),
    Warm(
        code = "F03",
        displayName = "Warm",
        colorMatrix = ColorMatrixOps.saturate(1.25f)
            .then(ColorMatrixOps.contrast(1.05f))
            .then(ColorMatrixOps.brightness(1.05f))
            .then(ColorMatrixOps.hueRotateDegrees(-8f)),
    ),
    SteelDuotone(
        code = "F04",
        displayName = "Steel duotone",
        colorMatrix = ColorMatrixOps.grayscale(1f)
            .then(ColorMatrixOps.contrast(1.06f))
            .then(ColorMatrixOps.brightness(1.06f)),
        // Was a hardcoded Color(0xFFB5D9FD) literal on the theory of keeping filter/
        // independent of theme/ - but FrameColorPreset.kt (same package) already imports
        // PhotoboothColors directly, so that independence didn't actually hold; referencing
        // the real token here instead avoids the two silently drifting apart on a future
        // design-token change.
        duotoneOverlay = PhotoboothColors.AccentTintStrong,
        duotoneOverlayAlpha = 0.85f,
    ),
}
