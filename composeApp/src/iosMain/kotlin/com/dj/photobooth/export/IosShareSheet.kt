package com.dj.photobooth.export

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * UIActivityViewController-backed [ShareSheet] - architecture.md's `ShareIface --> IOSShare`.
 * Phase 4 territory, same status as IosCameraController.kt/IosMediaRepo.kt: compiles as
 * Kotlin/Native source but has never been built or run. Treat as unverified until Phase 4.
 *
 * [mediaPath] is whatever [IosMediaRepo.savePng] returned - since that's currently just the
 * display name (see its TODO), this can't yet resolve back to a shareable asset URL; sharing
 * the raw PNG bytes directly (rather than round-tripping through a saved-asset lookup) is the
 * likely real fix, deferred to the same Phase-4 pass as that TODO.
 */
class IosShareSheet : ShareSheet {

    override suspend fun shareImage(mediaPath: String, displayName: String) {
        val activityController = UIActivityViewController(
            activityItems = listOf(mediaPath),
            applicationActivities = null,
        )
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(activityController, animated = true, completion = null)
    }
}
