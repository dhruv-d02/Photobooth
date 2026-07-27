package com.dj.photobooth.nav

/**
 * Every top-level NavHost destination, per architecture.md § Screen navigation:
 *
 * ```
 * Booth   -->|START SESSION|           Shoot (Capture)
 * Shoot   -->|always starts a session| Shoot
 * Shoot   -->|all frames accepted|     Preview
 * Preview -->|RESHOOT|                 Shoot   (fresh session, every frame cleared)
 * Preview -->|per-cell RETAKE 0N|      Shoot   (one slot only, other frames kept)
 * Preview -->|SAVE PNG|                Booth
 * Strips  -->|tap card|                system gallery app (leaves the app entirely)
 * ```
 *
 * A sealed class (not raw string literals scattered at every `navigate()`/`composable()` call
 * site) so the route set is closed and typo-proof. [Booth], [Shoot] and [Strips] are the three
 * bottom-tab destinations (design/handoff/README.md § Bottom tab bar); [Preview] is reached
 * only by finishing or retaking a Capture session - never from the tab bar itself, so it
 * deliberately has no [BottomTab] entry.
 *
 * Tapping a Strips card is not an in-app navigation at all: it hands the image to the
 * platform's gallery app via `MediaViewer`, so there is no "view an archived strip" route here.
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
