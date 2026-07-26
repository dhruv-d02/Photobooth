package com.dj.photobooth.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothType

/**
 * Bottom tab bar per design/handoff/README.md § Bottom tab bar - present on every screen
 * except Capture (a session is modal; see [PhotoboothNavHost] for the route-based
 * show/hide logic).
 *
 * [stripCount] is threaded through as a plain `Int` (default/placeholder `0` at the call
 * site in [PhotoboothNavHost]) rather than read from a real data source here - the
 * history/gallery data layer (Room) is being built on the parallel `feature/export-history`
 * branch and doesn't exist in this worktree yet. Wiring the real count is a later
 * integration step once that branch lands; this parameter is the seam for it.
 */
@Composable
fun BottomTabBar(
    currentRoute: String?,
    stripCount: Int,
    onTabSelected: (Route) -> Unit,
) {
    val hairlineWidth = 1.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(PhotoboothColors.Paper)
            // 1px top border - drawn rather than Modifier.border() so it applies to only
            // the top edge, per the spec ("1px top border", not a full outline).
            .drawBehind {
                drawLine(
                    color = PhotoboothColors.HairlineOnLight,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = hairlineWidth.toPx(),
                )
            },
    ) {
        BottomTab.entries.forEachIndexed { index, tab ->
            if (index > 0) {
                // 1px divider between cells (not on the bar's outer edges).
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(hairlineWidth)
                        .background(PhotoboothColors.HairlineOnLight),
                )
            }
            TabCell(
                tab = tab,
                selected = currentRoute == tab.route.route,
                stripCount = stripCount,
                onClick = { onTabSelected(tab.route) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun TabCell(
    tab: BottomTab,
    selected: Boolean,
    stripCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Shoot is always solid-accent regardless of selection - design/handoff/README.md line
    // 46: "Shoot (glyph ●) → starts a session. Always solid accent #5980a6 with #f5f5f8
    // label" - it's an action cell, not a normal selected/unselected toggle like Booth/Strips.
    val isAlwaysAccent = tab == BottomTab.Shoot
    val background = when {
        isAlwaysAccent -> PhotoboothColors.Accent
        selected -> PhotoboothColors.AccentTintSoft
        else -> Color.Transparent
    }
    val foreground = when {
        isAlwaysAccent -> PhotoboothColors.Paper
        selected -> PhotoboothColors.AccentDeeper
        else -> PhotoboothColors.TextMuted
    }
    val label = if (tab == BottomTab.Strips) {
        // "Strips NN (glyph ▤) → gallery, with the zero-padded strip count in the label."
        "${tab.label} ${stripCount.toString().padStart(2, '0')}"
    } else {
        tab.label
    }

    Column(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tab.glyph,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = foreground,
        )
        // design/handoff/README.md line 48: glyph and label stack "above/below ... 4px gap".
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = PhotoboothType.heading11,
            color = foreground,
        )
    }
}
