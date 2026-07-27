package com.dj.photobooth.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.ImageBitmap
import com.dj.photobooth.capture.CaptureFrame
import com.dj.photobooth.compose.decodeJpegToImageBitmap
import com.dj.photobooth.compose.encodeImageBitmapToPng
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.ShareSheet
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripCompositor
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.gallery.HistoryEntry
import com.dj.photobooth.gallery.currentDateStamp
import com.dj.photobooth.gallery.currentEpochMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MVVM ViewModel for the Strip Preview & Customise screen (design/handoff/README.md §3),
 * mirroring CaptureViewModel's shape: all logic lives here, the View (StripPreviewScreen)
 * only reads [uiState] and calls the on*() methods.
 *
 * [initialFrames] must be the fully-accepted frame list from a completed CaptureSession (one
 * non-null [CaptureFrame] per slot, in slot order) - this screen is only reachable once every
 * frame in a session has been accepted (architecture.md's nav graph: "Capture -->|all frames
 * accepted| Preview"), so a sparse list isn't a call site this class needs to support.
 *
 * Per-cell retake (RETAKE 0N) is only *modeled* here, not driven end-to-end: actually
 * re-entering the capture loop for one slot needs CaptureViewModel + navigation, which are a
 * separate branch's concern (see this module's CLAUDE.md/hard-boundary notes). [replaceFrame]
 * is the seam a future nav layer calls once a retake completes, and it enforces the same
 * "same slot index, never append/shift" invariant CaptureViewModel.onKeep/onShootAgain already
 * enforce for the main capture loop.
 *
 * [workDispatcher] is where JPEG decode / Skia compositing / PNG encode actually run
 * (Dispatchers.Default by default, off the main/Compose thread per architecture.md's
 * "Performance" non-functional note) - injectable so tests can pass the same single test
 * dispatcher used for viewModelScope, making the whole decode-then-compose pipeline advance
 * deterministically under runTest's virtual clock instead of racing a real background thread.
 */
class StripPreviewViewModel(
    initialFrames: List<CaptureFrame>,
    private val galleryRepo: GalleryRepo,
    private val mediaRepo: MediaRepo,
    private val shareSheet: ShareSheet? = null,
    brand: String = "Photobooth",
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private var rawFrames: List<CaptureFrame> = initialFrames

    private val _uiState = MutableStateFlow(
        StripPreviewUiState(
            decodedFrames = List(initialFrames.size) { null },
            stamp = currentDateStamp(),
            brand = brand,
        )
    )
    val uiState: StateFlow<StripPreviewUiState> = _uiState.asStateFlow()

    // Tracks the in-flight decode-then-compose pipeline so a rapid-fire style change (or a
    // retake landing mid-decode) cancels the stale run instead of letting two composes race
    // and possibly finish out of order - same reasoning as CaptureViewModel.launchSessionWork.
    private var pipelineJob: Job? = null

    init {
        runPipeline()
    }

    fun onLayoutChange(layout: StripLayout) {
        if (_uiState.value.layout == layout) return
        _uiState.update { it.copy(layout = layout, saved = false) }
        recomposeOnly()
    }

    fun onTreatmentChange(treatment: FilmTreatment) {
        if (_uiState.value.treatment == treatment) return
        _uiState.update { it.copy(treatment = treatment, saved = false) }
        recomposeOnly()
    }

    fun onFrameColorChange(frameColor: FrameColorPreset) {
        if (_uiState.value.frameColor == frameColor) return
        _uiState.update { it.copy(frameColor = frameColor, saved = false) }
        recomposeOnly()
    }

    /** See class doc - the seam a future nav layer calls once a per-cell RETAKE (or the
     *  full-session RESHOOT, applied per-slot) actually completes in Capture. */
    fun replaceFrame(index: Int, frame: CaptureFrame) {
        require(index in rawFrames.indices) { "slot $index out of range for ${rawFrames.size} frames" }
        rawFrames = rawFrames.toMutableList().also { it[index] = frame }
        _uiState.update { it.copy(saved = false, savedPath = null) }
        runPipeline()
    }

    fun onSavePng() {
        val composed = _uiState.value.composedImage ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val pngBytes = withContext(workDispatcher) { encodeImageBitmapToPng(composed) }
                val displayName = "photobooth-${currentEpochMillis()}"
                val savedPath = mediaRepo.savePng(pngBytes, displayName)
                galleryRepo.save(
                    HistoryEntry(
                        finalImagePath = savedPath,
                        // TODO(follow-up): generate a real downsampled thumbnail
                        // (architecture.md's "Performance" non-functional note) instead of
                        // reusing the full-res path - deferred, not a functional blocker for
                        // the Gallery grid, which can decode the full image meanwhile.
                        thumbnailPath = savedPath,
                        filmTreatmentId = _uiState.value.treatment.code,
                        createdAt = currentEpochMillis(),
                        stamp = _uiState.value.stamp,
                    )
                )
                _uiState.update { it.copy(isSaving = false, saved = true, savedPath = savedPath) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message ?: "Save failed") }
            }
        }
    }

    /** Native share sheet, optional per design/handoff/README.md's sequence diagram ("Prev->U:
     *  native share sheet (optional)") - only meaningful once a save has actually produced a
     *  path, and only on platforms that wired a [ShareSheet] in. */
    fun onShare() {
        val path = _uiState.value.savedPath ?: return
        val sheet = shareSheet ?: return
        viewModelScope.launch { sheet.shareImage(path, "photobooth-strip") }
    }

    private fun runPipeline() {
        pipelineJob?.cancel()
        pipelineJob = viewModelScope.launch {
            val decoded = withContext(workDispatcher) { rawFrames.map(::decodeFrame) }
            _uiState.update { it.copy(decodedFrames = decoded) }
            compose(decoded)
        }
    }

    private fun recomposeOnly() {
        val frames = _uiState.value.decodedFrames
        // A decode is still in flight (e.g. right after construction, or right after
        // replaceFrame): don't cancel it here, just let runPipeline's own compose() call at
        // the end of that decode pick up the style change that was just written to uiState.
        if (frames.any { it == null }) return
        pipelineJob?.cancel()
        pipelineJob = viewModelScope.launch { compose(frames) }
    }

    private suspend fun compose(decoded: List<ImageBitmap?>) {
        if (decoded.isEmpty() || decoded.any { it == null }) return
        @Suppress("UNCHECKED_CAST")
        val frames = decoded as List<ImageBitmap>
        val state = _uiState.value
        _uiState.update { it.copy(isComposing = true) }
        val composed = withContext(workDispatcher) {
            StripCompositor.compose(frames, state.treatment, state.frameColor, state.layout)
        }
        _uiState.update { it.copy(composedImage = composed, isComposing = false) }
    }

    private fun decodeFrame(frame: CaptureFrame): ImageBitmap {
        if (frame.isPlaceholder) return placeholderFrameBitmap()
        return try {
            decodeJpegToImageBitmap(frame.jpegBytes)
        } catch (e: Exception) {
            placeholderFrameBitmap()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pipelineJob?.cancel()
    }
}
