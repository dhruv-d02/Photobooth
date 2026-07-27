package com.dj.photobooth.export

/**
 * Hands an already-saved strip off to the platform's own photo viewer - tapping a card in the
 * Gallery ("Past strips") grid opens that image in the system gallery app rather than in a
 * screen this app has to build and maintain itself.
 *
 * Same pattern as [MediaRepo] and [ShareSheet]: a plain commonMain interface, not
 * expect/actual, implemented per-platform (Android `ACTION_VIEW` chooser, iOS Photos app) and
 * constructed at the platform entry point.
 */
interface MediaViewer {
    /** Opens the image previously saved at [mediaPath] (the string [MediaRepo.savePng]
     *  returned, stored as [com.dj.photobooth.gallery.HistoryEntry.finalImagePath]). */
    suspend fun openImage(mediaPath: String)
}
