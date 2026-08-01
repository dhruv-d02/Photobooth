package com.dj.photobooth.nav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothType

/**
 * Bottom tab bar - design_handoff_photobooth_rebrand/README.md's "Bottom tab bar" mentions
 * (Screens 1/3) and `Photobooth Rebrand.dc.html`'s repeated `tabBtnStyle`/tab markup (identical
 * across the booth/customize/gallery blocks, e.g. lines ~176-180 and ~375-379). Present on every
 * screen except Capture (a session is modal; see [PhotoboothNavHost] for the route-based
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
    val hairlineWidth = 2.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Background first, inset padding second, so the bar's fill extends *behind* the
            // system gesture bar / nav buttons (edge-to-edge is on, see MainActivity) while the
            // tappable cells stay above them. Without this the whole 56dp strip is drawn under
            // the system bar and its lower half is unreachable.
            .background(PhotoboothColors.SurfaceWash)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // A definite height, not heightIn(min = ...): inside Scaffold's bottomBar slot
                // this Row receives a loose-but-large maxHeight constraint. Without a bound of
                // its own, the cells' fillMaxHeight() below fills *that* incoming constraint
                // (effectively the whole screen) rather than the bar's own resolved size -
                // that's the "tabs are full screen" bug. Content here (a glyph + one label
                // line) never needs more than 56px, so a fixed height both satisfies the
                // spec's "min-height 56px" and bounds fillMaxHeight() sanely.
                .height(56.dp)
                // dc.html: "border-top:2px solid rgba(43,24,48,0.08)" - drawn rather than
                // Modifier.border() so it applies to only the top edge.
                .drawBehind {
                    drawLine(
                        color = PhotoboothColors.HairlineOnLight,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = hairlineWidth.toPx(),
                    )
                },
        ) {
            BottomTab.entries.forEach { tab ->
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
    // Shoot is always solid hot-pink regardless of selection - it's an action cell (always
    // starts a session), not a normal selected/unselected toggle like Booth/Strips, matching
    // this project's established "Shoot is always solid-accent" convention (previously
    // steel-blue #5980a6, now Boothie's hot pink).
    val isAlwaysAccent = tab == BottomTab.Shoot
    val background = if (isAlwaysAccent) PhotoboothColors.HotPink else Color.Transparent
    val foreground = when {
        isAlwaysAccent -> PhotoboothColors.Cream
        selected -> PhotoboothColors.HotPink
        else -> PhotoboothColors.TextMuted
    }
    // Type.kt convention: callers apply .uppercase() at the Text() call site, not a style flag -
    // tab.label is normalized to lowercase first (rather than relied on as already-lowercase)
    // because Route.kt's BottomTab enum - out of scope for this task, owned by a sibling agent
    // wiring the Share screen - still stores the pre-rebrand uppercase display strings.
    val labelText = if (tab == BottomTab.Strips) {
        // dc.html line 179/378: `strips ({{ galleryCount }})`.
        "${tab.label.lowercase()} (${stripCount})"
    } else {
        tab.label.lowercase()
    }

    Column(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TabGlyph(tab = tab, tint = foreground)
        // dc.html tabBtnStyleBase: "gap:3".
        Spacer(Modifier.height(3.dp))
        Text(
            text = labelText.uppercase(),
            style = PhotoboothType.tabLabel(),
            color = foreground,
        )
    }
}

// Outline vector glyphs confirmed against dc.html's inline SVGs (lines ~177-179 / ~376-378),
// all in a 24x24 viewBox at stroke-width 2.2 - approximated with Canvas draw calls scaled to
// the 19dp box the SVGs themselves use, rather than a pixel-exact vector port (not needed for
// visual equivalence at this size).
@Composable
private fun TabGlyph(tab: BottomTab, tint: Color) {
    Canvas(modifier = Modifier.size(19.dp)) {
        val scale = size.width / 24f
        val strokeWidthPx = 2.2f * scale
        when (tab) {
            BottomTab.Booth -> {
                // <rect x="3" y="4" width="18" height="16" rx="4"/><circle cx="12" cy="12" r="3"/>
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(3f * scale, 4f * scale),
                    size = Size(18f * scale, 16f * scale),
                    cornerRadius = CornerRadius(4f * scale, 4f * scale),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawCircle(
                    color = tint,
                    radius = 3f * scale,
                    center = Offset(12f * scale, 12f * scale),
                    style = Stroke(width = strokeWidthPx),
                )
            }
            BottomTab.Shoot -> {
                // <circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3.4" fill="currentColor"/>
                drawCircle(
                    color = tint,
                    radius = 9f * scale,
                    center = Offset(12f * scale, 12f * scale),
                    style = Stroke(width = strokeWidthPx),
                )
                drawCircle(color = tint, radius = 3.4f * scale, center = Offset(12f * scale, 12f * scale))
            }
            BottomTab.Strips -> {
                // Four <rect width="8" height="8" rx="2"/> at (3,3)/(13,3)/(3,13)/(13,13).
                val cellSize = Size(8f * scale, 8f * scale)
                val corner = CornerRadius(2f * scale, 2f * scale)
                val stroke = Stroke(width = strokeWidthPx)
                listOf(
                    Offset(3f * scale, 3f * scale),
                    Offset(13f * scale, 3f * scale),
                    Offset(3f * scale, 13f * scale),
                    Offset(13f * scale, 13f * scale),
                ).forEach { topLeft ->
                    drawRoundRect(color = tint, topLeft = topLeft, size = cellSize, cornerRadius = corner, style = stroke)
                }
            }
        }
    }
}
