package com.dj.photobooth.camera

import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun CameraPreviewSurface(controller: CameraController, modifier: Modifier) {
    val android = controller as? AndroidCameraController ?: return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lensFacing by android.lensFacing.collectAsState()
    val hasPermission by android.hasCameraPermission.collectAsState()

    if (!hasPermission) return

    val previewView = remember { PreviewView(context) }

    // Re-binds whenever the lens facing changes (front/back toggle) - CameraX requires a
    // fresh bindToLifecycle() with the new CameraSelector, it can't hot-swap the selector
    // on an existing binding.
    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, android.cameraSelector, preview, android.imageCapture)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
