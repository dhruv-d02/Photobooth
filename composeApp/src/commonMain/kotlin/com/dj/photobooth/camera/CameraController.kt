package com.dj.photobooth.camera

import kotlinx.coroutines.flow.StateFlow

/** Which physical camera is currently active. The design fires the front camera by default. */
enum class LensFacing { Front, Back }

/**
 * Why the camera can't be used, when permission alone doesn't explain it. Deliberately
 * separate from `hasCameraPermission == false`: the user declining is a different situation
 * from the hardware simply not being there, and only the latter is unrecoverable by asking
 * again.
 *
 * [NoCameraAvailable] - the requested lens doesn't exist on this device at all (an emulator
 * with `hw.camera.front=none`, a desktop-style device with no front camera).
 * [BindFailed] - the lens exists but couldn't be opened: driver error, another app already
 * holding it, or CameraX provider initialisation failing.
 */
enum class CameraError { NoCameraAvailable, BindFailed }

/**
 * Platform camera access, behind a plain interface rather than expect/actual - CameraX
 * (Android) and AVFoundation (iOS) are different enough that a shared implementation makes
 * no sense, but the interface itself doesn't need expect/actual machinery: each platform's
 * concrete class (AndroidCameraController, IosCameraController) just implements this from
 * its own source set. Only the live preview surface (CameraPreviewSurface) needs an actual
 * expect/actual Composable, since that's tied to a platform-specific View/layer type.
 */
interface CameraController {
    /** Reactive permission state so the UI (and CaptureViewModel) can observe grant/deny. */
    val hasCameraPermission: StateFlow<Boolean>

    /** Triggers the platform permission prompt. Result surfaces via [hasCameraPermission]. */
    fun requestCameraPermission()

    /** Which camera CameraPreviewSurface should bind. Read-only for now - design/handoff/
     *  README.md's Capture screen has no front/back toggle control, so there's no mutator
     *  here yet; add one only when a real caller (a UI control) needs it. */
    val lensFacing: StateFlow<LensFacing>

    /**
     * Non-null once the platform has actually failed to open the camera, null while it is
     * fine or not yet attempted. Set by whichever layer performs the real bind (on Android
     * that's CameraPreviewSurface, since CameraX binds to a LifecycleOwner the controller
     * doesn't have), and observed by CaptureViewModel so a session can arm placeholder mode
     * honestly instead of reporting "front camera live" while every exposure silently fails.
     */
    val cameraError: StateFlow<CameraError?>

    /**
     * Captures one exposure and returns JPEG-encoded bytes, mirrored horizontally to match
     * what the user saw in the (mirrored) preview - design/handoff/README.md § Capture
     * geometry: "Preview and proof are mirrored horizontally, and the composed strip is
     * mirrored too, so what the user saw is what they get."
     */
    suspend fun capturePhoto(): ByteArray
}
