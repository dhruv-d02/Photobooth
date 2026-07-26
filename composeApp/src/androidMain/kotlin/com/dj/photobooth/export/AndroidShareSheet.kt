package com.dj.photobooth.export

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android share Intent chooser - architecture.md's `ShareIface --> AndroidShare`.
 * [mediaPath] is expected to be a `content://` URI string, i.e. exactly what
 * [AndroidMediaRepo.savePng] returns - since the image already lives in MediaStore, the
 * MediaStore content provider grants read access to the chosen target app itself, no
 * FileProvider is needed here (that's only required for files the app owns privately).
 *
 * Takes a [ComponentActivity] (not a plain Context) so starting the chooser doesn't need
 * FLAG_ACTIVITY_NEW_TASK, mirroring why AndroidCameraController needs a ComponentActivity too.
 */
class AndroidShareSheet(private val activity: ComponentActivity) : ShareSheet {

    override suspend fun shareImage(mediaPath: String, displayName: String) = withContext(Dispatchers.Main) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaPath))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(sendIntent, displayName))
    }
}
