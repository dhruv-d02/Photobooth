package com.dj.photobooth.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens ported verbatim from design/handoff/README.md § Design Tokens.
 * Do not rename or recompute these — they are pixel-faithful to the design handoff.
 */
object PhotoboothColors {
    val Ground = Color(0xFFF2F2F3)
    val SurfaceWash = Color(0xFFE9E9EA)
    val Paper = Color(0xFFF5F5F8)

    val TextPrimary = Color(0xFF1D1F20)
    val TextBody = Color(0xFF424244)
    val TextMuted = Color(0xFF5D5D60)

    val Accent = Color(0xFF5980A6)
    val AccentPressed = Color(0xFF416180)
    val AccentDeeper = Color(0xFF2C455D)

    val AccentTintStrong = Color(0xFFB5D9FD)
    val AccentTintMedium = Color(0xFFD6EBFF)
    val AccentTintSoft = Color(0xFFEEF6FF)
    val AccentTintFaint = Color(0xFFDFE7F0)

    val OnDarkAccent = Color(0xFF94BCE3)
    val OnDarkSecondaryText = Color(0xFF9EBBD8)
    val OnInkSecondaryText = Color(0xFF98989B)

    val DarkSurface = Color(0xFF1D2D3D)
    val Ink = TextPrimary

    val HairlineOnLight = TextPrimary.copy(alpha = 0x29 / 255f)
    val HairlineOnDarkSubtle = Paper.copy(alpha = 0x26 / 255f)
    val HairlineOnDark = Paper.copy(alpha = 0x33 / 255f)
    val SurfaceWashTranslucent = SurfaceWash.copy(alpha = 0x66 / 255f)
}

/** Frame-color presets — the strip/grid background choices on the Preview screen. */
enum class FrameColorPreset(
    val background: Color,
    val text: Color,
    val dim: Color,
) {
    Paper(PhotoboothColors.Paper, PhotoboothColors.TextPrimary, PhotoboothColors.TextMuted),
    Steel(PhotoboothColors.AccentDeeper, PhotoboothColors.Paper, PhotoboothColors.AccentTintStrong),
    Ink(PhotoboothColors.Ink, PhotoboothColors.Paper, PhotoboothColors.OnInkSecondaryText),
    Sky(PhotoboothColors.AccentTintMedium, PhotoboothColors.DarkSurface, PhotoboothColors.AccentPressed),
}