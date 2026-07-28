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
        // These three take applicationContext internally, deliberately. They end up held by
        // ViewModels scoped to a NavBackStackEntry, and that store survives configuration
        // changes - so an Activity captured here would still be the destroyed pre-rotation one
        // the next time the user taps share/open.
        //
        // AndroidCameraController above is the exception: it genuinely must hold *this*
        // Activity, because registerForActivityResult needs an ActivityResultRegistry owner and
        // the CameraX preview binds to this Activity's lifecycle. That makes the instance built
        // here valid only until the next configuration change, which is why CaptureViewModel
        // does not keep the controller it was constructed with - PhotoboothNavHost re-points it
        // at the current instance on every composition pass, via
        // CaptureViewModel.onCameraControllerChanged. Without that, the surviving ViewModel
        // would drive a destroyed Activity's unbound ImageCapture and silently emit blank frames.
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
