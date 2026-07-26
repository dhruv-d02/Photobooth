package com.dj.photobooth.nav

/**
 * Every top-level NavHost destination, per architecture.md § Screen navigation:
 *
 * ```
 * Booth -->|START SESSION| Capture (Shoot)
 * Shoot -->|always starts a session| Capture
 * Capture -->|all frames accepted| Preview
 * Preview -->|RESHOOT| Capture
 * Preview -->|SAVE PNG| Booth
 * Strips -->|tap card| Preview
 * Preview -->|per-cell RETAKE| Capture
 * ```
 *
 * A sealed class (not raw string literals scattered at every `navigate()`/`composable()` call
 * site) so the route set is closed and typo-proof. [Booth], [Shoot] and [Strips] are the three
 * bottom-tab destinations (design/handoff/README.md § Bottom tab bar); [Preview] is reached
 * only by finishing a Capture session or tapping a Strips gallery card - never from the tab bar
 * itself, so it deliberately has no [BottomTab] entry.
 *
 * [Preview] and [Strips] route to the real `StripPreviewScreen` (`com.dj.photobooth.preview`)
 * and `GalleryScreen` (`com.dj.photobooth.gallery`) - see [com.dj.photobooth.nav.PhotoboothNavHost]
 * for how their ViewModels are constructed.
 */
sealed class Route(val route: String) {
    data object Booth : Route("booth")
    data object Shoot : Route("shoot")
    data object Strips : Route("strips")
    data object Preview : Route("preview")
}

/** The three bottom-tab destinations, in display order (design/handoff/README.md § Bottom tab bar). */
enum class BottomTab(val route: Route, val glyph: String, val label: String) {
    Booth(Route.Booth, glyph = "▣", label = "BOOTH"),
    Shoot(Route.Shoot, glyph = "●", label = "SHOOT"),
    Strips(Route.Strips, glyph = "▤", label = "STRIPS"),
}
