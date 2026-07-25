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

    // Capture-screen-specific alpha variants (design/handoff/README.md § 2) - named here so
    // they follow the same token pattern as the hairlines above instead of being scattered
    // as inline .copy(alpha = 0.xf) literals across CaptureScreen.kt.
    val FlashOverlay = Paper.copy(alpha = 0.92f) // "Flash: full-cover #f5f5f8 at opacity .92"
    val GhostBorderOnDark = Paper.copy(alpha = 0x66 / 255f) // SHOOT AGAIN border: "1px #f5f5f866"
    val ThumbnailEmptyFill = Paper.copy(alpha = 0x14 / 255f) // empty thumbnail cell: "#f5f5f814"
    val DisabledOnDark = Paper.copy(alpha = 0.45f) // "opacity .45 when disabled"
    val CaptionOnDark = Paper.copy(alpha = 0.8f) // viewfinder corner captions - not a doc-specified exact value, a readable default
    val ProofScrimApprox = DarkSurface.copy(alpha = 0.9f) // flat-alpha stand-in for the doc's linear-gradient(#1d2d3de6, #1d2d3d00) scrim - TODO(phase-2): real gradient once a Brush-based header is worth the complexity
}

// Frame-color presets (the strip/grid background choices on the Preview screen) are Phase 2
// work per CLAUDE.md's build sequence, not Phase 0 scaffolding - deliberately not defined
// here yet. Add a FrameColorPreset enum (Paper/Steel/Ink/Sky, per design/handoff/README.md's
// Design Tokens § Frame options) when the Preview screen is actually built, using the color
// tokens above.