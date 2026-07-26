package com.dj.photobooth.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

// Skiko-backed target (same as every non-Android Compose Multiplatform target) - Kotlin/
// Native-only, unverified until Phase 4 like the rest of this file's package siblings
// (IosMediaRepo.kt, IosAppDatabaseFactory.kt): compiles as Kotlin/Native source but has
// never actually been built (only macOS can compile Apple targets).
actual fun encodeImageBitmapToPng(bitmap: ImageBitmap): ByteArray {
    val skiaImage = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    val data = requireNotNull(skiaImage.encodeToData(EncodedImageFormat.PNG)) {
        "Failed to PNG-encode the composed strip image"
    }
    return data.bytes
}
