package com.dj.photobooth.filter

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure-math coverage for the compositor's dimension/placement/crop formulas - portable to
 * every target (no Robolectric/real Bitmap-Canvas needed, unlike StripCompositorTest, which
 * is Android-only). This is what actually verifies the geometry is correct on platforms this
 * machine can't run Android-specific tests on, e.g. iOS.
 */
class CompositeGeometryTest {

    @Test
    fun `strip layout dimensions match the documented formula`() {
        val geometry = CompositeGeometry.forLayout(StripLayout.Strip, frameCount = 4)
        assertEquals(1, geometry.columns)
        assertEquals(640, geometry.photoWidth)
        assertEquals(480, geometry.photoHeight)
        assertEquals(4, geometry.rows)
        assertEquals(2 * 32 + 640, geometry.outputWidth)
        assertEquals(2 * 32 + 4 * 480 + 3 * 20 + 60, geometry.outputHeight)
    }

    @Test
    fun `grid layout dimensions match the documented formula`() {
        val geometry = CompositeGeometry.forLayout(StripLayout.Grid, frameCount = 4)
        assertEquals(2, geometry.columns)
        assertEquals(310, geometry.photoWidth)
        assertEquals(310, geometry.photoHeight)
        assertEquals(2, geometry.rows)
        assertEquals(2 * 32 + 640, geometry.outputWidth)
        assertEquals(2 * 32 + 2 * 310 + 1 * 20 + 60, geometry.outputHeight)
    }

    @Test
    fun `grid layout rounds up to a partial final row`() {
        val geometry = CompositeGeometry.forLayout(StripLayout.Grid, frameCount = 3)
        assertEquals(2, geometry.rows)
    }

    @Test
    fun `single frame produces one row regardless of layout`() {
        assertEquals(1, CompositeGeometry.forLayout(StripLayout.Strip, frameCount = 1).rows)
        assertEquals(1, CompositeGeometry.forLayout(StripLayout.Grid, frameCount = 1).rows)
    }

    @Test
    fun `cell origin places frames in row-major order for a grid`() {
        val geometry = CompositeGeometry.forLayout(StripLayout.Grid, frameCount = 4)
        val (x0, y0) = geometry.cellOrigin(0)
        val (x1, y1) = geometry.cellOrigin(1)
        val (x2, y2) = geometry.cellOrigin(2)

        assertEquals(32 to 32, x0 to y0) // top-left: padding only
        assertEquals(32 + 310 + 20 to 32, x1 to y1) // top-right: one column over
        assertEquals(32 to 32 + 310 + 20, x2 to y2) // bottom-left: one row down
    }

    @Test
    fun `cell origin stacks strip cells in a single column`() {
        val geometry = CompositeGeometry.forLayout(StripLayout.Strip, frameCount = 3)
        val (x0, y0) = geometry.cellOrigin(0)
        val (x1, y1) = geometry.cellOrigin(1)

        assertEquals(x0, x1, "strip layout has one column - every cell shares the same x")
        assertEquals(32 + 480 + 20, y1 - y0 + 32) // row 1 is one photoHeight+gap below row 0
    }

    @Test
    fun `zero or negative frame count is rejected`() {
        var threw = false
        try {
            CompositeGeometry.forLayout(StripLayout.Strip, frameCount = 0)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertEquals(true, threw)
    }

    @Test
    fun `center crop keeps full height and trims width for a wider-than-cell source`() {
        // Source 1600x900 (16:9) into a 4:3 cell (400x300) - source is proportionally wider,
        // so height stays full and width gets trimmed to match the cell's aspect ratio.
        val crop = centerCropRect(sourceWidth = 1600, sourceHeight = 900, cellWidth = 400, cellHeight = 300)
        assertEquals(900, crop.height)
        assertEquals(1200, crop.width) // 900 * (400/300) = 1200
        assertEquals((1600 - 1200) / 2, crop.x)
        assertEquals(0, crop.y)
    }

    @Test
    fun `center crop keeps full width and trims height for a taller-than-cell source`() {
        // Source 900x1600 (portrait) into a 4:3 cell - proportionally taller, width stays
        // full, height gets trimmed.
        val crop = centerCropRect(sourceWidth = 900, sourceHeight = 1600, cellWidth = 400, cellHeight = 300)
        assertEquals(900, crop.width)
        assertEquals(675, crop.height) // 900 / (400/300) = 675
        assertEquals(0, crop.x)
        assertEquals((1600 - 675) / 2, crop.y)
    }

    @Test
    fun `center crop of an exact aspect-ratio match crops nothing`() {
        val crop = centerCropRect(sourceWidth = 1200, sourceHeight = 900, cellWidth = 400, cellHeight = 300)
        assertEquals(1200, crop.width)
        assertEquals(900, crop.height)
        assertEquals(0, crop.x)
        assertEquals(0, crop.y)
    }
}
