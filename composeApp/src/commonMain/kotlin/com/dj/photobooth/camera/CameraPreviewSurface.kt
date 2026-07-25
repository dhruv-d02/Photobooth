package com.dj.photobooth.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The live camera feed itself. This has to be expect/actual (not just an interface impl like
 * CameraController) because it renders a platform View/layer directly: Android hosts CameraX's
 * PreviewView via AndroidView, iOS hosts an AVCaptureVideoPreviewLayer via UIKitView - there's
 * no Compose Multiplatform type that abstracts over both.
 */
@Composable
expect fun CameraPreviewSurface(controller: CameraController, modifier: Modifier)
