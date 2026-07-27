package com.dj.photobooth.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore-backed [MediaRepo] - architecture.md's `MediaRepo --> MediaStoreImpl`. Writes
 * into the shared Pictures/Photobooth collection via scoped storage (ContentResolver +
 * MediaStore.Images), matching architecture.md's "no broad storage permission needed on API
 * 26+" claim: on API 29+ scoped storage needs no permission at all; API 26-28 needs
 * WRITE_EXTERNAL_STORAGE, declared in AndroidManifest.xml with maxSdkVersion="28" so it's
 * never requested on the modern OS versions this app actually targets going forward.
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
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Photobooth")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = requireNotNull(resolver.insert(collection, values)) {
                "MediaStore.insert() returned null - unable to create a new image entry"
            }
            resolver.openOutputStream(uri)?.use { it.write(pngBytes) }
                ?: error("Unable to open an output stream for $uri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
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
