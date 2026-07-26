package com.dj.photobooth

import androidx.compose.runtime.Composable
import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.nav.PhotoboothNavHost
import com.dj.photobooth.theme.PhotoboothTheme

/**
 * Root composable. Hosts [PhotoboothNavHost] - the Booth/Shoot/Strips tab graph plus the
 * Preview destination reached once a Capture session completes (architecture.md § Screen
 * navigation). Screen-level composables (CaptureScreen, LandingScreen, ...) are no longer
 * hosted directly here; that responsibility now lives entirely in the NavHost.
 *
 * [cameraController] is constructed per-platform (MainActivity on Android, MainViewController
 * on iOS) and threaded down here, since Koin/DI isn't wired yet either - plain constructor
 * injection is enough for the one dependency this currently needs.
 */
@Composable
fun App(cameraController: CameraController) {
    PhotoboothTheme {
        PhotoboothNavHost(cameraController = cameraController)
    }
}
