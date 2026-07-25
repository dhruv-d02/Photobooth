package com.dj.photobooth.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
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
    // 19px heading (BEGIN COUNTDOWN button, procedure-list titles): design/handoff/README.md
    // lines 85 and 91 both specify .14em, not the .12em this used to carry (copy-pasted from
    // heading18, which is a genuinely different letter-spacing value).
    val heading19 = heading(19.sp, FontWeight.SemiBold, letterSpacingEm = 0.14f)
    val heading18 = heading(18.sp, FontWeight.SemiBold, letterSpacingEm = 0.12f)
    val heading13 = heading(13.sp, FontWeight.SemiBold, letterSpacingEm = 0.14f)
    val heading12 = heading(12.sp, FontWeight.SemiBold, letterSpacingEm = 0.14f)
    // 11px heading (bottom-tab-bar labels): design/handoff/README.md line 48 specifies
    // weight 600 (SemiBold), not 500 (Medium) - this also matches the "600/700" claim in
    // the comment above rather than contradicting it as the Medium weight used to.
    val heading11 = heading(11.sp, FontWeight.SemiBold, letterSpacingEm = 0.14f)
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

    // heading()/body()/meta() are thin, category-specific wrappers (fixed family per
    // category, meta always weight Normal, only heading/body take a line-height multiplier)
    // around one shared TextStyle builder, instead of each independently reconstructing a
    // TextStyle(...) with its own ad-hoc subset of parameters - keeps the one place that
    // actually assembles a TextStyle single, while preserving the real per-category
    // differences (letterSpacingEm/lineHeightMultiplier default to "unspecified", matching
    // Compose's own TextStyle defaults, when a category doesn't set them).
    private fun heading(
        size: TextUnit,
        weight: FontWeight,
        letterSpacingEm: Float = 0.04f,
        lineHeight: Float = 1.0f,
    ) = buildStyle(HeadingFamily, weight, size, letterSpacingEm, lineHeight)

    private fun body(
        size: TextUnit,
        weight: FontWeight,
        lineHeight: Float,
    ) = buildStyle(BodyFamily, weight, size, lineHeightMultiplier = lineHeight)

    private fun meta(
        size: TextUnit,
        letterSpacingEm: Float,
    ) = buildStyle(MetaFamily, FontWeight.Normal, size, letterSpacingEm)

    private fun buildStyle(
        family: FontFamily,
        weight: FontWeight,
        size: TextUnit,
        letterSpacingEm: Float? = null,
        lineHeightMultiplier: Float? = null,
    ) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size,
        lineHeight = lineHeightMultiplier?.let { size * it } ?: TextUnit.Unspecified,
        letterSpacing = letterSpacingEm?.em ?: TextUnit.Unspecified,
    )
}
