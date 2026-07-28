package com.dj.photobooth.compose

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AndroidPathImageLoader(private val context: Context) : PathImageLoader {

    override suspend fun load(path: String, maxWidthPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(path)
            // Two passes. inJustDecodeBounds reads only the header (allocates no pixels) to
            // learn the real size, then inSampleSize decodes a downscaled version. Without
            // this every Gallery grid card allocates a full 2x-scale strip - roughly
            // 640x1800px, ~4.6MB as ARGB_8888 - and a screenful of cards OOMs modest devices.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, maxWidthPx)
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        } catch (e: Exception) {
            // Null rather than throwing - see PathImageLoader's doc comment: a missing or
            // revoked image should cost one blank card, not take down the whole grid.
            null
        }
    }
}

/** Largest power-of-two downscale that still leaves the image at least [maxWidthPx] wide.
 *  BitmapFactory rounds inSampleSize down to a power of two internally, so computing it
 *  explicitly avoids depending on that rounding behaviour. */
private fun sampleSizeFor(sourceWidth: Int, maxWidthPx: Int): Int {
    if (sourceWidth <= 0 || maxWidthPx <= 0) return 1
    var sample = 1
    while (sourceWidth / (sample * 2) >= maxWidthPx) {
        sample *= 2
    }
    return sample
}

@Composable
actual fun rememberPathImageLoader(): PathImageLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidPathImageLoader(context) }
}
