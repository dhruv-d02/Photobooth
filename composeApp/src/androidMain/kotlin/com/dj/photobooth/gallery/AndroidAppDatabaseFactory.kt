package com.dj.photobooth.gallery

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Android [AppDatabaseFactory]: the DB file lives in app-private storage
 * (`context.getDatabasePath`), same as every other Room app - no MediaStore/scoped-storage
 * concerns here, this is the app's own index, not user-visible media.
 */
class AndroidAppDatabaseFactory(context: Context) : AppDatabaseFactory {

    // applicationContext, not the passed-in Context: a database outlives every Activity (see
    // PhotoboothApplication, which owns the single instance), so holding an Activity here
    // would pin it for the life of the process.
    private val appContext = context.applicationContext

    override fun create(): AppDatabase {
        val dbFile = appContext.getDatabasePath(DATABASE_FILE_NAME)
        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
