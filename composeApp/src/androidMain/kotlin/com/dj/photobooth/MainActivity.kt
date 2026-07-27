package com.dj.photobooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dj.photobooth.camera.AndroidCameraController
import com.dj.photobooth.export.AndroidMediaRepo
import com.dj.photobooth.export.AndroidMediaViewer
import com.dj.photobooth.export.AndroidShareSheet

// The only thing Android-specific about launching this app is the Activity itself, plus
// constructing the platform implementations App() needs (camera, media store, share sheet,
// gallery viewer) - everything rendered (App()) lives in commonMain and is identical on iOS.
// Keeping this class down to "host the shared Composable" is what the commonMain-first
// convention in CLAUDE.md means in practice.
//
// Note what is NOT built here: the database/GalleryRepo. Those are process-wide singletons
// owned by PhotoboothApplication, because onCreate runs again on every configuration change
// and rebuilding a RoomDatabase per rotation leaks a connection pool each time.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cameraController = AndroidCameraController(this)
        val galleryRepo = (application as PhotoboothApplication).galleryRepo
        val mediaRepo = AndroidMediaRepo(this)
        // These two genuinely need the Activity (startActivity without NEW_TASK), so they are
        // Activity-scoped by necessity - safe now that the ViewModels holding them are
        // obtained via viewModel() and therefore actually cleared.
        val shareSheet = AndroidShareSheet(this)
        val mediaViewer = AndroidMediaViewer(this)
        enableEdgeToEdge()
        setContent {
            App(
                cameraController = cameraController,
                galleryRepo = galleryRepo,
                mediaRepo = mediaRepo,
                shareSheet = shareSheet,
                mediaViewer = mediaViewer,
            )
        }
    }
}
