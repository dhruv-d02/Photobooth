package com.dj.photobooth.filter

import androidx.compose.ui.graphics.ColorMatrix
import com.dj.photobooth.filter.ColorMatrixOps.then
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the CSS Filter Effects matrix math directly against the matrix coefficients
 * (no Canvas/Bitmap rendering involved - androidx.compose.ui.graphics.ColorMatrix is a plain
 * FloatArray holder, so this runs as a fast, real JVM unit test), plus the affine
 * composition helper `then()` that FilmTreatment relies on to chain multiple effects.
 */
class ColorMatrixOpsTest {

    private fun ColorMatrix.applyTo(r: Float, g: Float, b: Float, a: Float = 1f): FloatArray {
        val m = values
        return floatArrayOf(
            m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4],
            m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9],
            m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14],
            m[15] * r + m[16] * g + m[17] * b + m[18] * a + m[19],
        )
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        assertEquals(expected, actual, tolerance)
    }

    @Test
    fun `grayscale of pure red produces the red luminance coefficient on every channel`() {
        val out = ColorMatrixOps.grayscale(1f).applyTo(r = 1f, g = 0f, b = 0f)
        // Rec. 709 luma coefficients from the CSS spec: R=0.2126, G=0.7152, B=0.0722.
        assertClose(0.2126f, out[0])
        assertClose(0.2126f, out[1])
        assertClose(0.2126f, out[2])
    }

    @Test
    fun `grayscale of mid gray stays mid gray`() {
        val out = ColorMatrixOps.grayscale(1f).applyTo(r = 0.5f, g = 0.5f, b = 0.5f)
        assertClose(0.5f, out[0])
        assertClose(0.5f, out[1])
        assertClose(0.5f, out[2])
    }

    @Test
    fun `contrast of 1 is the identity transform on mid gray and full white`() {
        val gray = ColorMatrixOps.contrast(1f).applyTo(0.5f, 0.5f, 0.5f)
        assertClose(0.5f, gray[0])
        val white = ColorMatrixOps.contrast(1f).applyTo(1f, 1f, 1f)
        assertClose(1f, white[0])
    }

    @Test
    fun `contrast pushes values away from mid gray`() {
        // contrast(2): out = 2*in - 0.5. A value above mid-gray should move further from 0.5.
        val out = ColorMatrixOps.contrast(2f).applyTo(0.75f, 0.75f, 0.75f)
        assertClose(1.0f, out[0]) // 2*0.75 - 0.5 = 1.0
    }

    @Test
    fun `brightness scales every channel linearly`() {
        val out = ColorMatrixOps.brightness(1.5f).applyTo(0.4f, 0.4f, 0.4f)
        assertClose(0.6f, out[0])
    }

    @Test
    fun `saturate of 0 desaturates to the same luminance value as full grayscale`() {
        val saturateZero = ColorMatrixOps.saturate(0f).applyTo(1f, 0f, 0f)
        val grayscale = ColorMatrixOps.grayscale(1f).applyTo(1f, 0f, 0f)
        assertClose(grayscale[0], saturateZero[0])
    }

    @Test
    fun `saturate of 1 is the identity transform`() {
        val out = ColorMatrixOps.saturate(1f).applyTo(0.8f, 0.3f, 0.1f)
        assertClose(0.8f, out[0])
        assertClose(0.3f, out[1])
        assertClose(0.1f, out[2])
    }

    @Test
    fun `hue rotate of 0 degrees is the identity transform`() {
        val out = ColorMatrixOps.hueRotateDegrees(0f).applyTo(0.8f, 0.3f, 0.1f)
        assertClose(0.8f, out[0])
        assertClose(0.3f, out[1])
        assertClose(0.1f, out[2])
    }

    @Test
    fun `hue rotate of 360 degrees returns to the identity transform`() {
        val out = ColorMatrixOps.hueRotateDegrees(360f).applyTo(0.8f, 0.3f, 0.1f)
        assertClose(0.8f, out[0], tolerance = 0.01f)
        assertClose(0.3f, out[1], tolerance = 0.01f)
        assertClose(0.1f, out[2], tolerance = 0.01f)
    }

    @Test
    fun `sepia of 0 is the identity transform`() {
        val out = ColorMatrixOps.sepia(0f).applyTo(0.8f, 0.3f, 0.1f)
        assertClose(0.8f, out[0])
        assertClose(0.3f, out[1])
        assertClose(0.1f, out[2])
    }

    @Test
    fun `then composes two scale-only matrices by multiplying their factors`() {
        // brightness(2) then brightness(3) on a channel should scale by 2*3=6 total.
        val combined = ColorMatrixOps.brightness(2f).then(ColorMatrixOps.brightness(3f))
        val out = combined.applyTo(0.1f, 0.1f, 0.1f)
        assertClose(0.6f, out[0])
    }

    @Test
    fun `then composes offsets correctly - contrast then brightness`() {
        // Apply contrast(2) first (0.75 -> 1.0 per the earlier test), then brightness(0.5)
        // on that result (1.0 -> 0.5).
        val combined = ColorMatrixOps.contrast(2f).then(ColorMatrixOps.brightness(0.5f))
        val out = combined.applyTo(0.75f, 0.75f, 0.75f)
        assertClose(0.5f, out[0])
    }

    @Test
    fun `FilmTreatment None is the identity matrix`() {
        val out = FilmTreatment.None.colorMatrix.applyTo(0.37f, 0.62f, 0.81f)
        assertClose(0.37f, out[0])
        assertClose(0.62f, out[1])
        assertClose(0.81f, out[2])
    }

    @Test
    fun `FilmTreatment Disposable matches the documented saturate-contrast-brightness chain`() {
        // Gray/equal-channel inputs are invariant under saturate and hue-rotate (both matrices'
        // rows sum to 1 for any amount/angle), so a fully-desaturated white (1,1,1) isolates
        // just the contrast(1.15) then brightness(1.05) steps: 1.15*1 - 0.075 = 1.075, *1.05.
        val out = FilmTreatment.Disposable.colorMatrix.applyTo(1f, 1f, 1f)
        assertClose(1.12875f, out[0])
    }

    @Test
    fun `FilmTreatment Cyber's saturate and hue-rotate leave a desaturated white unchanged before contrast`() {
        // Same row-sums-to-1 property isolates Cyber's trailing contrast(1.1) step:
        // 1.1*1 - 0.05 = 1.05.
        val out = FilmTreatment.Cyber.colorMatrix.applyTo(1f, 1f, 1f)
        assertClose(1.05f, out[0])
    }

    @Test
    fun `none of the 5 Boothie treatments carry a duotone overlay`() {
        FilmTreatment.entries.forEach { treatment ->
            assertEquals(null, treatment.duotoneOverlay, "${treatment.name} should have no duotone overlay")
        }
    }
}
