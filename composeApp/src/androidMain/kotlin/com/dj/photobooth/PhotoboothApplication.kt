package com.dj.photobooth

import android.app.Application
import com.dj.photobooth.gallery.AndroidAppDatabaseFactory
import com.dj.photobooth.gallery.AppDatabase
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.gallery.RoomGalleryRepo
import com.google.android.gms.ads.MobileAds

/**
 * Owns the process-wide singletons that must outlive any single Activity.
 *
 * The database in particular: [android.app.Activity.onCreate] runs again on every
 * configuration change (rotation, theme switch, ...), so building [AppDatabase] there meant a
 * fresh connection pool per rotation with the previous one never closed. Room instances are
 * designed to be application-scoped singletons - one per process, shared by everything.
 *
 * Both are `by lazy` so nothing is built during `Application.onCreate` (which runs on the main
 * thread before the first frame); the database is only actually opened on its first query
 * anyway, since `Room.databaseBuilder(...).build()` is itself lazy.
 *
 * Not Koin: DI isn't wired yet (architecture.md picks Koin, deferred), and two singletons
 * don't justify pulling the framework in early - MainActivity reads them directly, the same
 * plain-constructor-injection style the rest of the app already uses.
 */
class PhotoboothApplication : Application() {

    val database: AppDatabase by lazy { AndroidAppDatabaseFactory(this).create() }

    val galleryRepo: GalleryRepo by lazy { RoomGalleryRepo(database) }

    override fun onCreate() {
        super.onCreate()
        // MobileAds.initialize() is safe to call from the main thread - it does its own SDK
        // setup work off-thread internally - and can run before consent is gathered, since it
        // only initializes the SDK rather than requesting an ad itself. The actual per-ad
        // request (AdBanner.android.kt) separately waits on AdConsent, which MainActivity
        // populates via the UMP consent flow - that gate needs an Activity to host its UI, so
        // it can't live here.
        MobileAds.initialize(this)
    }
}
