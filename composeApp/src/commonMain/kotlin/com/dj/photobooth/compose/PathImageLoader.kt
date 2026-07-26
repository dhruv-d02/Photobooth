package com.dj.photobooth.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Loads a previously-saved image back from the platform path/URI a MediaRepo.savePng call
 * returned (a `content://` URI on Android, a Photos identifier on iOS) - needed by the
 * Gallery grid, which reads HistoryEntry.finalImagePath rather than holding raw bytes in
 * memory. A plain interface, not expect/actual for the load itself - same reasoning as
 * CameraController/MediaRepo: Android needs a ContentResolver (itself Context-derived), iOS
 * needs a different asset-resolution mechanism entirely.
 *
 * Returns null (rather than throwing) on any failure - a missing/revoked image shouldn't
 * crash the Gallery grid, just render that one card without a thumbnail.
 */
interface PathImageLoader {
    suspend fun load(path: String): ImageBitmap?
}

/**
 * Obtains the platform [PathImageLoader] from within a Composable - this one *is* expect/
 * actual (unlike the interface above) because it needs a platform-specific ambient (Android's
 * LocalContext) to construct its concrete implementation, the same shape as
 * CameraPreviewSurface needing platform view types.
 */
@Composable
expect fun rememberPathImageLoader(): PathImageLoader
