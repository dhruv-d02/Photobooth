package com.dj.photobooth.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeJpegToImageBitmap(bytes: ByteArray): ImageBitmap =
    requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
        "Failed to decode image (${bytes.size} bytes)"
    }.asImageBitmap()
