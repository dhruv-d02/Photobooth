package com.dj.photobooth.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.ShareSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel for the Strip Detail screen - the in-app viewer opened by tapping a card in
 * the Gallery grid, replacing the previous hand-off to the platform's own photo viewer (see
 * the retired MediaViewer interface).
 *
 * [entryId] identifies which [HistoryEntry] to show and is looked up reactively from [repo]
 * rather than the whole entry being passed in - Navigation-Compose route args are
 * strings/primitives only (see Route.StripDetail's doc comment), and looking it up from the
 * same live flow the Gallery grid observes means a delete is reflected here the same way,
 * through one path, rather than needing a separate "did this change under me" check.
 *
 * [shareSheet] is nullable for the same reason StripPreviewViewModel's is - the screen can be
 * exercised standalone or in tests without a platform share sheet wired in.
 */
class StripDetailViewModel(
    private val entryId: Long,
    private val repo: GalleryRepo,
    private val mediaRepo: MediaRepo,
    private val shareSheet: ShareSheet? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StripDetailUiState())
    val uiState: StateFlow<StripDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.entries.collect { list ->
                _uiState.update { it.copy(entry = list.find { candidate -> candidate.id == entryId }, loaded = true) }
            }
        }
    }

    /** SHARE: hands the strip to the platform share sheet - same shape as
     *  StripPreviewViewModel.onShare, including the lack of explicit error handling: a share
     *  sheet failing to open isn't something this app can usefully recover from or explain. */
    fun onShare() {
        val entry = _uiState.value.entry ?: return
        val sheet = shareSheet ?: return
        viewModelScope.launch { sheet.shareImage(entry.finalImagePath, "photobooth-strip") }
    }

    /** DELETE tapped once: arm the confirmation, don't delete yet. This is a destructive,
     *  irreversible action (uncapped, local-only history - CLAUDE.md forbids automatic
     *  eviction, but says nothing about protecting a manual one), so it gets an explicit
     *  second step rather than firing on the first tap. */
    fun onDeleteRequested() {
        _uiState.update { it.copy(confirmingDelete = true) }
    }

    fun onDeleteCancelled() {
        _uiState.update { it.copy(confirmingDelete = false) }
    }

    /**
     * Confirmed: remove the archive row first - that's what makes the strip disappear from the
     * Gallery grid, the user-visible meaning of "deleted" - then best-effort clean up the
     * underlying file (see MediaRepo.delete's doc comment for why a row-only delete would
     * orphan it). Ordered deliberately: if the file delete ran first and then the row delete
     * failed, a HistoryEntry would keep pointing at a file that no longer exists - a
     * permanently broken card. Doing the row first leaves only a harmless orphaned file as the
     * failure mode, not a broken one.
     */
    fun onDeleteConfirmed() {
        val entry = _uiState.value.entry ?: return
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                repo.delete(entry)
                runCatching { mediaRepo.delete(entry.finalImagePath) }
                _uiState.update { it.copy(isDeleting = false, confirmingDelete = false, deleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, message = e.message ?: "couldn't delete this strip") }
            }
        }
    }
}
