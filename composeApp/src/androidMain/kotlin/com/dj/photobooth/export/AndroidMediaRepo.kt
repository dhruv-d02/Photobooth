package com.dj.photobooth.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore-backed [MediaRepo] - architecture.md's `MediaRepo --> MediaStoreImpl`. Writes
 * into the shared Pictures/Photobooth collection via scoped storage (ContentResolver +
 * MediaStore.Images).
 *
 * No broad storage permission is needed here, and the reason is specifically **scoped storage
 * on API 29+** (not API 26+, as architecture.md still claims): from Android 10 an app may
 * insert its own row into the shared media collection and write the bytes through the
 * resolver-owned URI it gets back, because the row belongs to this app until it's published.
 * The IS_PENDING = 1 -> write -> IS_PENDING = 0 handshake below is what makes that safe: the
 * half-written file stays hidden from other apps' gallery queries until the bytes are on disk.
 *
 * On API 26-28 none of that exists - the same insert() requires the WRITE_EXTERNAL_STORAGE
 * *runtime* permission (a manifest declaration alone throws SecurityException), which this app
 * never requests. That's why the module's minSdk is 29 rather than 26; see the comment on
 * `minSdk` in composeApp/build.gradle.kts. Because 29 is the floor, every API-level guard that
 * used to wrap the scoped-storage calls is unconditionally true and has been removed.
 */
class AndroidMediaRepo(context: Context) : MediaRepo {

    // applicationContext, not the passed-in Context: this outlives any single Activity (it's
    // held by ViewModels across configuration changes), so holding an Activity here would leak
    // it on every rotation. Nothing below needs Activity-scoped state anyway.
    private val context = context.applicationContext

    override suspend fun savePng(pngBytes: ByteArray, displayName: String): String =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            // RELATIVE_PATH and IS_PENDING are both API 29+ columns, and minSdk is 29, so they
            // are set unconditionally - no SDK_INT guard needed. IS_PENDING = 1 marks the row
            // as "not finished yet" so gallery apps skip it until the bytes are written.
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Photobooth")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = requireNotNull(resolver.insert(collection, values)) {
                "MediaStore.insert() returned null - unable to create a new image entry"
            }

            // Everything after the insert is wrapped so a failed write can't leave the row
            // behind. A row stuck at IS_PENDING = 1 is invisible to gallery apps but still
            // occupies a real MediaStore entry (and its reserved file) forever - and since the
            // user's natural response to a "save failed" message is to hit Save again, every
            // retry would leak another one. Cleanup on failure keeps retries idempotent.
            try {
                resolver.openOutputStream(uri)?.use { it.write(pngBytes) }
                    ?: error("Unable to open an output stream for $uri")

                // Publish: clearing IS_PENDING is what makes the image appear in Photos/Gallery.
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (t: Throwable) {
                // Throwable, not Exception, so structured cancellation (CancellationException -
                // e.g. the caller's viewModelScope dying mid-save) also cleans up its orphan
                // row rather than leaking one on every abandoned save.
                //
                // The delete is itself best-effort: if it fails (revoked access, resolver
                // already gone) that secondary failure must not replace the real cause of the
                // save failing, which is what the user needs to see.
                runCatching { resolver.delete(uri, null, null) }
                // Rethrow unchanged rather than swallowing. StripPreviewViewModel.save() and
                // GalleryViewModel catch this to surface a user-visible save error, and both
                // use the codebase's `catch (CancellationException) { throw e }` pattern - so
                // rethrowing the original Throwable leaves cancellation as cancellation and
                // leaves real failures actionable.
                throw t
            }

            uri.toString()
        }

    override suspend fun copyToDevice(sourcePath: String, displayName: String): String =
        withContext(Dispatchers.IO) {
            // Straight byte copy rather than decode-to-Bitmap-then-re-encode: the source is
            // already a PNG we wrote ourselves, so re-encoding would burn memory and CPU to
            // produce a (lossily re-compressed) equivalent of bytes we can just read.
            val bytes = context.contentResolver.openInputStream(Uri.parse(sourcePath))?.use {
                it.readBytes()
            } ?: error("Unable to read the archived strip at $sourcePath")
            savePng(bytes, displayName)
        }
}
