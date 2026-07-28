package com.dj.photobooth.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.dj.photobooth.theme.PhotoboothColors

/**
 * Generates a placeholder frame bitmap for [com.dj.photobooth.capture.CaptureFrame]s captured
 * in placeholder mode (camera denied/unavailable) - CaptureModels.kt's TODO on CaptureFrame
 * flagged this as blocked on the Skia-based compositor landing, which it now has (Phase 2),
 * so this is that follow-up. Without this, StripCompositor.compose (which requires a fully
 * populated, real ImageBitmap per slot) would have nothing to draw for a placeholder frame,
 * and Save/Export would have no pixels to write for that session.
 *
 * Simplified vs. design/handoff/README.md § Assets' full spec (a `#dfe7f0`->`#eef6ff`
 * gradient with 46px 45deg `#b5d9fd` strokes and a centred "FRAME 0N - NO CAMERA" label): flat
 * fill + coarse diagonal hatch, no text. The text label needs a FontFamily.Resolver to build a
 * Paragraph outside a @Composable context - the exact same gap StripCompositor.kt's footer-
 * text TODO already documents as deferred, so this doesn't add a new deferred item so much as
 * hit the same one.
 */
internal fun placeholderFrameBitmap(width: Int = 1200, height: Int = 900): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)

    canvas.drawRect(
        left = 0f,
        top = 0f,
        right = width.toFloat(),
        bottom = height.toFloat(),
        paint = Paint().apply { color = PhotoboothColors.AccentTintFaint },
    )

    val stroke = Paint().apply {
        color = PhotoboothColors.AccentTintStrong
        strokeWidth = 6f
    }
    val step = 46f
    var x = -height.toFloat()
    while (x < width) {
        canvas.drawLine(Offset(x, height.toFloat()), Offset(x + height, 0f), stroke)
        x += step
    }

    return bitmap
}
