package com.dj.photobooth

import androidx.compose.ui.window.ComposeUIViewController
import com.dj.photobooth.camera.IosCameraController
import com.dj.photobooth.export.IosMediaRepo
import com.dj.photobooth.export.IosShareSheet
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.gallery.IosAppDatabaseFactory
import com.dj.photobooth.gallery.RoomGalleryRepo
import platform.UIKit.UIViewController

// iOS counterpart of PhotoboothApplication's `by lazy` database: MainViewController can be
// called more than once over a process's life, and each call must not build another Room
// instance (one connection pool per call, none of them closed). A file-level `by lazy` is the
// Kotlin/Native equivalent of an application-scoped singleton here - there's no Application
// class to hang it off.
private val galleryRepo: GalleryRepo by lazy { RoomGalleryRepo(IosAppDatabaseFactory().create()) }

// Mirrors MainActivity.kt's job on the other platform: host the shared App() composable
// behind the minimal platform-specific glue iOS requires. The Xcode wrapper project that
// calls this (architecture.md's iosApp/) isn't built yet - iOS bring-up is Phase 4,
// blocked on Mac/cloud-CI access (CLAUDE.md § Known blockers) - so this can't be compiled
// or run from this (Windows) machine. It's included now purely so the commonMain code
// already targets three source sets consistently, matching the module structure in
// architecture.md, rather than iosMain being added later as an afterthought.
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(
        cameraController = IosCameraController(),
        galleryRepo = galleryRepo,
        mediaRepo = IosMediaRepo(),
        shareSheet = IosShareSheet(),
    )
}
