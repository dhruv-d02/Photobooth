package com.dj.photobooth.filter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil

/**
 * Composes accepted frames into one strip or grid image, per architecture.md's "Strip
 * composition - exact formulas" (itself transcribed from design/handoff/README.md). Pure
 * commonMain Kotlin, using only shared androidx.compose.ui.graphics APIs (Canvas, Paint,
 * ColorFilter, BlendMode) - no expect/actual needed here, unlike ImageDecoding.kt, since
 * these are genuinely uniform across every Compose Multiplatform target.
 *
 * Input frames are assumed already mirrored (CameraController.capturePhoto()'s documented
 * contract) - this does NOT mirror again, since that would flip them back.
 */
object StripCompositor {

    // Design units doubled for print-scale output (2x6in at ~300dpi) - see architecture.md.
    private const val SCALE = 2
    private const val CONTENT_WIDTH = 320 * SCALE
    private const val PADDING = 16 * SCALE
    private const val GAP = 10 * SCALE
    private const val FOOTER_HEIGHT = 30 * SCALE
    private const val STRIP_PHOTO_W = 320 * SCALE
    private const val STRIP_PHOTO_H = 240 * SCALE
    private const val GRID_COLUMNS = 2
    private const val GRID_PHOTO_W = 155 * SCALE
    private const val GRID_PHOTO_H = 155 * SCALE

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

        val columns = if (layout == StripLayout.Strip) 1 else GRID_COLUMNS
        val photoW = if (layout == StripLayout.Strip) STRIP_PHOTO_W else GRID_PHOTO_W
        val photoH = if (layout == StripLayout.Strip) STRIP_PHOTO_H else GRID_PHOTO_H
        val rows = ceil(frames.size / columns.toFloat()).toInt()

        val outputWidth = 2 * PADDING + CONTENT_WIDTH
        val outputHeight = 2 * PADDING + rows * photoH + (rows - 1).coerceAtLeast(0) * GAP + FOOTER_HEIGHT

        val output = ImageBitmap(outputWidth, outputHeight)
        val canvas = Canvas(output)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(outputWidth.toFloat(), outputHeight.toFloat()),
        ) {
            drawRect(color = frameColor.background)
        }

        val photoPaint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(treatment.colorMatrix)
        }

        frames.forEachIndexed { index, frame ->
            val col = index % columns
            val row = index / columns
            val cellX = PADDING + col * (photoW + GAP)
            val cellY = PADDING + row * (photoH + GAP)
            drawFrameIntoCell(canvas, frame, photoPaint, cellX, cellY, photoW, photoH)

            if (treatment.duotoneOverlay != null) {
                val overlayPaint = Paint().apply {
                    color = treatment.duotoneOverlay
                    alpha = treatment.duotoneOverlayAlpha
                    blendMode = BlendMode.Multiply
                }
                canvas.drawRect(
                    left = cellX.toFloat(),
                    top = cellY.toFloat(),
                    right = (cellX + photoW).toFloat(),
                    bottom = (cellY + photoH).toFloat(),
                    paint = overlayPaint,
                )
            }
        }

        drawFooterRule(canvas, frameColor, outputWidth, outputHeight)
        // TODO(follow-up): brand text (left) + date stamp (right) in the footer - needs a
        // FontFamily.Resolver to construct an androidx.compose.ui.text.Paragraph outside a
        // @Composable context, which needs its own small expect/actual (Android needs a
        // Context; other Skiko targets don't) - the footer rule/geometry above is unaffected,
        // this only leaves the two text labels themselves unimplemented for now.

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
        // Center-crop the source to the cell's aspect ratio before scaling to fill, so a
        // wider/taller-than-4:3 source (whatever resolution the camera actually picked)
        // never distorts - it crops instead, matching "clipped" in the design spec.
        val cellAspect = cellW.toFloat() / cellH.toFloat()
        val srcAspect = frame.width.toFloat() / frame.height.toFloat()
        val (srcW, srcH) = if (srcAspect > cellAspect) {
            (frame.height * cellAspect).toInt() to frame.height
        } else {
            frame.width to (frame.width / cellAspect).toInt()
        }
        val srcX = (frame.width - srcW) / 2
        val srcY = (frame.height - srcH) / 2

        canvas.drawImageRect(
            image = frame,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
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
