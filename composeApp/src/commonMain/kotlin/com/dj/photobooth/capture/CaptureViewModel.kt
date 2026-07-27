package com.dj.photobooth.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.photobooth.camera.CameraController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 *
 * Every coroutine this class starts (the countdown loop, the accept/reshoot pauses) is
 * launched through [launchSessionWork] and tracked in [sessionJob], so [onExit] (or a fresh
 * [onStartSession]) reliably cancels whatever is in flight - two separate untracked
 * `viewModelScope.launch{}` calls (one for keep, one for reshoot) previously meant a pending
 * accept/reshoot pause could resume and mutate state after the user had already exited.
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

    // Frozen once a session's queue actually starts running (see runQueue), rather than
    // re-read live from uiState.cameraState on every capture() call - otherwise a permission
    // grant that lands mid-session (after an earlier timeout already committed this session
    // to placeholder mode) would make some frames in the same strip real photos and others
    // placeholders, with no restart in between.
    private var sessionUsesPlaceholder = false

    init {
        // Keep cameraState in sync any time permission changes (e.g. the user grants it from
        // a system prompt after we already requested it in onStartSession). Only upgrades
        // Idle/RequestingPermission -> Live: once a session has already committed to Denied
        // (see sessionUsesPlaceholder above), a late grant should apply to the *next* session,
        // not silently flip cameraState under a queue that's already mid-flight.
        viewModelScope.launch {
            cameraController.hasCameraPermission.collect { granted ->
                if (granted && _uiState.value.cameraState != CameraState.Live && !sessionUsesPlaceholder) {
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
        sessionUsesPlaceholder = false
        _uiState.update {
            it.copy(
                frames = List(count) { null },
                queue = emptyList(),
                review = null,
                sessionComplete = false,
                log = "",
            )
        }
        launchSessionWork { ensureCameraReady() }
    }

    /**
     * Re-shoot exactly ONE slot of an already-finished session - the Preview screen's per-cell
     * `RETAKE 0N` chip. Unlike [onStartSession] this deliberately does not clear anything:
     * [existingFrames] is carried straight through and only [slotIndex] gets re-exposed, so the
     * other accepted frames (and, one layer up, the user's treatment/frame-colour/layout
     * choices) survive the round trip. Same "never append or shift, always the same slot"
     * invariant [onShootAgain] enforces inside a running session.
     *
     * Arms a single-index queue rather than auto-firing - the user still taps the shutter,
     * which then reads `SHOOT 0N` for that one frame (see [onRetake], whose shape this reuses).
     */
    fun onStartRetake(slotIndex: Int, existingFrames: List<CaptureFrame>) {
        require(slotIndex in existingFrames.indices) {
            "retake slot $slotIndex out of range for ${existingFrames.size} frames"
        }
        sessionUsesPlaceholder = false
        _uiState.update {
            it.copy(
                shotCount = existingFrames.size,
                frames = existingFrames,
                queue = listOf(slotIndex),
                review = null,
                sessionComplete = false,
                log = "",
            )
        }
        launchSessionWork { ensureCameraReady() }
    }

    /** Confirm or request camera access, falling back to placeholder mode if it never arrives.
     *  Shared verbatim by [onStartSession] and [onStartRetake] - a retake needs exactly the
     *  same camera bring-up as a fresh session, it just keeps the frames around. */
    private suspend fun ensureCameraReady() {
        if (cameraController.hasCameraPermission.value) {
            _uiState.update {
                it.copy(cameraState = CameraState.Live, log = "Front camera live · tap shoot when ready.")
            }
            return
        }
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

    /** Shutter: starts the already-queued frame (armed by [onRetake]) or, for a fresh
     *  session, queues every frame index. */
    fun onShutter() {
        if (_uiState.value.shooting) return
        if (_uiState.value.queue.isEmpty()) {
            val count = _uiState.value.shotCount
            _uiState.update { it.copy(queue = (0 until count).toList()) }
        }
        runQueue()
    }

    /** Per-cell RETAKE from the (future) Preview screen: arms a single-index queue and shows
     *  a "SHOOT 0N" shutter - design/handoff/README.md line 183 - rather than auto-firing;
     *  the user still has to tap the shutter, same as starting a fresh session. */
    fun onRetake(index: Int) {
        if (_uiState.value.shooting) return
        _uiState.update { it.copy(queue = listOf(index), review = null, sessionComplete = false) }
    }

    /** KEEP: commit the reviewed frame, pause 420ms, advance to the next queued index. */
    fun onKeep() {
        val review = _uiState.value.review ?: return
        launchSessionWork {
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
        launchSessionWork {
            _uiState.update {
                it.copy(review = null, log = "Frame ${it.frameLabel(review.index)} discarded · shooting again.")
            }
            delay(320)
            processNextInQueue()
        }
    }

    /** Exit at any time: cancel whatever session work is in flight, clear the queue. */
    fun onExit() {
        sessionJob?.cancel()
        _uiState.update { CaptureUiState(shotCount = it.shotCount, frames = List(it.shotCount) { null }) }
    }

    /** Cancels any previous in-flight session coroutine before starting a new one, so KEEP,
     *  SHOOT AGAIN, and the initial queue run can never overlap or outlive an EXIT/restart. */
    private fun launchSessionWork(block: suspend () -> Unit) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch { block() }
    }

    private fun runQueue() {
        sessionUsesPlaceholder = _uiState.value.cameraState == CameraState.Denied
        launchSessionWork {
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

    /** Falls back to a placeholder frame both when the session already committed to
     *  placeholder mode (see [sessionUsesPlaceholder]) and when a live capture throws for any
     *  other reason - a hardware/driver failure mid-session shouldn't crash the app when this
     *  exact fallback machinery already exists for the denied-permission case. */
    private suspend fun capture(): CaptureFrame {
        if (sessionUsesPlaceholder) {
            return CaptureFrame(jpegBytes = ByteArray(0), isPlaceholder = true)
        }
        return try {
            CaptureFrame(jpegBytes = cameraController.capturePhoto())
        } catch (e: CancellationException) {
            throw e // structured cancellation (e.g. onExit() mid-capture) must propagate, not be swallowed as a placeholder
        } catch (e: Exception) {
            CaptureFrame(jpegBytes = ByteArray(0), isPlaceholder = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionJob?.cancel()
    }

    private companion object {
        val COUNTDOWN_DIGITS = listOf("3", "2", "1")
        const val COUNTDOWN_STEP_MS = 760L
        const val FLASH_MS = 180L
    }
}
