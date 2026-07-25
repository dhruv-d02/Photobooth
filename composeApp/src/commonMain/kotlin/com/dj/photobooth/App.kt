package com.dj.photobooth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.capture.CaptureScreen
import com.dj.photobooth.capture.CaptureViewModel
import com.dj.photobooth.theme.PhotoboothTheme

/**
 * Root composable. Temporarily hosts CaptureScreen directly - there's no Landing screen or
 * tab navigation yet (that's a separate, later piece of work); wiring Capture as a reachable,
 * testable unit on its own first, matches the project's phased build sequence in CLAUDE.md
 * better than building navigation around screens that don't exist yet.
 *
 * [cameraController] is constructed per-platform (MainActivity on Android, MainViewController
 * on iOS) and threaded down here, since Koin/DI isn't wired yet either - plain constructor
 * injection is enough for the one dependency this currently needs.
 *
 * CaptureViewModel is `remember`ed rather than obtained via the androidx.lifecycle.viewmodel
 * `viewModel()` composable factory - it survives recomposition but not e.g. an Android
 * configuration change. Acceptable simplification for now; revisit once real navigation
 * (and a natural place to own ViewModelStoreOwner scoping) exists.
 */
@Composable
fun App(cameraController: CameraController) {
    PhotoboothTheme {
        val viewModel = remember(cameraController) { CaptureViewModel(cameraController) }
        CaptureScreen(
            viewModel = viewModel,
            cameraController = cameraController,
            onExitToLanding = { /* no-op until the Landing screen exists */ },
        )
    }
}
