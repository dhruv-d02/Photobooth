package com.dj.photobooth.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dj.photobooth.camera.CameraController
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
 * a single slot. The difference is [SessionHandoffViewModel.retakeSlot]: when set, Capture
 * starts via [CaptureViewModel.onStartRetake] (which carries the existing frames through
 * untouched) and, once the one frame is kept, we `popBackStack()` to the *existing* Preview
 * entry rather than pushing a new one. That's deliberate - it keeps the Preview ViewModel
 * alive, so the user's treatment/frame-colour/layout selections survive a retake instead of
 * resetting.
 *
 * **Cross-destination state.** The frames/retake-slot/retake-result trio lives in
 * [SessionHandoffViewModel], not in `remember` - see that class for the rotation bugs `remember`
 * caused. Everything else here is either derived from the back stack or owned by a screen's own
 * ViewModel.
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

    // Session hand-off state (frames, pending retake slot, retake result). Obtained with
    // [viewModel] at the NavHost's own ViewModelStoreOwner - the Activity on Android - so it
    // survives configuration changes exactly like the per-destination ViewModels below do.
    // `remember` here was the bug: it reset on rotation while those ViewModels did not, which
    // turned a post-rotation RETAKE into a frame-wiping fresh session and corrupted the back
    // stack. See SessionHandoffViewModel's doc comment for the full failure trail.
    val session = viewModel { SessionHandoffViewModel() }
    val sessionFrames by session.sessionFrames.collectAsState()
    // No collectAsState for retakeSlot: its only consumer is the startOnce effect below, which
    // reads session.retakeSlot.value directly to avoid depending on collector dispatch order.
    val retakeResult by session.retakeResult.collectAsState()

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
                        // Fresh session: clears the retake mode *and* the previous strip's
                        // frames, so nothing from an abandoned session leaks into this one.
                        session.startFreshSession()
                        navController.navigate(Route.Shoot.route)
                    },
                )
            }

            composable(Route.Shoot.route) {
                val viewModel = viewModel { CaptureViewModel(cameraController) }

                // The factory above only runs on FIRST creation, but this ViewModel is scoped to
                // the back-stack entry and survives Activity recreation - while on Android
                // MainActivity builds a *new* CameraController in every onCreate, and it is that
                // new controller the preview surface binds to the live lifecycle. Re-point the
                // ViewModel on every composition pass so it can never drive a stale, unbound
                // controller (which threw on every capture and produced silent blank frames) or
                // keep the destroyed Activity alive. SideEffect, not LaunchedEffect: it runs
                // during the apply phase, i.e. before the startOnce effect below is dispatched,
                // so the first session already talks to the right controller.
                SideEffect { viewModel.onCameraControllerChanged(cameraController) }

                // Starting the session lives here, not in CaptureScreen: only the nav layer
                // knows whether the user asked for a fresh session or a single-slot retake.
                // startOnce is idempotent, which matters because this effect re-runs whenever
                // the composition is rebuilt (rotation) while the ViewModel itself survives -
                // an unguarded onStartSession() there would wipe every accepted frame.
                LaunchedEffect(viewModel) {
                    // Read the flows directly rather than the collectAsState() snapshots above.
                    // WHY: requestRetake() and navigate() happen in the same click handler, so
                    // this effect needs the value written moments earlier. A snapshot write is
                    // visible to the next composition unconditionally, but collectAsState routes
                    // through a collector coroutine that must be dispatched first - making
                    // correctness depend on dispatcher ordering, which holds on Android's
                    // AndroidUiDispatcher but is not guaranteed on iOS/desktop. Losing that race
                    // would hand startOnce a null slot and wipe the strip: the original bug.
                    // Inside a coroutine body `.value` is always current, so the race disappears.
                    viewModel.startOnce(session.retakeSlot.value, session.sessionFrames.value)
                    // Consumed: the ViewModel now owns the mode (CaptureViewModel.retakeSlot),
                    // so clearing here stops a stale request leaking into some later, unrelated
                    // visit to Shoot - e.g. backing out of a retake, then tapping the SHOOT tab,
                    // which would otherwise start a phantom single-slot retake over an old strip.
                    session.consumeRetakeRequest()
                }

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
                        session.onSessionComplete(frames)
                        // Asked of the ViewModel, not of the hand-off state: CaptureViewModel is
                        // the one place that still knows the mode after consumeRetakeRequest()
                        // above cleared the request.
                        val slot = viewModel.retakeSlot
                        if (slot != null && slot in frames.indices) {
                            // Retake: hand the one new frame back and return to the Preview
                            // entry already sitting underneath, ViewModel and all.
                            session.publishRetakeResult(slot, frames[slot])
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
                        session.consumeRetakeResult()
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
                        // Full restart: clear the retake slot and the old frames so Capture
                        // starts a genuinely fresh session, and drop this Preview entry entirely.
                        session.startFreshSession()
                        navController.navigate(Route.Shoot.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                        }
                    },
                    onRetakeRequested = { index ->
                        // Single slot: leave this Preview entry on the back stack so its
                        // ViewModel (and the user's styling choices) survive the round trip.
                        session.requestRetake(index)
                        navController.navigate(Route.Shoot.route)
                    },
                )
            }
        }
    }
}
