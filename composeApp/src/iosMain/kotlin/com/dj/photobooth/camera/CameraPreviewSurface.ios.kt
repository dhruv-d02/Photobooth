package com.dj.photobooth.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Phase 4 territory (see IosCameraController) - a real implementation hosts an
 * AVCaptureVideoPreviewLayer via UIKitView, unbuildable/untestable without a Mac. Renders
 * an empty dark surface for now so the shared CaptureScreen layout at least has a
 * predictable Composable to lay out around, once this target can actually be built.
 */
@Composable
actual fun CameraPreviewSurface(controller: CameraController, modifier: Modifier) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize().background(Color.Black))
}
