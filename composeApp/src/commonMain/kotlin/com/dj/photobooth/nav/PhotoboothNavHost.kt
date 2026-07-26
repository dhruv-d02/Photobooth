package com.dj.photobooth.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.capture.CaptureFrame
import com.dj.photobooth.capture.CaptureScreen
import com.dj.photobooth.capture.CaptureViewModel
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.ShareSheet
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.gallery.GalleryScreen
import com.dj.photobooth.gallery.GalleryViewModel
import com.dj.photobooth.landing.LandingScreen
import com.dj.photobooth.preview.StripPreviewScreen
import com.dj.photobooth.preview.StripPreviewViewModel

/**
 * The app's root navigation graph - Navigation-Compose (multiplatform), the decided library
 * per architecture.md's "Library choices and rationale" table. Implements the nav diagram in
 * architecture.md § Screen navigation:
 *
 * ```
 * Booth -->|START SESSION| Shoot (Capture)
 * Shoot -->|always starts a session| Shoot (Capture)
 * Shoot -->|all frames accepted| Preview
 * Strips -->|tap card| Preview      (not yet wired - a saved HistoryEntry has no raw per-frame
 *                                    data to rebuild a StripPreviewViewModel from, only the
 *                                    already-composited final image; deferred, see GalleryScreen's
 *                                    onOpenEntry below)
 * Preview -->|RESHOOT| Shoot
 * Preview -->|SAVE PNG| Booth       (auto-navigates once StripPreviewUiState.saved flips true)
 * ```
 *
 * [galleryRepo], [mediaRepo] and [shareSheet] are constructed per-platform (MainActivity /
 * MainViewController, mirroring how [cameraController] already arrives) and threaded down to
 * the Strips and Preview destinations - this is the integration point between the two branches
 * that built Gallery/Preview (`feature/export-history`) and this nav graph
 * (`feature/landing-navigation`) independently in parallel.
 */
@Composable
fun PhotoboothNavHost(
    cameraController: CameraController,
    galleryRepo: GalleryRepo,
    mediaRepo: MediaRepo,
    shareSheet: ShareSheet? = null,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Real, uncapped count (CLAUDE.md: no eviction limit) straight from the Room-backed repo -
    // the `stripCount: Int = 0` seam BottomTabBar/PhotoboothNavHost shipped with while Gallery's
    // data layer was still being built in parallel is now retired in favour of this.
    val entries by galleryRepo.entries.collectAsState(initial = emptyList())

    // Frames from the just-completed Capture session, held here (not passed as a nav argument -
    // Navigation-Compose args aren't a good fit for a list of JPEG byte arrays) until the
    // Preview destination reads them. Reset to null once consumed isn't necessary: Shoot always
    // starts a fresh CaptureViewModel per architecture.md ("Shoot --> always starts a session"),
    // so a stale value here is simply overwritten before Preview could ever be reached again.
    var pendingFrames by remember { mutableStateOf<List<CaptureFrame>>(emptyList()) }

    // Tab bar hidden on Capture (design/handoff/README.md § Bottom tab bar: "present on
    // every screen except capture" - a session is modal) and on Preview (not one of the
    // three tab destinations in architecture.md's nav diagram).
    val showTabBar = currentRoute == Route.Booth.route || currentRoute == Route.Strips.route

    Scaffold(
        bottomBar = {
            if (showTabBar) {
                BottomTabBar(
                    currentRoute = currentRoute,
                    stripCount = entries.size,
                    onTabSelected = { route ->
                        navController.navigate(route.route) {
                            // Standard bottom-nav pattern: pop back to the graph's start
                            // destination (saving state) so repeated taps on the same tab
                            // don't stack duplicate destinations, and switching tabs restores
                            // whatever state that tab had rather than starting fresh.
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Booth.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Route.Booth.route) {
                LandingScreen(
                    onStartSession = { navController.navigate(Route.Shoot.route) },
                )
            }
            composable(Route.Shoot.route) {
                // Plain `remember`, matching App.kt's prior convention (see git history) -
                // Koin/DI isn't wired yet, so a fresh CaptureViewModel per visit to this
                // destination is the simplest correct thing: entering Shoot always starts a
                // new session, per architecture.md ("Shoot --> always starts a session").
                val viewModel = remember(cameraController) { CaptureViewModel(cameraController) }
                CaptureScreen(
                    viewModel = viewModel,
                    cameraController = cameraController,
                    onExitToLanding = {
                        navController.navigate(Route.Booth.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onSessionComplete = { frames ->
                        pendingFrames = frames
                        navController.navigate(Route.Preview.route)
                    },
                )
            }
            composable(Route.Strips.route) {
                val viewModel = remember(galleryRepo, shareSheet) {
                    GalleryViewModel(galleryRepo, shareSheet)
                }
                GalleryScreen(
                    viewModel = viewModel,
                    // See class doc above - opening a past entry doesn't have a destination to
                    // land on yet (no raw frames to preview/re-customize, only the already-saved
                    // PNG), so this is a deliberate no-op for now rather than a half-built flow.
                    onOpenEntry = {},
                )
            }
            composable(Route.Preview.route) {
                val viewModel = remember(pendingFrames, galleryRepo, mediaRepo, shareSheet) {
                    StripPreviewViewModel(
                        initialFrames = pendingFrames,
                        galleryRepo = galleryRepo,
                        mediaRepo = mediaRepo,
                        shareSheet = shareSheet,
                    )
                }
                val state by viewModel.uiState.collectAsState()
                // "Preview -->|SAVE PNG| Booth" (architecture.md's nav diagram) - fires once,
                // right after onSavePng() successfully archives the strip, same LaunchedEffect
                // shape CaptureScreen already uses for its own sessionComplete -> navigate edge.
                LaunchedEffect(state.saved) {
                    if (state.saved) {
                        navController.navigate(Route.Booth.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
                StripPreviewScreen(
                    viewModel = viewModel,
                    onReshoot = {
                        navController.navigate(Route.Shoot.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                        }
                    },
                    // True per-slot retake needs CaptureViewModel to support re-entering the
                    // capture loop for a single existing session, which doesn't exist yet -
                    // StripPreviewViewModel.replaceFrame's doc comment already flags this as the
                    // seam for it. Until that lands, route the same as the full RESHOOT action
                    // rather than silently doing nothing.
                    onRetakeRequested = {
                        navController.navigate(Route.Shoot.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                        }
                    },
                )
            }
        }
    }
}
