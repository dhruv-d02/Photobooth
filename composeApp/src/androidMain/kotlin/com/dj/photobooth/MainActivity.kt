package com.dj.photobooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dj.photobooth.camera.AndroidCameraController

// The only thing Android-specific about launching this app is the Activity itself, plus
// constructing AndroidCameraController (it needs the ComponentActivity to register the
// camera-permission launcher, which must happen before setContent/STARTED) - everything
// rendered (App()) lives in commonMain and is identical on iOS. Keeping this class down to
// "host the shared Composable" is what the commonMain-first convention in CLAUDE.md means
// in practice.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cameraController = AndroidCameraController(this)
        enableEdgeToEdge()
        setContent {
            App(cameraController)
        }
    }
}