package com.dj.photobooth.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Phase 4 territory, like CameraPreviewSurface.ios.kt - no Google Mobile Ads SDK wired into
 * the iOS target (would need the separate CocoaPod/SPM package via Kotlin/Native interop,
 * unbuildable/untestable without a Mac, see CLAUDE.md's iOS blocker). Renders nothing so
 * shared call sites (GalleryScreen) don't need any platform branching.
 */
@Composable
actual fun AdBanner(modifier: Modifier) {
}
