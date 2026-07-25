package com.dj.photobooth.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.photobooth.camera.CameraController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * MVVM ViewModel for the Capture screen. Owns the entire session sequence from
 * design/handoff/README.md § Interactions & Behaviour - countdown -> capture -> proof ->
 * accept/reshoot, looped per queued frame index - as plain, platform-independent Kotlin
 * driven by [CameraController]. The View (CaptureScreen) only reads [uiState] and calls the
 * on*() methods in response to taps; it holds no session logic itself.
 *
 * Timings are the design's exact values: 760ms per countdown digit, 180ms flash, 420ms
 * pause after an accepted frame, 320ms pause after a reshoot, 260ms before leaving capture
 * once the queue empties.
 */
class CaptureViewModel(
    private val cameraController: CameraController,
    initialShotCount: Int = 4,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CaptureUiState(shotCount = initialShotCount, frames = List(initialShotCount) { null })
    )
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null

    init {
        // Keep cameraState in sync any time permission changes (e.g. the user grants it from
        // a system prompt after we already requested it in onStartSession).
        viewModelScope.launch {
            cameraController.hasCameraPermission.collect { granted ->
                if (granted && _uiState.value.cameraState != CameraState.Live) {
                    _uiState.update { it.copy(cameraState = CameraState.Live) }
                }
            }
        }
    }

    /** Shot count is configurable 2-8 (default 4) - design/handoff/README.md § Config. */
    fun onShotCountChange(count: Int) {
        if (_uiState.value.shooting) return
        val clamped = count.coerceIn(2, 8)
        _uiState.update { it.copy(shotCount = clamped, frames = List(clamped) { null }) }
    }

    /** Step 1 of the session sequence: clear state, request/confirm camera access. */
    fun onStartSession() {
        val count = _uiState.value.shotCount
        _uiState.update {
            it.copy(
                frames = List(count) { null },
                queue = emptyList(),
                review = null,
                sessionComplete = false,
                log = "",
            )
        }
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            if (cameraController.hasCameraPermission.value) {
                _uiState.update {
                    it.copy(cameraState = CameraState.Live, log = "Front camera live · tap shoot when ready.")
                }
            } else {
                _uiState.update { it.copy(cameraState = CameraState.RequestingPermission) }
                cameraController.requestCameraPermission()
                val granted = withTimeoutOrNull(10_000) {
                    cameraController.hasCameraPermission.first { it }
                }
                if (granted == true) {
                    _uiState.update {
                        it.copy(cameraState = CameraState.Live, log = "Front camera live · tap shoot when ready.")
                    }
                } else {
                    _uiState.update {
                        it.copy(cameraState = CameraState.Denied, log = "Camera unavailable — placeholder frames armed.")
                    }
                }
            }
        }
    }

    /** Shutter: queue every frame index. */
    fun onShutter() {
        if (_uiState.value.shooting) return
        val count = _uiState.value.shotCount
        _uiState.update { it.copy(queue = (0 until count).toList()) }
        runQueue()
    }

    /** Per-cell RETAKE from the (future) Preview screen: a single-index queue. */
    fun onRetake(index: Int) {
        if (_uiState.value.shooting) return
        _uiState.update { it.copy(queue = listOf(index), review = null, sessionComplete = false) }
        runQueue()
    }

    /** KEEP: commit the reviewed frame, pause 420ms, advance to the next queued index. */
    fun onKeep() {
        val review = _uiState.value.review ?: return
        viewModelScope.launch {
            val newFrames = _uiState.value.frames.toMutableList().also { it[review.index] = review.frame }
            _uiState.update {
                it.copy(
                    frames = newFrames,
                    queue = it.queue.drop(1),
                    review = null,
                    log = "Frame ${it.frameLabel(review.index)} accepted.",
                )
            }
            delay(420)
            processNextInQueue()
        }
    }

    /** SHOOT AGAIN: discard the reviewed frame, pause 320ms, re-fire the SAME index - the
     *  queue head is never popped here, so a strip can never end up with a gap. */
    fun onShootAgain() {
        val review = _uiState.value.review ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(review = null, log = "Frame ${it.frameLabel(review.index)} discarded · shooting again.")
            }
            delay(320)
            processNextInQueue()
        }
    }

    /** Exit at any time: cancel the session, clear the queue. */
    fun onExit() {
        sessionJob?.cancel()
        _uiState.update { CaptureUiState(shotCount = it.shotCount, frames = List(it.shotCount) { null }) }
    }

    private fun runQueue() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            _uiState.update { it.copy(shooting = true) }
            processNextInQueue()
        }
    }

    private suspend fun processNextInQueue() {
        val index = _uiState.value.queue.firstOrNull()
        if (index == null) {
            delay(260)
            _uiState.update { it.copy(shooting = false, sessionComplete = true, log = "") }
            return
        }

        for (digit in COUNTDOWN_DIGITS) {
            _uiState.update { it.copy(countdown = digit) }
            delay(COUNTDOWN_STEP_MS)
        }
        _uiState.update { it.copy(countdown = "") }

        _uiState.update { it.copy(flash = true) }
        val frame = capture()
        delay(FLASH_MS)
        _uiState.update { it.copy(flash = false) }

        _uiState.update {
            it.copy(review = ReviewState(index, frame), log = "Frame ${it.frameLabel(index)} exposed · keep it?")
        }
        // Loop pauses here (shutter/queue processing stays idle) until onKeep()/onShootAgain().
    }

    private suspend fun capture(): CaptureFrame =
        if (_uiState.value.cameraState == CameraState.Denied) {
            CaptureFrame(jpegBytes = ByteArray(0), isPlaceholder = true)
        } else {
            CaptureFrame(jpegBytes = cameraController.capturePhoto())
        }

    fun onToggleLensFacing() {
        if (_uiState.value.shooting) return
        cameraController.toggleLensFacing()
        _uiState.update { it.copy(lensFacing = cameraController.lensFacing.value) }
    }

    fun onToggleFlash() {
        cameraController.toggleFlash()
        _uiState.update { it.copy(flashEnabled = cameraController.flashEnabled.value) }
    }

    override fun onCleared() {
        sessionJob?.cancel()
    }

    private companion object {
        val COUNTDOWN_DIGITS = listOf("3", "2", "1")
        const val COUNTDOWN_STEP_MS = 760L
        const val FLASH_MS = 180L
    }
}
