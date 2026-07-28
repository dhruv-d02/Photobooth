package com.dj.photobooth.gallery

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Real Room, not a fake - GalleryRepo is thin enough (a couple of DAO pass-throughs) that the
// only thing worth verifying is that Room itself is wired correctly (entity round-trips,
// newest-first ordering, delete actually removes a row) - a fake would just re-assert its own
// fake logic. In androidUnitTest, not commonTest, for the same reason as StripCompositorTest:
// Robolectric (needed for a real Context to build the in-memory database) can't compile for
// iOS targets.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GalleryRepoTest {

    // AndroidSQLiteDriver (routes through android.database.sqlite, which Robolectric shadows
    // with a real native SQLite implementation), not BundledSQLiteDriver (production's
    // driver, see AndroidAppDatabaseFactory) - see the dependency comment in build.gradle.kts
    // for why the bundled driver's native library can't load under a plain JVM test run.
    private fun newInMemoryRepo(): GalleryRepo {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>(ApplicationProvider.getApplicationContext())
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        return RoomGalleryRepo(db)
    }

    private fun entry(id: Long = 0, createdAt: Long, stamp: String = "JUL 25, 2026") = HistoryEntry(
        id = id,
        finalImagePath = "content://media/external/images/$createdAt",
        thumbnailPath = "content://media/external/images/$createdAt",
        filmTreatmentId = "F01",
        createdAt = createdAt,
        stamp = stamp,
    )

    @Test
    fun `save persists an entry and assigns an id`() = runTest {
        val repo = newInMemoryRepo()
        val id = repo.save(entry(createdAt = 1_000))
        assertTrue(id > 0)

        val all = repo.entries.first()
        assertEquals(1, all.size)
        assertEquals(id, all.first().id)
    }

    @Test
    fun `entries are newest first and never capped`() = runTest {
        val repo = newInMemoryRepo()
        // CLAUDE.md: "Gallery/history has no cap" - save well past the prototype's old
        // 12-item localStorage cap to prove there's no eviction logic left over from that.
        repeat(15) { i -> repo.save(entry(createdAt = i.toLong())) }

        val all = repo.entries.first()
        assertEquals(15, all.size, "history must be uncapped")
        assertEquals((14L downTo 0L).toList(), all.map { it.createdAt }, "must be newest-first")
    }

    @Test
    fun `delete is manual and removes only the targeted entry`() = runTest {
        val repo = newInMemoryRepo()
        repo.save(entry(createdAt = 1))
        repo.save(entry(createdAt = 2))
        val toDelete = repo.entries.first().first { it.createdAt == 1L }

        repo.delete(toDelete)

        val remaining = repo.entries.first()
        assertEquals(1, remaining.size)
        assertEquals(2L, remaining.first().createdAt)
    }
}
