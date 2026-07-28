package com.dj.photobooth.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * CameraX-backed [CameraController]. Owns permission state and the [ImageCapture] use case;
 * the actual camera *session* (Preview use case, bindToLifecycle) is set up in
 * [CameraPreviewSurface]'s AndroidView, which needs the LifecycleOwner/PreviewView that
 * only exist inside a Composable - this class just holds the shared [imageCapture]
 * reference both sides bind to, plus which camera to bind ([cameraSelector]).
 *
 * Must be constructed in MainActivity.onCreate BEFORE setContent(): registerForActivityResult
 * requires registration before the Activity reaches STARTED.
 */
class AndroidCameraController(private val activity: ComponentActivity) : CameraController {

    internal val imageCapture: ImageCapture = ImageCapture.Builder().build()

    private val _hasCameraPermission = MutableStateFlow(
        ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
    override val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> _hasCameraPermission.value = granted }

    override fun requestCameraPermission() {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // No mutator yet - design/handoff/README.md's Capture screen has no front/back toggle,
    // so always Front (design: "the front camera fires N times"). Add a setter only once a
    // real caller needs it (see CameraController.lensFacing's doc comment).
    private val _lensFacing = MutableStateFlow(LensFacing.Front)
    override val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()

    internal val cameraSelector: CameraSelector
        get() = if (_lensFacing.value == LensFacing.Front) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

    private val _cameraError = MutableStateFlow<CameraError?>(null)
    override val cameraError: StateFlow<CameraError?> = _cameraError.asStateFlow()

    /**
     * Reported by [CameraPreviewSurface], which is where the actual CameraX bind happens -
     * `bindToLifecycle` needs a LifecycleOwner this class deliberately doesn't hold. `internal`
     * so only that Composable can call it; the common [CameraController] interface exposes
     * [cameraError] read-only.
     */
    internal fun onBindResult(error: CameraError?) {
        _cameraError.value = error
    }

    override suspend fun capturePhoto(): ByteArray = suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(
            // A background executor, not Main: onCaptureSuccess below does a full-resolution
            // decode + mirror + re-encode, which would otherwise block the UI thread on every
            // single shutter press (this used to run on getMainExecutor()).
            Dispatchers.IO.asExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Any failure here (including a null decode) must still resume the
                    // continuation via resumeWithException, not throw past this callback -
                    // an uncaught throw here would leave capturePhoto() suspended forever,
                    // since CameraX has no way to route it to onError() after the fact.
                    try {
                        val bytes = try {
                            image.toMirroredJpegBytes()
                        } finally {
                            image.close()
                        }
                        continuation.resume(bytes)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            },
        )
    }
}

// Front-camera capture comes back un-mirrored from CameraX even though the live preview is
// mirrored (PreviewView does the mirroring itself, at display time, not on the underlying
// buffer) - so the captured JPEG needs an explicit horizontal flip to match what the user
// actually saw, per design/handoff/README.md § Capture geometry ("what the user saw is
// what they get").
//
// TODO(commonMain): everything past the raw buffer read below is plain bitmap math (decode /
// horizontal flip / re-encode) with no CameraX dependency - CLAUDE.md's commonMain-first
// convention says this belongs in shared Kotlin, not androidMain. Left here for now rather
// than pulling in the Skiko dependency mid-Phase-1; move it to a shared function (Skia-based,
// same as the Phase 2 filter/compositor work) once that dependency is already in place,
// instead of duplicating this flip logic again for IosCameraController in Phase 4.
private fun ImageProxy.toMirroredJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
        "Failed to decode captured JPEG (${bytes.size} bytes)"
    }
    val matrix = android.graphics.Matrix().apply { postScale(-1f, 1f) }
    val mirrored = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    try {
        return ByteArrayOutputStream().use { stream ->
            mirrored.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
            stream.toByteArray()
        }
    } finally {
        bitmap.recycle()
        mirrored.recycle()
    }
}
