package com.dj.photobooth.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opens a saved strip in whichever gallery app the user has, via an `ACTION_VIEW` chooser.
 *
 * [mediaPath] is a `content://` MediaStore URI (exactly what [AndroidMediaRepo.savePng]
 * returns), so `FLAG_GRANT_READ_URI_PERMISSION` is enough for the target app to read it - no
 * FileProvider needed, same reasoning as [AndroidShareSheet].
 *
 * Holds `applicationContext`, never an Activity, and pays for that with
 * `FLAG_ACTIVITY_NEW_TASK` (required when launching from a non-Activity context). An Activity
 * reference would be actively wrong here: this object is reached through ViewModels scoped to
 * a NavBackStackEntry, whose ViewModelStore *survives* configuration changes - so a captured
 * Activity would be the destroyed pre-rotation one, and `startActivity` on it would either
 * leak it for the life of the process or fail outright.
 */
class AndroidMediaViewer(context: Context) : MediaViewer {

    private val context = context.applicationContext

    override suspend fun openImage(mediaPath: String) = withContext(Dispatchers.Main) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(mediaPath), "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Chooser rather than a bare startActivity: a device with no image viewer at all would
        // throw ActivityNotFoundException and crash; createChooser degrades to a "no apps can
        // perform this action" dialog instead.
        val chooser = Intent.createChooser(viewIntent, "Open strip").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
