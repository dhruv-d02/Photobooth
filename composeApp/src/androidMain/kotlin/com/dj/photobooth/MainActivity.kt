package com.dj.photobooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// The only thing Android-specific about launching this app is the Activity itself -
// everything it renders (App()) lives in commonMain and is identical on iOS. Keeping
// this class down to "host the shared Composable" is what the commonMain-first
// convention in CLAUDE.md means in practice.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}