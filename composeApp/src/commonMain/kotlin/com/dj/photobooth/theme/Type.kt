package com.dj.photobooth.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dj.photobooth.generated.resources.Res
import com.dj.photobooth.generated.resources.caveat_bold
import com.dj.photobooth.generated.resources.caveat_semibold
import com.dj.photobooth.generated.resources.fredoka_bold
import com.dj.photobooth.generated.resources.fredoka_medium
import com.dj.photobooth.generated.resources.fredoka_semibold
import com.dj.photobooth.generated.resources.nunito_bold
import com.dj.photobooth.generated.resources.nunito_extrabold
import com.dj.photobooth.generated.resources.nunito_regular
import com.dj.photobooth.generated.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

/**
 * The three Boothie type families (design/handoff/README.md § Design Tokens > Typography,
 * confirmed against `Photobooth Rebrand.dc.html`'s Google Fonts link: Fredoka 500/600/700,
 * Nunito 400/600/700/800, Caveat 600/700). Every named style below is a @Composable function,
 * not a plain val, because building a [FontFamily] from [Font] resources needs a composition
 * (it resolves through Compose Multiplatform's resource-reading machinery) - a call site reads
 * `PhotoboothType.display36()` rather than the old plain-property `PhotoboothType.display64`.
 */
object PhotoboothType {
    @Composable
    private fun fredokaFamily(): FontFamily = FontFamily(
        Font(Res.font.fredoka_medium, FontWeight.Medium),
        Font(Res.font.fredoka_semibold, FontWeight.SemiBold),
        Font(Res.font.fredoka_bold, FontWeight.Bold),
    )

    @Composable
    private fun nunitoFamily(): FontFamily = FontFamily(
        Font(Res.font.nunito_regular, FontWeight.Normal),
        Font(Res.font.nunito_semibold, FontWeight.SemiBold),
        Font(Res.font.nunito_bold, FontWeight.Bold),
        Font(Res.font.nunito_extrabold, FontWeight.ExtraBold),
    )

    @Composable
    private fun caveatFamily(): FontFamily = FontFamily(
        Font(Res.font.caveat_semibold, FontWeight.SemiBold),
        Font(Res.font.caveat_bold, FontWeight.Bold),
    )

    // The Capture screen's countdown digit (design/handoff/README.md §2 / dc.html line 193):
    // Fredoka 700, 130px, bounce-in keyframe applied by the caller (Animatable, not here).
    @Composable
    fun countdownDisplay(): TextStyle = display(130.sp, FontWeight.Bold)

    // Display (Fredoka): headlines, buttons, pills, badges, the app wordmark. Sizes below are
    // the recurring anchor points across the 5 screens - a call site whose exact figure falls
    // between two of these (e.g. the Booth CTA's 16.5px, or Capture's 14.5px pill labels)
    // should `.copy(fontSize = ...)` off the nearest one, the same convention the Industry-era
    // Type.kt used (see LandingScreen's meta10.copy(letterSpacing = ...) for precedent).
    @Composable
    fun display36(): TextStyle = display(36.sp, FontWeight.Bold, lineHeight = 1.03f) // Booth headline
    @Composable
    fun display26(): TextStyle = display(26.sp, FontWeight.Bold) // Share/Gallery headline
    @Composable
    fun display24(): TextStyle = display(24.sp, FontWeight.Bold) // Customize headline
    @Composable
    fun display17(): TextStyle = display(17.sp, FontWeight.Bold) // wordmark, empty-state title
    @Composable
    fun display15(): TextStyle = display(15.sp, FontWeight.Bold) // Booth wordmark
    @Composable
    fun display13(): TextStyle = display(13.sp, FontWeight.Bold) // pills/buttons/segmented control
    @Composable
    fun display11(): TextStyle = display(11.sp, FontWeight.Bold) // review badge, footer brand
    @Composable
    fun display9(): TextStyle = display(9.sp, FontWeight.Bold) // thumbnail cell numbers

    // Section labels (flicket / frame / layout row headers): Fredoka 600, uppercase applied
    // at the call site (this project's established convention - see Type.kt's own history).
    @Composable
    fun sectionLabel(): TextStyle = display(11.5.sp, FontWeight.SemiBold, letterSpacingEm = 0.04f)

    // Body (Nunito): paragraph copy, helper text, chip/swatch labels, the bottom tab bar.
    @Composable
    fun bodyRegular(): TextStyle = body(14.sp, FontWeight.Normal, lineHeight = 1.55f)
    @Composable
    fun bodySmall(): TextStyle = body(13.sp, FontWeight.Normal, lineHeight = 1.5f)
    @Composable
    fun bodyCaption(): TextStyle = body(11.sp, FontWeight.Normal, lineHeight = 1.4f) // idle hint, log line, helper text
    @Composable
    fun bodyBold12(): TextStyle = body(12.sp, FontWeight.Bold) // flicket chip / frame swatch labels
    @Composable
    fun bodyBold11(): TextStyle = body(11.sp, FontWeight.Bold) // exit button, "make another strip" link

    // Bottom tab bar labels: Nunito 700 9.5px, `.02em` letter-spacing, uppercase applied at the
    // call site (correction: the prototype's own strings are lowercase, textTransform does the
    // rest - see BottomTabBar.kt's TabCell).
    @Composable
    fun tabLabel(): TextStyle = body(9.5.sp, FontWeight.Bold, letterSpacingEm = 0.02f)

    // Stamp (Caveat): handwritten taglines and date stamps.
    @Composable
    fun stamp13(): TextStyle = stamp(13.sp, FontWeight.Bold)
    @Composable
    fun stamp17(): TextStyle = stamp(17.sp, FontWeight.Bold)

    @Composable
    private fun display(
        size: TextUnit,
        weight: FontWeight,
        letterSpacingEm: Float? = null,
        lineHeight: Float? = null,
    ) = buildStyle(fredokaFamily(), weight, size, letterSpacingEm, lineHeight)

    @Composable
    private fun body(
        size: TextUnit,
        weight: FontWeight,
        letterSpacingEm: Float? = null,
        lineHeight: Float? = null,
    ) = buildStyle(nunitoFamily(), weight, size, letterSpacingEm, lineHeight)

    @Composable
    private fun stamp(
        size: TextUnit,
        weight: FontWeight,
    ) = buildStyle(caveatFamily(), weight, size)

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
