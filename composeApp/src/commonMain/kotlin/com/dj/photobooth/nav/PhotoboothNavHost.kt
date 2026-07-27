package com.dj.photobooth.nav

import androidx.compose.foundation.layout.WindowInsets
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.dj.photobooth.export.MediaViewer
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
 * Booth   -->|START SESSION|      Shoot (Capture)
 * Shoot   -->|always starts a session| Shoot
 * Shoot   -->|all frames accepted|     Preview
 * Preview -->|RESHOOT|                 Shoot   (fresh session, every frame cleared)
 * Preview -->|per-cell RETAKE 0N|      Shoot   (one slot only, other frames kept)
 * Preview -->|SAVE PNG|                Booth   (auto, once StripPreviewUiState.saved flips)
 * Strips  -->|tap card|                system gallery app (not an in-app destination)
 * ```
 *
 * **ViewModel ownership.** Every ViewModel here is obtained with [viewModel], not `remember` -
 * that scopes it to the destination's `NavBackStackEntry`, which is what makes `onCleared()`
 * actually run when the entry leaves the back stack. Constructing them with `remember` instead
 * leaks their `viewModelScope` forever (CaptureViewModel's permission collector and
 * GalleryViewModel's eager Room-flow collector both run for the life of the process), and pins
 * whatever they hold - including the Activity behind [shareSheet]/[mediaViewer].
 *
 * **Retake vs reshoot.** RESHOOT clears everything and starts over; per-cell RETAKE re-shoots
 * a single slot. The difference is [retakeSlot]: when set, Capture starts via
 * [CaptureViewModel.onStartRetake] (which carries [sessionFrames] through untouched) and, once
 * the one frame is kept, we `popBackStack()` to the *existing* Preview entry rather than
 * pushing a new one. That's deliberate - it keeps the Preview ViewModel alive, so the user's
 * treatment/frame-colour/layout selections survive a retake instead of resetting.
 */
@Composable
fun PhotoboothNavHost(
    cameraController: CameraController,
    galleryRepo: GalleryRepo,
    mediaRepo: MediaRepo,
    shareSheet: ShareSheet? = null,
    mediaViewer: MediaViewer? = null,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Real, uncapped count (CLAUDE.md: no eviction limit) straight from the Room-backed repo.
    val entries by galleryRepo.entries.collectAsState(initial = emptyList())

    // Frames from the most recent Capture session. Held here rather than passed as a nav
    // argument - Navigation-Compose args are strings/primitives, not lists of JPEG byte arrays.
    var sessionFrames by remember { mutableStateOf<List<CaptureFrame>>(emptyList()) }

    // Non-null only while a per-cell RETAKE round trip is in flight; see the class doc.
    var retakeSlot by remember { mutableStateOf<Int?>(null) }

    // The frame a retake produced, waiting to be handed to the surviving Preview ViewModel.
    var retakeResult by remember { mutableStateOf<Pair<Int, CaptureFrame>?>(null) }

    // Tab bar hidden on Capture (design/handoff/README.md § Bottom tab bar: "present on
    // every screen except capture" - a session is modal) and on Preview (not one of the
    // three tab destinations in architecture.md's nav diagram).
    val showTabBar = currentRoute == Route.Booth.route || currentRoute == Route.Strips.route

    Scaffold(
        // Zero content insets: Scaffold's default would pad the NavHost by the status bar
        // height for *every* destination, which breaks Capture's specced full-bleed dark
        // screen (a light band appears above it, since Scaffold's own container is the light
        // Ground). Each screen applies the insets it actually wants instead - see
        // CaptureScreen's top bar vs the light screens' statusBarsPadding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
            // With zero content insets above, this is purely the bottom bar's own height -
            // exactly what we want reserved, and nothing more.
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Route.Booth.route) {
                LandingScreen(
                    onStartSession = {
                        retakeSlot = null
                        navController.navigate(Route.Shoot.route)
                    },
                )
            }

            composable(Route.Shoot.route) {
                val viewModel = viewModel { CaptureViewModel(cameraController) }
                // Read once at entry, so a later reset of retakeSlot can't change this
                // destination's mode out from under an in-flight session.
                val slot = remember { retakeSlot }
                val framesAtEntry = remember { sessionFrames }

                // Starting the session lives here, not in CaptureScreen: only the nav layer
                // knows whether the user asked for a fresh session or a single-slot retake.
                LaunchedEffect(viewModel) {
                    if (slot != null && slot in framesAtEntry.indices) {
                        viewModel.onStartRetake(slot, framesAtEntry)
                    } else {
                        viewModel.onStartSession()
                    }
                }

                CaptureScreen(
                    viewModel = viewModel,
                    cameraController = cameraController,
                    onExitToLanding = {
                        retakeSlot = null
                        navController.navigate(Route.Booth.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onSessionComplete = { frames ->
                        sessionFrames = frames
                        if (slot != null && slot in frames.indices) {
                            // Retake: hand the one new frame back and return to the Preview
                            // entry already sitting underneath, ViewModel and all.
                            retakeResult = slot to frames[slot]
                            retakeSlot = null
                            navController.popBackStack()
                        } else {
                            // Fresh session: replace Shoot with a brand-new Preview entry, so
                            // Back from Preview goes to Booth rather than re-entering Capture.
                            navController.navigate(Route.Preview.route) {
                                popUpTo(Route.Shoot.route) { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable(Route.Strips.route) {
                val viewModel = viewModel {
                    GalleryViewModel(repo = galleryRepo, mediaRepo = mediaRepo, mediaViewer = mediaViewer)
                }
                GalleryScreen(viewModel = viewModel)
            }

            composable(Route.Preview.route) {
                // sessionFrames is read only when the factory first runs for this entry, so a
                // retake landing later doesn't rebuild the ViewModel and discard the user's
                // treatment/frame-colour/layout choices - replaceFrame updates it in place.
                val viewModel = viewModel {
                    StripPreviewViewModel(
                        initialFrames = sessionFrames,
                        galleryRepo = galleryRepo,
                        mediaRepo = mediaRepo,
                        shareSheet = shareSheet,
                    )
                }
                val state by viewModel.uiState.collectAsState()

                // Consume a completed retake exactly once - replaceFrame re-runs the
                // decode/compose pipeline for the updated slot.
                LaunchedEffect(retakeResult) {
                    retakeResult?.let { (index, frame) ->
                        viewModel.replaceFrame(index, frame)
                        retakeResult = null
                    }
                }

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
                        // Full restart: clear the retake slot so Capture starts a fresh
                        // session, and drop this Preview entry entirely.
                        retakeSlot = null
                        navController.navigate(Route.Shoot.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                        }
                    },
                    onRetakeRequested = { index ->
                        // Single slot: leave this Preview entry on the back stack so its
                        // ViewModel (and the user's styling choices) survive the round trip.
                        retakeSlot = index
                        navController.navigate(Route.Shoot.route)
                    },
                )
            }
        }
    }
}
