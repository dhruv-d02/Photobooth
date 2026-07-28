package com.dj.photobooth.gallery

/**
 * UI state for the Gallery ("Past strips") screen (design/handoff/README.md §4). Uncapped,
 * newest-first, straight from [GalleryRepo.entries] - see GalleryViewModel.
 *
 * [message] is transient one-line feedback for the per-card SAVE action, cleared on a timer by
 * the ViewModel; null the rest of the time.
 *
 * No zero-padded `countLabel` here: the "STRIPS NN" tab label is the only thing that renders a
 * padded count, and it lives in BottomTabBar - having a second copy of the same padStart in
 * this class just meant two places to keep in sync.
 */
data class GalleryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val message: String? = null,
) {
    val isEmpty: Boolean get() = entries.isEmpty()
}
