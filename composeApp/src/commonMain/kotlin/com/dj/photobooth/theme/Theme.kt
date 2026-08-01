package com.dj.photobooth.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Boothie's rounded scale (design/handoff/README.md § Screens, radii read off the dc.html's
// inline `border-radius` values - 10-20px depending on element). Pill shapes (buttons, chips,
// tab bar segments) are NOT part of this scale - they use `RoundedCornerShape(50)` directly at
// the call site, since a percent-based pill corner isn't expressible as one of Shapes' fixed
// dp corner sizes.
private val BoothieShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), // thumbnail cells, per-cell strip previews
    small = RoundedCornerShape(12.dp), // gallery cards, frame swatch chips
    medium = RoundedCornerShape(16.dp), // Customize strip/grid mount
    large = RoundedCornerShape(18.dp), // Share strip mount
    extraLarge = RoundedCornerShape(20.dp), // Capture viewfinder card
)

// Compose Multiplatform components (Material3 buttons, text fields, etc.) still expect a
// MaterialTheme color scheme even though this app's visual language is mostly custom-drawn
// rather than stock Material. We map our design tokens onto the closest M3 slots so any
// Material component we do use (e.g. ripple, focus indication) picks up the right base
// colors instead of Material's default purple.
private val LightScheme = lightColorScheme(
    background = PhotoboothColors.Cream,
    surface = PhotoboothColors.Paper,
    primary = PhotoboothColors.HotPink,
    onPrimary = PhotoboothColors.Cream,
    onBackground = PhotoboothColors.Ink,
    onSurface = PhotoboothColors.Ink,
)

@Composable
fun PhotoboothTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        shapes = BoothieShapes,
        content = content,
    )
}
