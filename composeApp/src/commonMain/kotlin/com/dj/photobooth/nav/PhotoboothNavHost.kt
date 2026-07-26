package com.dj.photobooth.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.capture.CaptureScreen
import com.dj.photobooth.capture.CaptureViewModel
import com.dj.photobooth.landing.LandingScreen
import com.dj.photobooth.nav.placeholder.PlaceholderGalleryScreen
import com.dj.photobooth.nav.placeholder.PlaceholderPreviewScreen

/**
 * The app's root navigation graph - Navigation-Compose (multiplatform), the decided library
 * per architecture.md's "Library choices and rationale" table. Implements the nav diagram in
 * architecture.md § Screen navigation:
 *
 * ```
 * Booth -->|START SESSION| Shoot (Capture)
 * Shoot -->|always starts a session| Shoot (Capture)
 * Shoot -->|all frames accepted| Preview
 * Strips -->|tap card| Preview      (not yet wired - Strips is a placeholder, see below)
 * Preview -->|SAVE PNG| Booth       (stands in via PreviewPlaceholderScreen's back button)
 * ```
 *
 * [stripCount] defaults to 0 (placeholder) and is threaded straight into [BottomTabBar] - the
 * real count comes from the Room-backed history layer being built in parallel on
 * `feature/export-history`, not in this worktree yet. Wiring the real value is a later
 * integration step; this parameter is the seam for it.
 *
 * Route -> screen mapping (each is exactly one `composable(...)` block below, so swapping a
 * placeholder for its real screen once the parallel branch lands is a one-line change):
 * - [Route.Booth] -> [LandingScreen]
 * - [Route.Shoot] -> [CaptureScreen] (existing, Phase 1)
 * - [Route.Strips] -> [PlaceholderGalleryScreen] (placeholder for the real Gallery - the real
 *   screen lives in `com.dj.photobooth.gallery` on `feature/export-history`, a package this
 *   placeholder deliberately avoids to prevent a merge collision)
 * - [Route.Preview] -> [PlaceholderPreviewScreen] (placeholder for the real Strip Preview)
 */
@Composable
fun PhotoboothNavHost(
    cameraController: CameraController,
    stripCount: Int = 0,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Tab bar hidden on Capture (design/handoff/README.md § Bottom tab bar: "present on
    // every screen except capture" - a session is modal) and on Preview (not one of the
    // three tab destinations in architecture.md's nav diagram).
    val showTabBar = currentRoute == Route.Booth.route || currentRoute == Route.Strips.route

    Scaffold(
        bottomBar = {
            if (showTabBar) {
                BottomTabBar(
                    currentRoute = currentRoute,
                    stripCount = stripCount,
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
                    onSessionComplete = {
                        navController.navigate(Route.Preview.route)
                    },
                )
            }
            composable(Route.Strips.route) {
                PlaceholderGalleryScreen()
            }
            composable(Route.Preview.route) {
                PlaceholderPreviewScreen(
                    onBackToBooth = {
                        navController.navigate(Route.Booth.route) {
                            popUpTo(Route.Booth.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
