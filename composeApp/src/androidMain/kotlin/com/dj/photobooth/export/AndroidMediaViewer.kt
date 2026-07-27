package com.dj.photobooth.export

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opens a saved strip in whichever gallery app the user has, via an `ACTION_VIEW` chooser.
 *
 * [mediaPath] is a `content://` MediaStore URI (exactly what [AndroidMediaRepo.savePng]
 * returns), so `FLAG_GRANT_READ_URI_PERMISSION` is enough for the target app to read it - no
 * FileProvider needed, same reasoning as [AndroidShareSheet].
 *
 * Takes a [ComponentActivity] rather than a plain Context so the viewer opens as a normal
 * forward navigation from this app instead of needing `FLAG_ACTIVITY_NEW_TASK` (which would
 * push it into its own task and break back-navigation).
 */
class AndroidMediaViewer(private val activity: ComponentActivity) : MediaViewer {

    override suspend fun openImage(mediaPath: String) = withContext(Dispatchers.Main) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(mediaPath), "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Chooser rather than a bare startActivity: a device with no image viewer at all would
        // throw ActivityNotFoundException and crash; createChooser degrades to a "no apps can
        // perform this action" dialog instead.
        activity.startActivity(Intent.createChooser(viewIntent, "Open strip"))
    }
}
