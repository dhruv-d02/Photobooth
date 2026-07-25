package com.dj.photobooth.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// TODO(design-fonts): the handoff specifies Barlow Condensed (headings) + Barlow (body) +
// a monospace face (metadata), all Google Fonts (OFL-licensed). We're using platform
// default/monospace families as a placeholder so Phase 0 has a working, buildable theme
// without pulling in binary font assets yet. Swap FontFamily.Default -> Barlow and
// FontFamily.SansSerif -> BarlowCondensed once the font files are added as Compose
// Multiplatform font resources (composeResources/font/) - the sizes/weights/letter-spacing
// below already match design/handoff/README.md § Type exactly, only the face is a stand-in.
private val HeadingFamily = FontFamily.Default
private val BodyFamily = FontFamily.Default
private val MetaFamily = FontFamily.Monospace

/**
 * Every named TextStyle the design calls for, grouped the way the handoff groups them
 * (Headings / Body / Metadata) so a screen implementation can pick the exact style named
 * in design/handoff/README.md rather than reinventing sizes ad hoc.
 */
object PhotoboothType {
    // Headings: Barlow Condensed 600/700, uppercase, tight/negative letter-spacing on the
    // largest display sizes. Callers apply `.uppercase()` to the string, not a style flag,
    // since Compose has no built-in uppercase text transform.
    val display64 = heading(64.sp, FontWeight.Bold, letterSpacingEm = -0.01f, lineHeight = 0.86f)
    val display52 = heading(52.sp, FontWeight.Bold, letterSpacingEm = -0.01f, lineHeight = 0.9f)
    val display42 = heading(42.sp, FontWeight.Bold, lineHeight = 1.0f)
    val heading36 = heading(36.sp, FontWeight.Bold, lineHeight = 1.05f)
    val heading19 = heading(19.sp, FontWeight.SemiBold, letterSpacingEm = 0.12f)
    val heading18 = heading(18.sp, FontWeight.SemiBold, letterSpacingEm = 0.12f)
    val heading13 = heading(13.sp, FontWeight.SemiBold, letterSpacingEm = 0.14f)
    val heading12 = heading(12.sp, FontWeight.SemiBold, letterSpacingEm = 0.14f)
    val heading11 = heading(11.sp, FontWeight.Medium, letterSpacingEm = 0.14f)
    val heading10 = heading(10.sp, FontWeight.SemiBold, letterSpacingEm = 0.16f)

    // Body: Barlow 300-500, sentence case, generous line-height for readability.
    val body15 = body(15.sp, FontWeight.Normal, lineHeight = 1.5f)
    val body14 = body(14.sp, FontWeight.Normal, lineHeight = 1.45f)
    val body11 = body(11.sp, FontWeight.Normal, lineHeight = 1.35f)

    // Metadata: monospace, uppercase, wide letter-spacing - used for kickers, stamps,
    // status text, and the corner-tick labels throughout every screen.
    val meta11 = meta(11.sp, letterSpacingEm = 0.10f)
    val meta10 = meta(10.sp, letterSpacingEm = 0.14f)
    val meta9 = meta(9.sp, letterSpacingEm = 0.16f)
    val meta8 = meta(8.sp, letterSpacingEm = 0.20f)

    private fun heading(
        size: androidx.compose.ui.unit.TextUnit,
        weight: FontWeight,
        letterSpacingEm: Float = 0.04f,
        lineHeight: Float = 1.0f,
    ) = TextStyle(
        fontFamily = HeadingFamily,
        fontWeight = weight,
        fontSize = size,
        lineHeight = size * lineHeight,
        letterSpacing = letterSpacingEm.em,
    )

    private fun body(
        size: androidx.compose.ui.unit.TextUnit,
        weight: FontWeight,
        lineHeight: Float,
    ) = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = weight,
        fontSize = size,
        lineHeight = size * lineHeight,
    )

    private fun meta(
        size: androidx.compose.ui.unit.TextUnit,
        letterSpacingEm: Float,
    ) = TextStyle(
        fontFamily = MetaFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size,
        letterSpacing = letterSpacingEm.em,
    )
}