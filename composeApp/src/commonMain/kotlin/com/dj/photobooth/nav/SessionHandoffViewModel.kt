package com.dj.photobooth.nav

import androidx.lifecycle.ViewModel
import com.dj.photobooth.capture.CaptureFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The small amount of state the nav layer has to carry *between* destinations: the frames a
 * Capture session produced, whether the next visit to Capture is a full session or a single-slot
 * retake, and the one frame a finished retake is handing back to Preview.
 *
 * **Why a ViewModel and not `remember`.** All three of these used to live in
 * `remember { mutableStateOf(...) }` inside PhotoboothNavHost. `remember` is scoped to the
 * composition, and on Android the composition is destroyed and rebuilt on every configuration
 * change (there is no `android:configChanges` in the manifest) - while the ViewModels scoped to
 * the NavBackStackEntries survive. That mismatch produced three real defects:
 *
 * 1. Rotate on Preview, then tap `RETAKE 02`: `sessionFrames` had reset to empty, so
 *    `CaptureViewModel.startOnce(1, emptyList())` was asked to retake a slot that no longer
 *    existed - which used to silently start a *fresh* session and wipe every accepted frame.
 * 2. That same fresh session left `CaptureViewModel.retakeSlot` null, so finishing it pushed a
 *    second Preview entry instead of popping back to the existing one: the back stack became
 *    `[Booth, Preview, Preview]` and Back showed a stale strip.
 * 3. An Activity recreation landing between `popBackStack()` and Preview consuming
 *    [retakeResult] dropped the retaken frame entirely.
 *
 * Hosted in a ViewModel, the state now has the same lifetime as the ViewModels it has to stay
 * consistent with. Deliberately **not** `rememberSaveable`: [CaptureFrame] carries full-resolution
 * JPEG byte arrays, and a strip of them would blow the ~1MB `savedInstanceState`/Binder budget.
 * That means this survives configuration changes but not process death - the same guarantee the
 * Capture/Preview ViewModels themselves give, so nothing here can outlive its counterpart.
 */
class SessionHandoffViewModel : ViewModel() {

    // Frames from the most recent Capture session. Held here rather than passed as a nav
    // argument - Navigation-Compose args are strings/primitives, not lists of JPEG byte arrays.
    private val _sessionFrames = MutableStateFlow<List<CaptureFrame>>(emptyList())
    val sessionFrames: StateFlow<List<CaptureFrame>> = _sessionFrames.asStateFlow()

    // Non-null only while a per-cell RETAKE round trip is being handed to Capture; Capture takes
    // ownership of the mode the moment it starts (CaptureViewModel.retakeSlot), and this is
    // cleared right after - see [consumeRetakeRequest].
    private val _retakeSlot = MutableStateFlow<Int?>(null)
    val retakeSlot: StateFlow<Int?> = _retakeSlot.asStateFlow()

    // The (slotIndex, frame) a finished retake produced, waiting to be handed to the surviving
    // Preview ViewModel via StripPreviewViewModel.replaceFrame.
    private val _retakeResult = MutableStateFlow<Pair<Int, CaptureFrame>?>(null)
    val retakeResult: StateFlow<Pair<Int, CaptureFrame>?> = _retakeResult.asStateFlow()

    /**
     * A fresh session is starting (Booth's `START SESSION`, or Preview's `RESHOOT`): clear the
     * retake mode *and* the previous session's frames. Dropping the frames here is what bounds
     * how long a strip's worth of JPEG bytes is held - at most one session's worth, which is the
     * same ceiling the old `remember`ed state had.
     */
    fun startFreshSession() {
        _retakeSlot.value = null
        _retakeResult.value = null
        _sessionFrames.value = emptyList()
    }

    /** Preview's per-cell `RETAKE 0N`: re-shoot exactly this slot, keeping [sessionFrames]. */
    fun requestRetake(slotIndex: Int) {
        _retakeSlot.value = slotIndex
    }

    /**
     * Capture has read the request and now owns the mode itself, so drop it here. WHY: a stale
     * request left set would leak into some later, unrelated visit to Shoot - e.g. backing out
     * of a retake and then tapping the SHOOT tab, which would otherwise start a phantom
     * single-slot retake over an old strip.
     */
    fun consumeRetakeRequest() {
        _retakeSlot.value = null
    }

    /**
     * A Capture session finished. [frames] is the whole strip either way: a retake carries the
     * untouched frames through and replaces only its own slot (CaptureViewModel.onStartRetake),
     * so this never appends or shifts - the same "always the same slot index" invariant the
     * capture loop enforces.
     */
    fun onSessionComplete(frames: List<CaptureFrame>) {
        _sessionFrames.value = frames
    }

    /** Hand one retaken frame back to the Preview entry sitting underneath Capture. */
    fun publishRetakeResult(slotIndex: Int, frame: CaptureFrame) {
        _retakeResult.value = slotIndex to frame
    }

    /** Preview has applied the retaken frame; clear it so it is applied exactly once. */
    fun consumeRetakeResult() {
        _retakeResult.value = null
    }
}
