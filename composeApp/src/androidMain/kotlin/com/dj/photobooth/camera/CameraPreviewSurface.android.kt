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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun CameraPreviewSurface(controller: CameraController, modifier: Modifier) {
    // AndroidCameraController is the only CameraController implementation on this target -
    // failing loudly on a mismatch beats silently rendering a blank viewfinder with no clue
    // why, if some other CameraController implementation (a test double, a decorator) ever
    // reaches this Composable.
    val android = controller as? AndroidCameraController
        ?: error("CameraPreviewSurface (Android) requires an AndroidCameraController, got ${controller::class}")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lensFacing by android.lensFacing.collectAsState()
    val hasPermission by android.hasCameraPermission.collectAsState()

    if (!hasPermission) return

    val previewView = remember { PreviewView(context) }

    // Re-binds whenever the lens facing changes (front/back toggle) - CameraX requires a
    // fresh bindToLifecycle() with the new CameraSelector, it can't hot-swap the selector
    // on an existing binding.
    //
    // Everything here is guarded because a bind failure used to be invisible: the exception
    // escaped this LaunchedEffect, imageCapture was left unbound, and every subsequent
    // takePicture() threw - which CaptureViewModel.capture() quietly turned into a blank
    // placeholder frame while the UI still displayed "FRONT CAMERA LIVE". Reporting the
    // failure lets the session arm placeholder mode honestly instead.
    LaunchedEffect(lensFacing) {
        try {
            val cameraProvider = context.getCameraProvider()

            // Pre-check rather than relying on the bind to throw: hasCamera() distinguishes
            // "this device has no such lens" from "the lens exists but wouldn't open", and
            // the two are worth telling apart (only the latter is worth a retry).
            if (!cameraProvider.hasCamera(android.cameraSelector)) {
                android.onBindResult(CameraError.NoCameraAvailable)
                return@LaunchedEffect
            }

            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, android.cameraSelector, preview, android.imageCapture)
            android.onBindResult(null)
        } catch (e: CancellationException) {
            // Structured cancellation (leaving the screen mid-bind) is not a camera fault.
            throw e
        } catch (e: Exception) {
            android.onBindResult(CameraError.BindFailed)
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
