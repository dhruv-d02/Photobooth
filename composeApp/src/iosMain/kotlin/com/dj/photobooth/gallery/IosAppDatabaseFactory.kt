package com.dj.photobooth.gallery

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS [AppDatabaseFactory] - Phase 4 territory (CLAUDE.md known blockers: iOS builds/testing
 * need a Mac, which isn't available yet). Compiles as Kotlin/Native source, matching
 * IosCameraController.kt's status - written against Room KMP's documented iOS setup (DB file
 * in the app's Documents directory) but never actually built or run. Treat as unverified
 * until Phase 4.
 */
class IosAppDatabaseFactory : AppDatabaseFactory {
    override fun create(): AppDatabase {
        val dbFilePath = documentsDirectoryPath() + "/" + DATABASE_FILE_NAME
        val builder: RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
        )
        return builder
            .setDriver(BundledSQLiteDriver())
            // iOS has no Dispatchers.IO (that's a JVM-only dispatcher) - Default is the
            // documented Kotlin/Native substitute for offloading blocking work.
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    private fun documentsDirectoryPath(): String {
        val url = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask,
        ).firstOrNull() as? NSURL
        return requireNotNull(url?.path) { "Could not resolve the iOS Documents directory" }
    }
}
