package com.dj.photobooth.filter

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun solidFrame(width: Int = 400, height: Int = 300): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    Canvas(bitmap).drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint())
    return bitmap
}

// This test needs real Bitmap/Canvas rendering (StripCompositor.compose() draws through
// androidx.compose.ui.graphics onto an actual ImageBitmap), which the plain Android unit-test
// stub jar can't provide - "Method createBitmap in android.graphics.Bitmap not mocked" is
// what actually happens without Robolectric. It's in androidUnitTest (not commonTest)
// specifically because Robolectric can't compile for the iOS targets.
// SDK 33, not this project's compileSdk 37: Robolectric's SDK 34+ shadow jars require
// Java 17 to execute, but this module's jvmToolchain(11) targets Java 11 - Bitmap/Canvas
// pixel behavior is stable across these API levels, so this doesn't affect what's verified.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StripCompositorTest {

    @Test
    fun `strip layout output dimensions match the documented formula`() {
        val frames = List(4) { solidFrame() }
        val output = StripCompositor.compose(frames, FilmTreatment.None, FrameColorPreset.Butter, StripLayout.Strip)

        // Design units doubled: padding 32, photoW 640, photoH 480, gap 20, footer 60.
        val expectedWidth = 2 * 32 + 640
        val expectedHeight = 2 * 32 + 4 * 480 + 3 * 20 + 60
        assertEquals(expectedWidth, output.width)
        assertEquals(expectedHeight, output.height)
    }

    @Test
    fun `grid layout output dimensions match the documented formula`() {
        val frames = List(4) { solidFrame() }
        val output = StripCompositor.compose(frames, FilmTreatment.None, FrameColorPreset.Butter, StripLayout.Grid)

        // 2 columns, photoW/H 310, gap 20, padding 32, footer 60; 4 frames -> 2 rows.
        val expectedWidth = 2 * 32 + 640
        val expectedHeight = 2 * 32 + 2 * 310 + 1 * 20 + 60
        assertEquals(expectedWidth, output.width)
        assertEquals(expectedHeight, output.height)
    }

    @Test
    fun `grid layout rounds up to a partial final row`() {
        // 3 frames in a 2-column grid must still allocate 2 rows (ceil(3/2) = 2), not 1.
        val frames = List(3) { solidFrame() }
        val output = StripCompositor.compose(frames, FilmTreatment.None, FrameColorPreset.Butter, StripLayout.Grid)

        val expectedHeight = 2 * 32 + 2 * 310 + 1 * 20 + 60
        assertEquals(expectedHeight, output.height)
    }

    @Test
    fun `background fills with the chosen frame color outside the photo area`() {
        val frames = List(2) { solidFrame() }
        val output = StripCompositor.compose(frames, FilmTreatment.None, FrameColorPreset.Grape, StripLayout.Strip)

        // The top-left corner (inside the outer padding, above/left of any photo) must be
        // the frame color's background, not left transparent/black.
        val pixelMap = output.toPixelMap()
        val topLeft = pixelMap[2, 2]
        assertClose(FrameColorPreset.Grape.background.red, topLeft.red)
        assertClose(FrameColorPreset.Grape.background.green, topLeft.green)
        assertClose(FrameColorPreset.Grape.background.blue, topLeft.blue)
    }

    @Test
    fun `single frame count still produces a valid single-cell composite`() {
        val output = StripCompositor.compose(listOf(solidFrame()), FilmTreatment.None, FrameColorPreset.Butter, StripLayout.Strip)
        val expectedHeight = 2 * 32 + 1 * 480 + 0 * 20 + 60
        assertEquals(expectedHeight, output.height)
    }

    @Test
    fun `composing an empty frame list is rejected rather than producing a bogus image`() {
        var threw = false
        try {
            StripCompositor.compose(emptyList(), FilmTreatment.None, FrameColorPreset.Butter, StripLayout.Strip)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "compose() with no frames should throw, not silently produce a degenerate image")
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.02f) {
        assertTrue(kotlin.math.abs(expected - actual) <= tolerance, "expected $expected, got $actual")
    }
}
