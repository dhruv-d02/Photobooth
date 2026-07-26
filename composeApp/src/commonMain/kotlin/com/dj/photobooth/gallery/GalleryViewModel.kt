package com.dj.photobooth.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.photobooth.export.ShareSheet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel for the Gallery screen. Thin by design - [GalleryRepo.entries] is already a
 * reactive, newest-first, uncapped Flow (CLAUDE.md: no eviction limit, deletion is manual
 * only), so this just reshapes it into [GalleryUiState] and forwards the two user actions.
 */
class GalleryViewModel(
    private val repo: GalleryRepo,
    private val shareSheet: ShareSheet? = null,
) : ViewModel() {

    // Eagerly, not WhileSubscribed - this ViewModel exists solely to mirror the repo, so there's
    // no cost tradeoff in deferring collection until a UI subscriber shows up (unlike a flow
    // shared across many consumers), and Eagerly means uiState.value is always current even
    // before the screen (or a test) has started collecting it.
    val uiState: StateFlow<GalleryUiState> = repo.entries
        .map { GalleryUiState(entries = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState())

    /** Manual deletion only - see [GalleryRepo.delete]'s doc comment. */
    fun onDelete(entry: HistoryEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    /** The per-card "SAVE" link (design/handoff/README.md §4) - re-shares an already-archived
     *  strip, since it's already saved to photo storage; no-op if no [ShareSheet] was wired
     *  in (e.g. this screen is exercised standalone/in tests without one). */
    fun onShare(entry: HistoryEntry) {
        val sheet = shareSheet ?: return
        viewModelScope.launch { sheet.shareImage(entry.finalImagePath, "photobooth-strip") }
    }
}
