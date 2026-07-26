package com.dj.photobooth.preview

import androidx.compose.ui.graphics.ImageBitmap
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout

/**
 * UI state for the Strip Preview & Customise screen (design/handoff/README.md §3). Mirrors
 * CaptureUiState's shape: one immutable data class, one StateFlow, view methods only read it.
 *
 * [decodedFrames] holds one entry per accepted-frame slot, in the same order as the source
 * CaptureSession's frames - null only while a slot's JPEG is still being decoded off-thread
 * (see StripPreviewViewModel.decodeAllFrames), never left null once decoding settles:
 * CaptureFrame.isPlaceholder / decode-failure cases both fall back to a generated placeholder
 * bitmap (PlaceholderFrame.kt) rather than staying null forever, since StripCompositor.compose
 * requires a fully-populated, non-null frame list to draw from.
 */
data class StripPreviewUiState(
    val decodedFrames: List<ImageBitmap?> = emptyList(),
    val treatment: FilmTreatment = FilmTreatment.None,
    val frameColor: FrameColorPreset = FrameColorPreset.Paper,
    val layout: StripLayout = StripLayout.Strip,
    val composedImage: ImageBitmap? = null,
    val isComposing: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val savedPath: String? = null,
    val saveError: String? = null,
    val stamp: String = "",
    val brand: String = "Photobooth",
) {
    /** Header-right "STRIP · F01"-style code (design/handoff/README.md line 131). */
    val layoutFilterCode: String
        get() = "${if (layout == StripLayout.Strip) "STRIP" else "GRID"} · ${treatment.code}"

    /** Action-bar caption, flips once a save actually lands (line 151-152). */
    val saveCaption: String
        get() = if (saved) "saved to photos · archived in strips" else "saving also archives a copy in strips"
}
