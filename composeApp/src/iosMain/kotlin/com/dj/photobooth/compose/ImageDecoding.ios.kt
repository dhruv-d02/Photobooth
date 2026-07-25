package com.dj.photobooth.compose

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalResourceApi::class)
actual fun decodeJpegToImageBitmap(bytes: ByteArray): ImageBitmap = bytes.decodeToImageBitmap()
