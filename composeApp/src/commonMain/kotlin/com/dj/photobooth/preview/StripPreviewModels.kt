package com.dj.photobooth.preview

import androidx.compose.ui.graphics.ImageBitmap
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.theme.Brand

/**
 * UI state for the Strip Preview & Customise screen (design/handoff/README.md §3). Mirrors
 * CaptureUiState's shape: one immutable data class, one StateFlow, view methods only read it.
 *
 * [decodedFrames] holds one entry per accepted-frame slot, in the same order as the source
 * CaptureSession's frames - null only while a slot's JPEG is still being decoded off-thread
 * (see StripPreviewViewModel.runPipeline/decodeFrame, which null every slot on construction,
 * and replaceFrame, which nulls just the retaken slot), never left null once decoding settles:
 * CaptureFrame.isPlaceholder / decode-failure cases both fall back to a generated placeholder
 * bitmap (PlaceholderFrame.kt) rather than staying null forever, since StripCompositor.compose
 * requires a fully-populated, non-null frame list to draw from.
 */
data class StripPreviewUiState(
    val decodedFrames: List<ImageBitmap?> = emptyList(),
    val treatment: FilmTreatment = FilmTreatment.None,
    val frameColor: FrameColorPreset = FrameColorPreset.Bubblegum,
    val layout: StripLayout = StripLayout.Strip,
    val composedImage: ImageBitmap? = null,
    val isComposing: Boolean = false,
    val stamp: String = "",
    val brand: String = Brand.NAME,
)
