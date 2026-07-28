package com.dj.photobooth.compose

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Encodes an [ImageBitmap] to PNG bytes - the inverse of [decodeJpegToImageBitmap], and
 * expect/actual for the same reason: Android's ImageBitmap is android.graphics.Bitmap-backed
 * (encode via Bitmap.compress), while every other Compose Multiplatform target is Skia/Skiko-
 * backed (encode via org.jetbrains.skia.Image) - there is no single cross-platform encode API
 * the way there is for the ColorMatrix/Canvas compositing work in StripCompositor.
 *
 * architecture.md's compositor contract: "final export is PNG" - this is what turns
 * StripCompositor's composed ImageBitmap into the bytes MediaRepo.savePng actually writes.
 */
expect fun encodeImageBitmapToPng(bitmap: ImageBitmap): ByteArray
