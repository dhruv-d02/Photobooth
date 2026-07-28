package com.dj.photobooth.nav

/**
 * Every top-level NavHost destination, per architecture.md § Screen navigation:
 *
 * ```
 * Booth       -->|START SESSION|           Shoot (Capture)
 * Shoot       -->|always starts a session| Shoot
 * Shoot       -->|all frames accepted|     Preview
 * Preview     -->|RESHOOT|                 Shoot       (fresh session, every frame cleared)
 * Preview     -->|per-cell RETAKE 0N|      Shoot       (one slot only, other frames kept)
 * Preview     -->|SAVE PNG|                Booth
 * Strips      -->|tap card|                StripDetail (in-app viewer)
 * StripDetail -->|← BACK|                  Strips
 * StripDetail -->|DELETE, confirmed|       Strips      (pops back automatically)
 * ```
 *
 * A sealed class (not raw string literals scattered at every `navigate()`/`composable()` call
 * site) so the route set is closed and typo-proof. [Booth], [Shoot] and [Strips] are the three
 * bottom-tab destinations (design/handoff/README.md § Bottom tab bar); [Preview] and
 * [StripDetail] are each reached only from another screen, never from the tab bar itself, so
 * they deliberately have no [BottomTab] entry.
 *
 * [StripDetail] carries which archived strip to show as a path argument (`entryId`, a
 * [com.dj.photobooth.gallery.HistoryEntry.id]) rather than the whole entry - Navigation-Compose
 * route args are strings/primitives, the same constraint that keeps CaptureFrame lists out of
 * nav args entirely (see SessionHandoffViewModel). [StripDetail.routeFor] builds the concrete
 * route string for a `navigate()` call; the bare [StripDetail.route] pattern is what
 * `composable()` registers against.
 */
sealed class Route(val route: String) {
    data object Booth : Route("booth")
    data object Shoot : Route("shoot")
    data object Strips : Route("strips")
    data object Preview : Route("preview")
    data object StripDetail : Route("stripDetail/{entryId}") {
        fun routeFor(entryId: Long): String = "stripDetail/$entryId"
    }
}

/** The three bottom-tab destinations, in display order (design/handoff/README.md § Bottom tab bar). */
enum class BottomTab(val route: Route, val glyph: String, val label: String) {
    Booth(Route.Booth, glyph = "▣", label = "BOOTH"),
    Shoot(Route.Shoot, glyph = "●", label = "SHOOT"),
    Strips(Route.Strips, glyph = "▤", label = "STRIPS"),
}
