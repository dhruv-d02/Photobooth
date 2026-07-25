package com.dj.photobooth.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The design uses a literal 0.85x-density spacing scale instead of the usual 4/8px grid
 * (design/handoff/README.md § Design Tokens). We port the exact steps here rather than
 * rounding to a standard grid, because the handoff's padding/gap values throughout every
 * screen spec are these specific numbers ("13.6px gap", "20.4px padding", etc.) - using a
 * different scale would drift from pixel-faithful.
 */
object PhotoboothSpacing {
    val xxs: Dp = 3.4.dp
    val xs: Dp = 5.dp
    val sm: Dp = 6.8.dp
    val smMedium: Dp = 8.dp
    val md: Dp = 10.2.dp
    val mdLarge: Dp = 13.6.dp
    val lg: Dp = 17.dp
    val lgLarge: Dp = 20.4.dp
    val xl: Dp = 24.dp
    val xlLarge: Dp = 27.2.dp
    val xxl: Dp = 34.dp
    val hitTarget: Dp = 44.dp
    val xxxl: Dp = 54.4.dp

    /** The faint background grid pitch used on the landing screen (1a Spec sheet). */
    val gridPitch: Dp = 27.2.dp
}