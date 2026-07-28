package com.dj.photobooth.export

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Opens a saved strip in the iOS Photos app. Phase 4 territory, same status as
 * IosCameraController.kt / IosMediaRepo.kt / IosShareSheet.kt: compiles as Kotlin/Native
 * source but has never been built or run (no Mac available - CLAUDE.md § Known blockers).
 * Treat as unverified until Phase 4.
 *
 * TODO(phase-4, needs Mac/cloud-CI): this depends on [IosMediaRepo.savePng] returning a real
 * asset identifier - it currently echoes back the display name (see that class's own TODO), so
 * the `photos-redirect://` deep link below has nothing meaningful to point at yet. Fixing that
 * TODO is a prerequisite for this one.
 */
class IosMediaViewer : MediaViewer {

    override suspend fun openImage(mediaPath: String) {
        // photos-redirect:// is the documented scheme for handing off to Photos; a plain
        // asset URL isn't openable by UIApplication directly.
        val url = NSURL(string = "photos-redirect://$mediaPath")
        UIApplication.sharedApplication.openURL(url)
    }
}
