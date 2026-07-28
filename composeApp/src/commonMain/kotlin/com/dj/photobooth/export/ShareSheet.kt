package com.dj.photobooth.export

/**
 * Native share sheet - architecture.md's `ShareIface --> AndroidShare` / `ShareIface -->
 * IOSShare`. Same pattern as [MediaRepo]: a plain commonMain interface, not expect/actual,
 * implemented per-platform (Android share Intent chooser, iOS UIActivityViewController).
 */
interface ShareSheet {
    /** Opens the platform share UI for the image previously saved at [mediaPath] (the string
     *  [MediaRepo.savePng] returned). */
    suspend fun shareImage(mediaPath: String, displayName: String)
}
