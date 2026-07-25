package com.dj.photobooth.compose

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes JPEG bytes into an [ImageBitmap]. Has to be expect/actual, not a plain shared
 * function: Compose Multiplatform's own `decodeToImageBitmap()` resource utility explicitly
 * excludes Android (it's Skiko-backed, and Android's Compose graphics don't route through
 * Skiko) - so decoding genuinely isn't available as one cross-platform API, unlike the
 * ColorMatrix/Canvas compositing work in FilterEngine/StripCompositor, which is.
 */
expect fun decodeJpegToImageBitmap(bytes: ByteArray): ImageBitmap
