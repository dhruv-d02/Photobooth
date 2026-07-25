package com.dj.photobooth.filter

import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.cos
import kotlin.math.sin

/**
 * The CSS Filter Effects matrices (grayscale/sepia/saturate/hue-rotate/brightness/contrast),
 * transcribed from the W3C spec, operating in Compose's 0..1 normalized color space (not
 * Android View's legacy 0..255 android.graphics.ColorMatrix convention). These are the
 * building blocks film treatments compose together to match design/handoff/README.md's
 * "Film treatments (CSS filter equivalents)" section exactly.
 *
 * ColorMatrix stores a 4x5 row-major array: values[0..4] = R' row, [5..9] = G' row,
 * [10..14] = B' row, [15..19] = A' row, each row being [rCoef, gCoef, bCoef, aCoef, offset].
 */
internal object ColorMatrixOps {

    fun grayscale(amount: Float): ColorMatrix {
        val a = 1 - amount
        return ColorMatrix(
            floatArrayOf(
                0.2126f + 0.7874f * a, 0.7152f - 0.7152f * a, 0.0722f - 0.0722f * a, 0f, 0f,
                0.2126f - 0.2126f * a, 0.7152f + 0.2848f * a, 0.0722f - 0.0722f * a, 0f, 0f,
                0.2126f - 0.2126f * a, 0.7152f - 0.7152f * a, 0.0722f + 0.9278f * a, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    fun sepia(amount: Float): ColorMatrix {
        val a = 1 - amount
        return ColorMatrix(
            floatArrayOf(
                0.393f + 0.607f * a, 0.769f - 0.769f * a, 0.189f - 0.189f * a, 0f, 0f,
                0.349f - 0.349f * a, 0.686f + 0.314f * a, 0.168f - 0.168f * a, 0f, 0f,
                0.272f - 0.272f * a, 0.534f - 0.534f * a, 0.131f + 0.869f * a, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    fun saturate(amount: Float): ColorMatrix {
        val s = amount
        return ColorMatrix(
            floatArrayOf(
                0.213f + 0.787f * s, 0.715f - 0.715f * s, 0.072f - 0.072f * s, 0f, 0f,
                0.213f - 0.213f * s, 0.715f + 0.285f * s, 0.072f - 0.072f * s, 0f, 0f,
                0.213f - 0.213f * s, 0.715f - 0.715f * s, 0.072f + 0.928f * s, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    fun hueRotateDegrees(degrees: Float): ColorMatrix {
        val rad = degrees * (kotlin.math.PI.toFloat() / 180f)
        val c = cos(rad)
        val s = sin(rad)
        return ColorMatrix(
            floatArrayOf(
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    fun brightness(amount: Float): ColorMatrix = ColorMatrix(
        floatArrayOf(
            amount, 0f, 0f, 0f, 0f,
            0f, amount, 0f, 0f, 0f,
            0f, 0f, amount, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    )

    fun contrast(amount: Float): ColorMatrix {
        val offset = 0.5f * (1 - amount)
        return ColorMatrix(
            floatArrayOf(
                amount, 0f, 0f, 0f, offset,
                0f, amount, 0f, 0f, offset,
                0f, 0f, amount, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    /**
     * Composes [this] applied first, then [next] applied to its result - matching CSS's
     * `filter: a() b()` semantics (a applied first, b operates on a's output). Compose's
     * ColorMatrix has no public multiply/concat operator, so this reimplements 4x5 affine
     * composition directly: for affine transforms output = M*input + t, applying M1/t1 after
     * M2/t2 gives combined linear part M1*M2 and combined offset M1*t2 + t1.
     */
    fun ColorMatrix.then(next: ColorMatrix): ColorMatrix {
        val a = this.values // applied first
        val b = next.values // applied second
        val result = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += b[row * 5 + k] * a[k * 5 + col]
                }
                result[row * 5 + col] = sum
            }
            // Combined offset: b's linear part applied to a's offset column, plus b's own offset.
            var offset = b[row * 5 + 4]
            for (k in 0 until 4) {
                offset += b[row * 5 + k] * a[k * 5 + 4]
            }
            result[row * 5 + 4] = offset
        }
        return ColorMatrix(result)
    }
}
