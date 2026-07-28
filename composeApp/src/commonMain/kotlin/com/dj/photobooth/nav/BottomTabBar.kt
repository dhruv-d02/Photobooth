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
import androidx.compose.foundation.layout.navigationBarsPadding
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
 * [stripCount] is threaded through as a plain `Int` rather than read from a data source
 * directly here - [PhotoboothNavHost] collects the live, uncapped count from `GalleryRepo`
 * and passes it down, keeping this composable itself free of a repo dependency.
 */
@Composable
fun BottomTabBar(
    currentRoute: String?,
    stripCount: Int,
    onTabSelected: (Route) -> Unit,
) {
    val hairlineWidth = 1.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Background first, inset padding second, so the bar's paper fill extends *behind*
            // the system gesture bar / nav buttons (edge-to-edge is on, see MainActivity) while
            // the tappable cells stay above them. Without this the whole 56dp strip is drawn
            // under the system bar and its lower half is unreachable.
            .background(PhotoboothColors.Paper)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // A definite height, not heightIn(min = ...): inside Scaffold's bottomBar slot
                // this Row receives a loose-but-large maxHeight constraint. Without a bound of
                // its own, the cells' and divider's Modifier.fillMaxHeight() below fill *that*
                // incoming constraint (effectively the whole screen) rather than the bar's own
                // resolved size - that's the "tabs are full screen" bug. Content here (a glyph
                // + one label line) never needs more than 56px, so a fixed height both
                // satisfies the spec's "min-height 56px" and bounds fillMaxHeight() sanely.
                .height(56.dp)
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
