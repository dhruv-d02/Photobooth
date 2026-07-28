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
 *
 * The [CameraController] is passed in as an *initial* value only, never held as a constructor
 * `val` - see [onCameraControllerChanged] for why that distinction is load-bearing on Android.
 */
class CaptureViewModel(
    initialCameraController: CameraController,
    initialShotCount: Int = 4,
) : ViewModel() {

    // WHAT: the controller every capture/permission call goes through, swappable at runtime.
    // WHY: on Android the controller is owned by the Activity (it registers a permission
    // launcher and holds the ImageCapture use case that the preview surface binds to the
    // Activity's lifecycle), and the Activity is rebuilt on every configuration change - while
    // this ViewModel, scoped to a NavBackStackEntry, is not. Pinning the constructor argument
    // would leave us driving a controller nobody is bound to. See [onCameraControllerChanged].
    private var cameraController: CameraController = initialCameraController

    // Tracks the collectors watching the *current* controller's permission/error flows, so a
    // controller swap can cancel them and re-subscribe to the new instance instead of listening
    // to a dead Activity's state forever.
    private var cameraObserverJob: Job? = null

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

    // Which slot a single-slot retake is targeting, or null for a normal full session. Owned
    // here rather than by the nav layer because the nav layer hands the request over and then
    // immediately clears it (SessionHandoffViewModel.consumeRetakeRequest), so from that moment
    // on this is the only record of which mode the in-flight session is in - which is what the
    // NavHost reads to decide "pop back to Preview" vs "push a new Preview".
    var retakeSlot: Int? = null
        private set

    // Guards [startOnce]: the composition is torn down and rebuilt on every configuration
    // change, but this ViewModel is not, so an unguarded start-on-appear would re-run
    // onStartSession() after each rotation and silently wipe every accepted frame.
    private var started = false

    /**
     * Starts this ViewModel's one and only session, idempotently - safe to call from a
     * `LaunchedEffect` that re-fires on recomposition/rotation. Picks between a fresh session
     * and a single-slot retake based on [retakeSlotIndex]; re-entering Capture later gets a new
     * ViewModel (new back-stack entry) and therefore a new session, which is exactly the
     * intended lifetime.
     *
     * A retake for a slot that doesn't exist in [existingFrames] is refused outright rather
     * than quietly downgraded to a fresh session. WHY: the downgrade was silently destructive -
     * it wiped every already-accepted frame, and (because [retakeSlot] then stayed null) made
     * the nav layer push a *second* Preview entry on top of the one the retake came from. A
     * caller asking to re-shoot slot N of a strip that has no slot N is a programming error in
     * the caller, so fail visibly and change nothing.
     */
    fun startOnce(retakeSlotIndex: Int?, existingFrames: List<CaptureFrame>) {
        if (started) return
        if (retakeSlotIndex != null && retakeSlotIndex !in existingFrames.indices) {
            // Spend the one-shot guard anyway: re-running this on the next recomposition would
            // only repeat the same refusal. The user still has EXIT, and the Preview entry the
            // request came from is untouched underneath, so nothing is lost.
            started = true
            _uiState.update {
                it.copy(
                    // Mark the entry a dead end. WITHOUT this the refusal is only skin-deep:
                    // it leaves shooting = false and the queue empty, CaptureScreen keeps the
                    // shutter enabled (it gates on !shooting), and one tap of SHOOT sends
                    // onShutter() down its "empty queue = fresh session" path - wiping the very
                    // frames this branch exists to protect, then pushing a duplicate Preview
                    // entry because retakeSlot stayed null. Refusing has to disarm the shutter.
                    sessionRefused = true,
                    cameraState = CameraState.Unavailable,
                    // Report the slot the caller actually asked for. frameLabel() is 1-based
                    // padding over an arbitrary Int, so an out-of-range index would render a
                    // nonsense frame number ("06" of a 2-frame strip); state the real problem.
                    log = "Retake unavailable · this strip has only ${existingFrames.size} frame(s) · exit and try again.",
                )
            }
            return
        }
        started = true
        if (retakeSlotIndex != null) {
            onStartRetake(retakeSlotIndex, existingFrames)
        } else {
            onStartSession()
        }
    }

    init {
        observeCamera()
    }

    /**
     * Re-points this ViewModel at the [CameraController] that is currently bound to the live
     * lifecycle, and re-subscribes the permission/availability collectors to it. Cheap and
     * idempotent (a same-instance call is a no-op), so the composition can call it
     * unconditionally on every pass - which is exactly what PhotoboothNavHost does.
     *
     * WHY this exists: MainActivity builds a new AndroidCameraController in every `onCreate`,
     * and there is no `android:configChanges` in the manifest, so a rotation mid-session means
     * a brand-new controller instance. This ViewModel survives that rotation (it is scoped to a
     * NavBackStackEntry), and the preview surface binds the *new* controller's ImageCapture to
     * the *new* Activity's lifecycle. Without this hook we would keep driving the old,
     * now-unbound controller: takePicture() throws, [capture]'s catch-all quietly turns every
     * remaining exposure into a blank placeholder frame, and the bind result/[CameraController.cameraError]
     * that would have explained it lands on the new controller nobody is listening to - so the
     * viewfinder still claims "FRONT CAMERA LIVE" while producing nothing. It also stops us
     * retaining the destroyed Activity (AndroidCameraController holds one) and firing
     * requestCameraPermission() at a launcher registered against a dead Activity.
     */
    fun onCameraControllerChanged(controller: CameraController) {
        if (controller === cameraController) return
        cameraController = controller
        observeCamera()
    }

    /** Subscribes the two "is the camera actually usable" collectors to whichever controller is
     *  current, cancelling any previous controller's subscriptions first (see [cameraObserverJob]).
     *  Both flows are StateFlows, so a swap immediately re-delivers the new controller's current
     *  permission/error values rather than waiting for a change. */
    private fun observeCamera() {
        cameraObserverJob?.cancel()
        val controller = cameraController
        cameraObserverJob = viewModelScope.launch {
            // Keep cameraState in sync any time permission changes (e.g. the user grants it from
            // a system prompt after we already requested it in onStartSession). Only upgrades
            // Idle/RequestingPermission -> Live: once a session has already committed to Denied
            // (see sessionUsesPlaceholder above), a late grant should apply to the *next* session,
            // not silently flip cameraState under a queue that's already mid-flight.
            launch {
                controller.hasCameraPermission.collect { granted ->
                    if (granted && _uiState.value.cameraState != CameraState.Live && !sessionUsesPlaceholder) {
                        _uiState.update { it.copy(cameraState = CameraState.Live) }
                    }
                }
            }

            // Camera *availability*, which permission alone doesn't tell us. ensureCameraReady()
            // can only optimistically report Live once permission is granted - the real bind
            // happens later, in the preview surface. Without this, a bind failure left the UI
            // saying "FRONT CAMERA LIVE" while every exposure silently became a blank placeholder.
            launch {
                controller.cameraError.collect { error ->
                    if (error != null && !sessionUsesPlaceholder) {
                        _uiState.update {
                            it.copy(
                                cameraState = CameraState.Unavailable,
                                log = "Camera unavailable — placeholder frames armed.",
                            )
                        }
                    }
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
        retakeSlot = null
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
        retakeSlot = slotIndex
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
            markPermissionGranted()
            return
        }
        _uiState.update { it.copy(cameraState = CameraState.RequestingPermission) }
        cameraController.requestCameraPermission()
        // The controller we actually asked through - pinned so the suspension below can't be
        // re-targeted halfway.
        val requestedOn = cameraController
        val grantedByRequestedController = withTimeoutOrNull(10_000) {
            requestedOn.hasCameraPermission.first { it }
        }
        // On timeout, re-read the *current* controller before declaring denial: rotating while
        // the system permission dialog is up recreates the Activity, and the result is then
        // delivered to the new Activity's launcher - i.e. to a controller onCameraControllerChanged
        // swapped in while we were suspended on the old one's flow, which will now never emit.
        // Without this re-check, a permission the user actually granted still times out into
        // placeholder mode.
        val granted = grantedByRequestedController ?: cameraController.hasCameraPermission.value
        if (granted) {
            markPermissionGranted()
        } else {
            _uiState.update {
                it.copy(cameraState = CameraState.Denied, log = "Camera unavailable — placeholder frames armed.")
            }
        }
    }

    /** Permission is granted, but that alone doesn't mean the camera opened - if a bind has
     *  already failed (see the error collector in [observeCamera]), stay honest
     *  rather than announcing a live camera that isn't. */
    private fun markPermissionGranted() {
        if (cameraController.cameraError.value != null) {
            _uiState.update {
                it.copy(cameraState = CameraState.Unavailable, log = "Camera unavailable — placeholder frames armed.")
            }
        } else {
            _uiState.update {
                it.copy(cameraState = CameraState.Live, log = "Front camera live · tap shoot when ready.")
            }
        }
    }

    /** Shutter: starts the already-queued frame (armed by [onRetake]) or, for a fresh
     *  session, queues every frame index. */
    fun onShutter() {
        if (_uiState.value.shooting) return
        // A refused entry (out-of-range retake, see startOnce) must never start a session. This
        // guard lives here as well as on the button's `enabled` because the ViewModel is the
        // authority: the empty-queue branch below reads as "fresh session", so any future caller
        // reaching onShutter() by another route would otherwise silently wipe the strip.
        if (_uiState.value.sessionRefused) return
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
        // Both non-Live terminal states mean "no real pixels are coming" - freeze that in for
        // the whole session so a strip can't end up half real photos, half placeholders.
        sessionUsesPlaceholder = _uiState.value.cameraState.let {
            it == CameraState.Denied || it == CameraState.Unavailable
        }
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
