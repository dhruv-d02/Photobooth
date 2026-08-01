package com.dj.photobooth.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens for the Boothie Y2K/scrapbook system (design/handoff/README.md § Design
 * Tokens, verified against `Photobooth Rebrand.dc.html`'s actual inline styles - the README
 * summarizes, the dc.html is source of truth). The block below the core palette keeps every
 * pre-rebrand token name (Ground, TextPrimary, Accent, ...) pointed at its closest Boothie
 * equivalent, so screens a Stage 1 agent hasn't reached yet keep compiling against the same
 * names; a screen being rewritten should reference the primary Boothie names directly instead.
 */
object PhotoboothColors {
    // --- Core palette ---
    val Cream = Color(0xFFFFF7EA)
    val Ink = Color(0xFF2B1830)
    val HotPink = Color(0xFFFF4FA0)
    val HotPinkPressed = Color(0xFFC22A79)
    val Purple = Color(0xFF8A3FFC)
    val Gold = Color(0xFFFFC53D)
    val Mint = Color(0xFF5FE3C4)

    // Capture/Share dark background: CSS `linear-gradient(160deg, #3d1f5c, #1B0A2E 70%)`.
    // Brush.linearGradient's default start/end (Offset.Zero -> Offset.Infinite) paints a
    // top-left-to-bottom-right diagonal across whatever bounds it's drawn into, which reads
    // close enough to the CSS's 160deg tilt for a full-bleed background wash - an
    // angle-exact version would need a Modifier.size-aware Brush for very little visible gain.
    val DarkGradient = Brush.linearGradient(
        0f to Color(0xFF3D1F5C),
        0.7f to Color(0xFF1B0A2E),
    )

    // Capture's "frame X of 4" review badge - a lighter mint than the Mint accent above, so
    // it gets its own token rather than reusing/approximating Mint (design/handoff/README.md
    // §2, confirmed as #94F0DE in the dc.html review-state badge).
    val ReviewBadge = Color(0xFF94F0DE)

    // --- Continuity aliases (see class doc) ---
    val Ground = Cream
    val Paper = Color(0xFFFFFFFF)
    val SurfaceWash = Color(0xFFFFFDF7)
    val TextPrimary = Ink
    val TextBody = Color(0xFF5B4A63)
    val TextMuted = Color(0xFF8A7A90)
    val Accent = HotPink
    val AccentPressed = HotPinkPressed
    val AccentDeeper = Purple
    val AccentTintStrong = Mint
    val AccentTintMedium = Gold
    val AccentTintSoft = Cream
    val AccentTintFaint = Color(0xFFF2E4D0)
    val OnDarkAccent = Mint
    val OnDarkSecondaryText = Color.White.copy(alpha = 0.65f)
    val OnInkSecondaryText = Color.White.copy(alpha = 0.7f)
    val DarkSurface = Color(0xFF1B0A2E)

    val HairlineOnLight = Ink.copy(alpha = 0x1F / 255f)
    val HairlineOnDarkSubtle = Color.White.copy(alpha = 0x26 / 255f)
    val HairlineOnDark = Color.White.copy(alpha = 0x33 / 255f)
    val HairlineDashed = Ink.copy(alpha = 0x33 / 255f)
    val SurfaceWashTranslucent = SurfaceWash.copy(alpha = 0x66 / 255f)

    val FlashOverlay = Color.White.copy(alpha = 0.92f)
    val GhostBorderOnDark = Color.White.copy(alpha = 0x66 / 255f)
    val ThumbnailEmptyFill = Color.White.copy(alpha = 0x14 / 255f)
    val DisabledOnDark = Color.White.copy(alpha = 0.45f)
    val CaptionOnDark = Color.White.copy(alpha = 0.8f)
    val ProofScrimApprox = DarkSurface.copy(alpha = 0.9f)

    // Booth/Customize dotted background: "radial-gradient(circle, rgba(43,24,48,0.07) 1px,
    // transparent 1.5px)" - alpha 0.07 of Ink, on a 16dp grid pitch (drawn by whichever
    // screen composable owns the dot pattern, not this token file).
    val GridLineOnLight = Ink.copy(alpha = 0x12 / 255f)
}
