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
 * [Preview] and [Strips] are wired to placeholder composables for now (see
 * `nav/placeholder/PlaceholderPreviewScreen.kt` and `nav/placeholder/PlaceholderGalleryScreen.kt`
 * - deliberately kept out of `preview`/`gallery` packages so they don't collide with the real
 * screens' package paths) - the real "Strip Preview & Customize" and "Past strips" screens are
 * Phase 3 scope being built in parallel on `feature/export-history`. Swapping a placeholder for
 * the real screen is a one-line change: replace the placeholder composable inside that route's
 * single `composable(...)` block in [com.dj.photobooth.nav.PhotoboothNavHost] - nothing else in
 * this file needs to change.
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
