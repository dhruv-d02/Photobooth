package com.dj.photobooth.filter

import kotlin.math.ceil

// Design units doubled for print-scale output (2x6in at ~300dpi) - see architecture.md
// § "Strip composition - exact formulas".
internal const val SCALE = 2
internal const val CONTENT_WIDTH = 320 * SCALE
internal const val PADDING = 16 * SCALE
internal const val GAP = 10 * SCALE
internal const val FOOTER_HEIGHT = 30 * SCALE
internal const val STRIP_PHOTO_W = 320 * SCALE
internal const val STRIP_PHOTO_H = 240 * SCALE
internal const val GRID_COLUMNS = 2
internal const val GRID_PHOTO_W = 155 * SCALE
internal const val GRID_PHOTO_H = 155 * SCALE

/**
 * The compositor's pure arithmetic - output dimensions and per-cell placement - factored out
 * of StripCompositor so it's testable as plain numbers in commonTest, without needing
 * Robolectric/real Bitmap-Canvas rendering the way pixel-level compositor tests do. This is
 * also what actually gets verified on every target (including iOS, which can't run the
 * Robolectric-based rendering tests on this machine) - the geometry formula itself is
 * platform-uniform and shouldn't only be checked through an Android-specific test.
 */
internal data class CompositeGeometry(
    val columns: Int,
    val photoWidth: Int,
    val photoHeight: Int,
    val rows: Int,
    val outputWidth: Int,
    val outputHeight: Int,
) {
    /** Top-left pixel coordinate of the cell at [index] (0-based, row-major). */
    fun cellOrigin(index: Int): Pair<Int, Int> {
        val col = index % columns
        val row = index / columns
        val x = PADDING + col * (photoWidth + GAP)
        val y = PADDING + row * (photoHeight + GAP)
        return x to y
    }

    companion object {
        fun forLayout(layout: StripLayout, frameCount: Int): CompositeGeometry {
            require(frameCount > 0) { "frameCount must be positive, was $frameCount" }
            val columns = if (layout == StripLayout.Strip) 1 else GRID_COLUMNS
            val photoW = if (layout == StripLayout.Strip) STRIP_PHOTO_W else GRID_PHOTO_W
            val photoH = if (layout == StripLayout.Strip) STRIP_PHOTO_H else GRID_PHOTO_H
            val rows = ceil(frameCount / columns.toFloat()).toInt()
            val outputWidth = 2 * PADDING + CONTENT_WIDTH
            val outputHeight = 2 * PADDING + rows * photoH + (rows - 1).coerceAtLeast(0) * GAP + FOOTER_HEIGHT
            return CompositeGeometry(columns, photoW, photoH, rows, outputWidth, outputHeight)
        }
    }
}

/**
 * Center-crop source rect (in the source image's own pixel space) that matches [cellWidth]x
 * [cellHeight]'s aspect ratio, so scaling that rect to fill the cell never distorts - it
 * crops instead, matching "clipped" in the design spec. Pure integer math, no ImageBitmap
 * dependency, so this is independently testable from the actual drawImageRect call.
 */
internal fun centerCropRect(
    sourceWidth: Int,
    sourceHeight: Int,
    cellWidth: Int,
    cellHeight: Int,
): CropRect {
    val cellAspect = cellWidth.toFloat() / cellHeight.toFloat()
    val srcAspect = sourceWidth.toFloat() / sourceHeight.toFloat()
    val (cropW, cropH) = if (srcAspect > cellAspect) {
        (sourceHeight * cellAspect).toInt() to sourceHeight
    } else {
        sourceWidth to (sourceWidth / cellAspect).toInt()
    }
    val cropX = (sourceWidth - cropW) / 2
    val cropY = (sourceHeight - cropH) / 2
    return CropRect(cropX, cropY, cropW, cropH)
}

internal data class CropRect(val x: Int, val y: Int, val width: Int, val height: Int)
