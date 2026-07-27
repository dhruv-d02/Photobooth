package com.dj.photobooth.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.MediaViewer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel for the Gallery screen. Mostly a thin reshape of [GalleryRepo.entries], which
 * is already a reactive, newest-first, uncapped Flow (CLAUDE.md: no eviction limit, deletion
 * is manual only), plus the two per-card actions.
 *
 * [mediaViewer] is nullable so the screen can be exercised standalone (or in tests) without a
 * platform viewer wired in - same shape as the previously-optional ShareSheet.
 */
class GalleryViewModel(
    private val repo: GalleryRepo,
    private val mediaRepo: MediaRepo,
    private val mediaViewer: MediaViewer? = null,
) : ViewModel() {

    // Transient one-line feedback for SAVE (design has no toast/snackbar component, and a
    // save that reports nothing at all reads as a dead button). Cleared on a timer by
    // [onSaveCopy] rather than by the View, so the state stays fully owned here.
    private val transientMessage = MutableStateFlow<String?>(null)

    // Eagerly, not WhileSubscribed - this ViewModel exists solely to mirror the repo, so there's
    // no cost tradeoff in deferring collection until a UI subscriber shows up (unlike a flow
    // shared across many consumers), and Eagerly means uiState.value is always current even
    // before the screen (or a test) has started collecting it.
    val uiState: StateFlow<GalleryUiState> = repo.entries
        .combine(transientMessage) { entries, message ->
            GalleryUiState(entries = entries, message = message)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState())

    /** Manual deletion only - see [GalleryRepo.delete]'s doc comment. No UI surfaces this yet
     *  (the design specifies no delete affordance); kept so the capability exists the moment
     *  one is designed. */
    fun onDelete(entry: HistoryEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    /**
     * The per-card `SAVE` link (design/handoff/README.md §4) - writes another copy of this
     * strip into device photo storage. Note the archived strip is already on the device (it
     * got there via StripPreviewViewModel.onSavePng, which is what created the archive row),
     * so this deliberately produces a second file rather than being a first-time export.
     */
    fun onSaveCopy(entry: HistoryEntry) {
        viewModelScope.launch {
            try {
                mediaRepo.copyToDevice(entry.finalImagePath, "photobooth-${currentEpochMillis()}")
                showTransiently("saved another copy to photos")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showTransiently(e.message ?: "save failed")
            }
        }
    }

    /** Tapping a card hands the image to the platform's own gallery app rather than opening a
     *  viewer screen this app would have to build - see [MediaViewer]. */
    fun onOpen(entry: HistoryEntry) {
        val viewer = mediaViewer ?: return
        viewModelScope.launch {
            try {
                viewer.openImage(entry.finalImagePath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showTransiently("couldn't open this strip")
            }
        }
    }

    private suspend fun showTransiently(message: String) {
        transientMessage.value = message
        delay(TRANSIENT_MESSAGE_MS)
        // Only clear if nothing newer replaced it, so a second action's message isn't wiped
        // early by the first action's timer.
        if (transientMessage.value == message) transientMessage.value = null
    }

    private companion object {
        const val TRANSIENT_MESSAGE_MS = 2_500L
    }
}
