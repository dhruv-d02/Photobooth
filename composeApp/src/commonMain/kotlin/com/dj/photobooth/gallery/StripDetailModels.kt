package com.dj.photobooth.gallery

/**
 * UI state for the Strip Detail screen - the in-app viewer opened by tapping a card in the
 * Gallery grid. Mirrors GalleryUiState/StripPreviewUiState's shape: one immutable data class,
 * one StateFlow, the screen only reads it.
 *
 * [entry] is null both before the backing repo flow has emitted at least once and after this
 * strip has been deleted - [loaded] and [deleted] are what tell those two "still nothing" cases
 * apart from each other and from [notFound].
 */
data class StripDetailUiState(
    val entry: HistoryEntry? = null,
    val loaded: Boolean = false,
    val confirmingDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val message: String? = null,
) {
    /** Distinguishes "still loading" from "no entry with this id exists" - [deleted] is
     *  excluded deliberately: the row disappears from the repo the instant this screen's own
     *  delete succeeds, and without this guard that brief window (before the nav layer's
     *  popBackStack effect actually fires) would flash a "not found" error onto a screen
     *  that's already on its way out. */
    val notFound: Boolean get() = loaded && entry == null && !deleted
}
