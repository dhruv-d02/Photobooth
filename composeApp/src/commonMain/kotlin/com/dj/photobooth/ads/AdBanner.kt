package com.dj.photobooth.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A banner ad slot (Google AdMob). Expect/actual, not an interface impl like
 * CameraController/MediaRepo, because it renders a platform ad-serving View directly - same
 * reason CameraPreviewSurface is expect/actual instead of an interface: there's no Compose
 * Multiplatform type that abstracts over "host a native ad view."
 *
 * AdMob has no iOS artifact wired into this project yet (iOS bring-up is blocked on a Mac
 * regardless, per CLAUDE.md), so AdBanner.ios.kt renders nothing - call sites don't need to
 * branch on platform.
 *
 * Callers should not reserve fixed height for this: on Android it renders an adaptive banner
 * sized to the available width, and collapses to zero height when no ad is available to serve
 * (no network, no fill, or consent not yet gathered - see AdConsent) rather than showing blank
 * space.
 */
@Composable
expect fun AdBanner(modifier: Modifier = Modifier)
