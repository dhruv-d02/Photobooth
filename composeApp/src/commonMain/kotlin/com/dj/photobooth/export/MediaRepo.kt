package com.dj.photobooth.export

/**
 * Saves a finished composite strip PNG to platform photo storage - architecture.md's
 * `MediaRepo --> MediaStoreImpl` / `MediaRepo --> PhotosImpl`. A plain commonMain interface
 * rather than expect/actual: like CameraController, Android's write path (MediaStore
 * ContentValues via ContentResolver) and iOS's (PHPhotoLibrary change requests) are different
 * enough that no shared implementation is possible, only the interface is shared - each
 * platform's concrete class (AndroidMediaRepo, IosMediaRepo) is constructed at the platform
 * entry point and threaded down, same as CameraController.
 */
interface MediaRepo {
    /**
     * Persists [pngBytes] as a new image named [displayName] (no extension) into the
     * platform's shared photo storage (Android: Pictures/<app>, via MediaStore; iOS: the
     * Photos library). Returns a platform path/URI string suitable for
     * [com.dj.photobooth.gallery.HistoryEntry.finalImagePath].
     */
    suspend fun savePng(pngBytes: ByteArray, displayName: String): String
}
