package com.dj.photobooth.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

/**
 * iOS PathImageLoader - Phase 4 territory, same status as this package's other iosMain
 * siblings. IosMediaRepo.savePng currently returns a display name, not a resolvable Photos
 * asset identifier (see its TODO), so there isn't yet a real lookup this could perform;
 * returning null keeps the Gallery grid rendering (cardless, per PathImageLoader's documented
 * "missing image" contract) instead of crashing, until that TODO and this one land together.
 */
private object NoopIosPathImageLoader : PathImageLoader {
    override suspend fun load(path: String): ImageBitmap? = null
}

@Composable
actual fun rememberPathImageLoader(): PathImageLoader = remember { NoopIosPathImageLoader }
