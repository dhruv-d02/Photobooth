package com.dj.photobooth.export

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Photos-framework-backed [MediaRepo] - architecture.md's `MediaRepo --> PhotosImpl`. Phase 4
 * territory (CLAUDE.md known blockers: iOS builds/testing need a Mac, not available yet).
 * Best-effort skeleton written against PHAssetCreationRequest's documented "add raw image
 * data directly" API (no temp file needed) - compiles as Kotlin/Native source, matching
 * IosCameraController.kt's status, but has never been built or run (Kotlin/Native can only
 * compile Apple targets on macOS). Treat as unverified until Phase 4.
 *
 * TODO(phase-4, needs Mac/cloud-CI): request NSPhotoLibraryAddUsageDescription-backed
 * add-only authorization explicitly before calling [savePng] (this assumes it's already been
 * granted - see [hasAddOnlyAuthorization]) and return a real, stable identifier (e.g. the
 * created PHAsset's localIdentifier) instead of just echoing back [displayName], once there's
 * a way to verify PHAssetCreationRequest's actual return/placeholder behaviour against a real
 * Photos library.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMediaRepo : MediaRepo {

    override suspend fun savePng(pngBytes: ByteArray, displayName: String): String =
        suspendCancellableCoroutine { continuation ->
            val data = pngBytes.toNSData()
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                val request = PHAssetCreationRequest.creationRequestForAsset()
                request.addResourceWithType(PHAssetResourceTypePhoto, data = data, options = null)
            }) { success, error ->
                if (success) {
                    continuation.resume(displayName)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Photos save failed: ${error?.localizedDescription}")
                    )
                }
            }
        }

    /**
     * TODO(phase-4, needs Mac/cloud-CI): unimplementable until [savePng] returns a real
     * PHAsset localIdentifier rather than echoing [displayName] back (see this class's own
     * TODO) - without that there's no way to resolve [sourcePath] to an asset whose image
     * data could be re-saved. Throws rather than silently no-op'ing so the gap is loud when
     * iOS bring-up starts, matching how the rest of iosMain is deliberately unverified.
     */
    // UnsupportedOperationException, not NotImplementedError/TODO(): callers catch Exception
    // (see GalleryViewModel.onSaveCopy), and NotImplementedError extends Error - it would slip
    // straight past that catch and crash the app rather than surfacing "save failed".
    override suspend fun copyToDevice(sourcePath: String, displayName: String): String =
        throw UnsupportedOperationException(
            "iOS copyToDevice needs IosMediaRepo.savePng to return a real PHAsset identifier first"
        )

    /** Photos-library authorization is a prerequisite for [savePng] - surfaced separately so
     *  a future caller can gate the SAVE PNG button on it, same shape as
     *  CameraController.hasCameraPermission. */
    fun hasAddOnlyAuthorization(): Boolean =
        PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly) == PHAuthorizationStatusAuthorized
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = this.size.toULong())
}
