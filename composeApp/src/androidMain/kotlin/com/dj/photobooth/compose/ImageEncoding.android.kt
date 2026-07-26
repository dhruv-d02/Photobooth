package com.dj.photobooth.compose

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun encodeImageBitmapToPng(bitmap: ImageBitmap): ByteArray =
    ByteArrayOutputStream().use { stream ->
        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
