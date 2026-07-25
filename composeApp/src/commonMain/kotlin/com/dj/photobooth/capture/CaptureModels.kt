package com.dj.photobooth.capture

import com.dj.photobooth.camera.LensFacing

/**
 * One accepted (or in-review) exposure. JPEG bytes, mirrored - see
 * CameraController.capturePhoto. [isPlaceholder] frames (camera denied/unavailable) carry
 * empty bytes; the UI renders a text placeholder instead of decoding an image.
 *
 * TODO(phase-2): generate a real placeholder image (gradient + hatch + "FRAME 0N · NO
 * CAMERA" label, pre-mirrored per design/handoff/README.md § Assets) once the Skia-based
 * compositor lands - the strip PNG export needs actual pixels to place, not just UI text.
 * Deliberately not pulling in the Skiko dependency early just for this.
 */
data class CaptureFrame(val jpegBytes: ByteArray, val isPlaceholder: Boolean = false) {
    // Data classes with a ByteArray property need equals/hashCode written by hand - the
    // generated ones compare array identity, not content, which would break state-diffing
    // (e.g. in tests) that expects two frames with the same bytes to be equal.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CaptureFrame) return false
        return isPlaceholder == other.isPlaceholder && jpegBytes.contentEquals(other.jpegBytes)
    }

    override fun hashCode(): Int = 31 * jpegBytes.contentHashCode() + isPlaceholder.hashCode()
}

/** Whether the live camera is available, still being requested, or unavailable/denied. */
enum class CameraState { Idle, RequestingPermission, Live, Denied }

/** The just-captured frame awaiting KEEP or SHOOT AGAIN, per design/handoff/README.md § 2. */
data class ReviewState(val index: Int, val frame: CaptureFrame)

data class CaptureUiState(
    val shotCount: Int = 4,
    val frames: List<CaptureFrame?> = List(shotCount) { null },
    val queue: List<Int> = emptyList(),
    val countdown: String = "",
    val flash: Boolean = false,
    val review: ReviewState? = null,
    val shooting: Boolean = false,
    val cameraState: CameraState = CameraState.Idle,
    val lensFacing: LensFacing = LensFacing.Front,
    val flashEnabled: Boolean = false,
    val log: String = "",
    val sessionComplete: Boolean = false,
) {
    /** Zero-padded 1-based frame count, e.g. "02" - the label format used throughout the design. */
    fun frameLabel(index: Int): String = (index + 1).toString().padStart(2, '0')

    val acceptedCount: Int get() = frames.count { it != null }
}
