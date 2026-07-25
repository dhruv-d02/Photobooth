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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * CameraX-backed [CameraController]. Owns permission state and the [ImageCapture] use case;
 * the actual camera *session* (Preview use case, bindToLifecycle) is set up in
 * [CameraPreviewSurface]'s AndroidView, which needs the LifecycleOwner/PreviewView that
 * only exist inside a Composable - this class just holds the shared [imageCapture]
 * reference both sides bind to, plus the front/back and flash toggles.
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

    private val _lensFacing = MutableStateFlow(LensFacing.Front)
    override val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()
    override fun toggleLensFacing() {
        _lensFacing.value = if (_lensFacing.value == LensFacing.Front) LensFacing.Back else LensFacing.Front
    }

    internal val cameraSelector: CameraSelector
        get() = if (_lensFacing.value == LensFacing.Front) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

    private val _flashEnabled = MutableStateFlow(false)
    override val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()
    override fun toggleFlash() {
        _flashEnabled.value = !_flashEnabled.value
        imageCapture.flashMode = if (_flashEnabled.value) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    override suspend fun capturePhoto(): ByteArray = suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = try {
                        image.toMirroredJpegBytes()
                    } finally {
                        image.close()
                    }
                    continuation.resume(bytes)
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
private fun ImageProxy.toMirroredJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = android.graphics.Matrix().apply { postScale(-1f, 1f) }
    val mirrored = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return ByteArrayOutputStream().use { stream ->
        mirrored.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
        stream.toByteArray()
    }
}
