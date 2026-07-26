package com.dj.photobooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dj.photobooth.camera.AndroidCameraController
import com.dj.photobooth.export.AndroidMediaRepo
import com.dj.photobooth.export.AndroidShareSheet
import com.dj.photobooth.gallery.AndroidAppDatabaseFactory
import com.dj.photobooth.gallery.RoomGalleryRepo

// The only thing Android-specific about launching this app is the Activity itself, plus
// constructing the platform implementations App() needs (camera, database, media store,
// share sheet) - everything rendered (App()) lives in commonMain and is identical on iOS.
// Keeping this class down to "host the shared Composable" is what the commonMain-first
// convention in CLAUDE.md means in practice.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cameraController = AndroidCameraController(this)
        val database = AndroidAppDatabaseFactory(this).create()
        val galleryRepo = RoomGalleryRepo(database)
        val mediaRepo = AndroidMediaRepo(this)
        val shareSheet = AndroidShareSheet(this)
        enableEdgeToEdge()
        setContent {
            App(
                cameraController = cameraController,
                galleryRepo = galleryRepo,
                mediaRepo = mediaRepo,
                shareSheet = shareSheet,
            )
        }
    }
}