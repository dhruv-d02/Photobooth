package com.dj.photobooth.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The design is "radius 0 everywhere" (design/handoff/README.md § Design Tokens) - square
// corners on every card, button, and chip, no exceptions. Defining one shared Shapes
// instance with every corner size at 0.dp means new components get square corners for
// free from MaterialTheme.shapes, instead of every call site needing to remember to
// override the default rounded corners individually.
private val SquareShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

// Compose Multiplatform components (Material3 buttons, text fields, etc.) still expect a
// MaterialTheme color scheme even though this app's visual language is mostly custom-drawn
// rather than stock Material. We map our design tokens onto the closest M3 slots so any
// Material component we do use (e.g. ripple, focus indication) picks up the right base
// colors instead of Material's default purple.
private val LightScheme = lightColorScheme(
    background = PhotoboothColors.Ground,
    surface = PhotoboothColors.Paper,
    primary = PhotoboothColors.Accent,
    onPrimary = PhotoboothColors.Paper,
    onBackground = PhotoboothColors.TextPrimary,
    onSurface = PhotoboothColors.TextPrimary,
)

// Just one scheme for now: Phase 0 has a single (light) placeholder screen, so a dark
// "steel" scheme would be unexercised, unverified code. The design does call for a dark
// surface on the Capture screen (design/handoff/README.md § 2) - add a DarkScheme and a
// darkSurface toggle back here in whichever phase actually builds that screen, so it ships
// tested against real content instead of speculative.
@Composable
fun PhotoboothTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        shapes = SquareShapes,
        content = content,
    )
}
