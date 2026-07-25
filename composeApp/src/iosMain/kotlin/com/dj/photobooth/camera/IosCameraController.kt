package com.dj.photobooth.camera

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AVFoundation-backed [CameraController] - Phase 4 territory (CLAUDE.md known blockers: iOS
 * builds/testing need a Mac, which isn't available yet). This is a best-effort skeleton so
 * commonMain has a real class to reference and the module structure matches
 * architecture.md, NOT a working implementation - it compiles as Kotlin/Native source but
 * has never been built or run (Kotlin/Native can only compile Apple targets on macOS).
 *
 * TODO(phase-4, needs Mac/cloud-CI): replace with a real AVCaptureSession-backed
 * implementation - AVCaptureDevice.requestAccessForMediaType for permissions,
 * AVCaptureSession + AVCapturePhotoOutput for capture, mirroring to match
 * CameraController.capturePhoto's documented contract.
 */
class IosCameraController : CameraController {
    private val _hasCameraPermission = MutableStateFlow(false)
    override val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    override fun requestCameraPermission() {
        // Unimplemented placeholder: leaves permission denied, which drives the app into
        // documented placeholder-frame mode (design/handoff/README.md § Session sequence)
        // rather than crashing or hanging.
    }

    private val _lensFacing = MutableStateFlow(LensFacing.Front)
    override val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()

    override suspend fun capturePhoto(): ByteArray = ByteArray(0)
}
