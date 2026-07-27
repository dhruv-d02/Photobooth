package com.dj.photobooth

import androidx.compose.runtime.Composable
import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.MediaViewer
import com.dj.photobooth.export.ShareSheet
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.nav.PhotoboothNavHost
import com.dj.photobooth.theme.PhotoboothTheme

/**
 * Root composable. Hosts [PhotoboothNavHost] - the Booth/Shoot/Strips tab graph plus the
 * Preview destination reached once a Capture session completes (architecture.md § Screen
 * navigation). Screen-level composables (CaptureScreen, LandingScreen, ...) are no longer
 * hosted directly here; that responsibility now lives entirely in the NavHost.
 *
 * Every parameter is constructed per-platform (MainActivity on Android, MainViewController on
 * iOS) and threaded down here, since Koin/DI isn't wired yet either - plain constructor
 * injection is enough for the dependencies this currently needs.
 */
@Composable
fun App(
    cameraController: CameraController,
    galleryRepo: GalleryRepo,
    mediaRepo: MediaRepo,
    shareSheet: ShareSheet? = null,
    mediaViewer: MediaViewer? = null,
) {
    PhotoboothTheme {
        PhotoboothNavHost(
            cameraController = cameraController,
            galleryRepo = galleryRepo,
            mediaRepo = mediaRepo,
            shareSheet = shareSheet,
            mediaViewer = mediaViewer,
        )
    }
}
