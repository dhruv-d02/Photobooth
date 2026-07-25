package com.dj.photobooth.filter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Composes accepted frames into one strip or grid image, per architecture.md's "Strip
 * composition - exact formulas" (itself transcribed from design/handoff/README.md). Pure
 * commonMain Kotlin, using only shared androidx.compose.ui.graphics APIs (Canvas, Paint,
 * ColorFilter, BlendMode) - no expect/actual needed here, unlike ImageDecoding.kt, since
 * these are genuinely uniform across every Compose Multiplatform target.
 *
 * Input frames are assumed already mirrored (CameraController.capturePhoto()'s documented
 * contract) - this does NOT mirror again, since that would flip them back.
 *
 * The pure dimension/placement/crop-rect math lives in CompositeGeometry.kt, separately
 * testable without touching ImageBitmap/Canvas at all - this class is just draw calls.
 */
object StripCompositor {

    private const val FOOTER_RULE_ALPHA = 0.35f

    /**
     * @param frames accepted frames in slot order (must be fully populated - compositing an
     *   in-progress session with gaps isn't a supported call site).
     */
    fun compose(
        frames: List<ImageBitmap>,
        treatment: FilmTreatment,
        frameColor: FrameColorPreset,
        layout: StripLayout,
    ): ImageBitmap {
        require(frames.isNotEmpty()) { "Cannot compose an empty frame list" }

        val geometry = CompositeGeometry.forLayout(layout, frames.size)
        val output = ImageBitmap(geometry.outputWidth, geometry.outputHeight)
        val canvas = Canvas(output)

        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = geometry.outputWidth.toFloat(),
            bottom = geometry.outputHeight.toFloat(),
            paint = Paint().apply { color = frameColor.background },
        )

        // FilmTreatment.None's colorMatrix is the identity transform - applying it would be
        // correct but wasted per-pixel work on every photo, every composite, for the most
        // common case (no treatment selected). Skip setting a colorFilter entirely for it.
        val photoPaint = Paint().apply {
            if (treatment != FilmTreatment.None) {
                colorFilter = ColorFilter.colorMatrix(treatment.colorMatrix)
            }
        }

        frames.forEachIndexed { index, frame ->
            val (cellX, cellY) = geometry.cellOrigin(index)
            drawFrameIntoCell(canvas, frame, photoPaint, cellX, cellY, geometry.photoWidth, geometry.photoHeight)

            if (treatment.duotoneOverlay != null) {
                val overlayPaint = Paint().apply {
                    color = treatment.duotoneOverlay
                    alpha = treatment.duotoneOverlayAlpha
                    blendMode = BlendMode.Multiply
                }
                canvas.drawRect(
                    left = cellX.toFloat(),
                    top = cellY.toFloat(),
                    right = (cellX + geometry.photoWidth).toFloat(),
                    bottom = (cellY + geometry.photoHeight).toFloat(),
                    paint = overlayPaint,
                )
            }
        }

        drawFooterRule(canvas, frameColor, geometry.outputWidth, geometry.outputHeight)
        // TODO(follow-up): brand text (left) + date stamp (right) in the footer - needs a
        // FontFamily.Resolver to construct an androidx.compose.ui.text.Paragraph outside a
        // @Composable context, which needs its own small expect/actual (Android needs a
        // Context; other Skiko targets don't) - the footer rule/geometry above is unaffected,
        // this only leaves the two text labels themselves unimplemented for now. Not yet met:
        // architecture.md's compositor contract documents both labels as part of the footer.

        return output
    }

    private fun drawFrameIntoCell(
        canvas: Canvas,
        frame: ImageBitmap,
        paint: Paint,
        cellX: Int,
        cellY: Int,
        cellW: Int,
        cellH: Int,
    ) {
        val crop = centerCropRect(frame.width, frame.height, cellW, cellH)
        canvas.drawImageRect(
            image = frame,
            srcOffset = IntOffset(crop.x, crop.y),
            srcSize = IntSize(crop.width, crop.height),
            dstOffset = IntOffset(cellX, cellY),
            dstSize = IntSize(cellW, cellH),
            paint = paint,
        )
    }

    private fun drawFooterRule(
        canvas: Canvas,
        frameColor: FrameColorPreset,
        outputWidth: Int,
        outputHeight: Int,
    ) {
        val ruleY = (outputHeight - FOOTER_HEIGHT).toFloat()
        val paint = Paint().apply {
            color = frameColor.text
            alpha = FOOTER_RULE_ALPHA
            strokeWidth = 1f
        }
        canvas.drawLine(
            p1 = Offset(PADDING.toFloat(), ruleY),
            p2 = Offset((outputWidth - PADDING).toFloat(), ruleY),
            paint = paint,
        )
    }
}
