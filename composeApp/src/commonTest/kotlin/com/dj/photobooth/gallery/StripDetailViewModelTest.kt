package com.dj.photobooth.gallery

import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.ShareSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// "Detail"-prefixed, not the plain FakeGalleryRepo/FakeMediaRepo/FakeShareSheet names
// GalleryViewModelTest.kt already uses: Kotlin's top-level `private` only restricts *use* to
// the declaring file, it doesn't give each file its own declaration namespace - two files in
// the same package can't each declare a top-level class with the same simple name, private or
// not.
private class DetailFakeGalleryRepo : GalleryRepo {
    private val state = MutableStateFlow<List<HistoryEntry>>(emptyList())
    override val entries = state.asStateFlow()
    val deleted = mutableListOf<HistoryEntry>()

    override suspend fun save(entry: HistoryEntry): Long {
        state.value = state.value + entry
        return entry.id
    }

    override suspend fun delete(entry: HistoryEntry) {
        deleted += entry
        state.value = state.value.filterNot { it.id == entry.id }
    }
}

private class DetailFakeMediaRepo(private val deleteShouldThrow: Boolean = false) : MediaRepo {
    val deletedPaths = mutableListOf<String>()

    override suspend fun savePng(pngBytes: ByteArray, displayName: String) = "unused"
    override suspend fun copyToDevice(sourcePath: String, displayName: String) = "unused"

    override suspend fun delete(path: String) {
        if (deleteShouldThrow) error("simulated file delete failure")
        deletedPaths += path
    }
}

private class DetailFakeShareSheet : ShareSheet {
    var lastSharedPath: String? = null
    override suspend fun shareImage(mediaPath: String, displayName: String) {
        lastSharedPath = mediaPath
    }
}

private fun entry(id: Long) = HistoryEntry(
    id = id,
    finalImagePath = "content://strip/$id",
    thumbnailPath = "content://strip/$id",
    filmTreatmentId = "F01",
    createdAt = id,
    stamp = "JUL 25, 2026",
)

@OptIn(ExperimentalCoroutinesApi::class)
class StripDetailViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the entry matching entryId out of the repo's full list`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        repo.save(entry(id = 2))
        val viewModel = StripDetailViewModel(entryId = 2, repo = repo, mediaRepo = DetailFakeMediaRepo())
        runCurrent()

        assertEquals(2L, viewModel.uiState.value.entry?.id)
        assertTrue(viewModel.uiState.value.loaded)
        assertFalse(viewModel.uiState.value.notFound)
    }

    @Test
    fun `an id with no matching entry surfaces as not found, not stuck loading`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        val viewModel = StripDetailViewModel(entryId = 999, repo = repo, mediaRepo = DetailFakeMediaRepo())
        runCurrent()

        assertNull(viewModel.uiState.value.entry)
        assertTrue(viewModel.uiState.value.notFound)
    }

    @Test
    fun `delete requires confirmation before anything happens`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        val viewModel = StripDetailViewModel(entryId = 1, repo = repo, mediaRepo = DetailFakeMediaRepo())
        runCurrent()

        viewModel.onDeleteRequested()
        runCurrent()

        assertTrue(viewModel.uiState.value.confirmingDelete)
        assertTrue(repo.deleted.isEmpty(), "requesting delete must not delete anything yet")
        assertFalse(viewModel.uiState.value.deleted)
    }

    @Test
    fun `cancelling a delete request leaves the entry untouched`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        val viewModel = StripDetailViewModel(entryId = 1, repo = repo, mediaRepo = DetailFakeMediaRepo())
        runCurrent()

        viewModel.onDeleteRequested()
        viewModel.onDeleteCancelled()
        runCurrent()

        assertFalse(viewModel.uiState.value.confirmingDelete)
        assertNotNull(viewModel.uiState.value.entry, "cancelling must not delete the entry")
    }

    @Test
    fun `confirming delete removes the archive row and the underlying file`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        val mediaRepo = DetailFakeMediaRepo()
        val viewModel = StripDetailViewModel(entryId = 1, repo = repo, mediaRepo = mediaRepo)
        runCurrent()

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        runCurrent()

        assertEquals(listOf(1L), repo.deleted.map { it.id })
        assertEquals(listOf("content://strip/1"), mediaRepo.deletedPaths)
        assertTrue(viewModel.uiState.value.deleted, "deleted must flip so the nav layer pops back")
        assertFalse(viewModel.uiState.value.confirmingDelete)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `a file-cleanup failure still completes the delete rather than surfacing an error`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        val viewModel = StripDetailViewModel(
            entryId = 1,
            repo = repo,
            mediaRepo = DetailFakeMediaRepo(deleteShouldThrow = true),
        )
        runCurrent()

        viewModel.onDeleteConfirmed()
        runCurrent()

        // The row is the user-visible source of truth for "deleted" - a lingering orphan file
        // is a harmless storage leak, not something worth blocking or erroring the user over.
        assertTrue(repo.deleted.isNotEmpty(), "the row must still be removed")
        assertTrue(viewModel.uiState.value.deleted)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `share hands the entry's path to the platform share sheet`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        val shareSheet = DetailFakeShareSheet()
        val viewModel = StripDetailViewModel(entryId = 1, repo = repo, mediaRepo = DetailFakeMediaRepo(), shareSheet = shareSheet)
        runCurrent()

        viewModel.onShare()
        runCurrent()

        assertEquals("content://strip/1", shareSheet.lastSharedPath)
    }

    @Test
    fun `sharing without a wired share sheet is a no-op, not a crash`() = runTest {
        val repo = DetailFakeGalleryRepo()
        repo.save(entry(id = 1))
        val viewModel = StripDetailViewModel(entryId = 1, repo = repo, mediaRepo = DetailFakeMediaRepo()) // no ShareSheet
        runCurrent()

        viewModel.onShare() // must not throw
        runCurrent()
    }
}
