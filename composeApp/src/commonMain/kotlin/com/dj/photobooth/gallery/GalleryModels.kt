package com.dj.photobooth.gallery

/**
 * UI state for the Gallery ("Past strips") screen (design/handoff/README.md §4). Uncapped,
 * newest-first, straight from [GalleryRepo.entries] - see GalleryViewModel.
 */
data class GalleryUiState(val entries: List<HistoryEntry> = emptyList()) {
    val isEmpty: Boolean get() = entries.isEmpty()

    /** Zero-padded strip count for the "Strips NN" tab label (design/handoff/README.md line 47). */
    val countLabel: String get() = entries.size.toString().padStart(2, '0')
}
