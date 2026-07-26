package com.dj.photobooth.gallery

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
import kotlin.test.assertTrue

/** An in-memory GalleryRepo double, mirroring CaptureViewModelTest's FakeCameraController -
 *  lets GalleryViewModel's reshaping/delete logic be tested without a real Room database. */
private class FakeGalleryRepo : GalleryRepo {
    private val state = MutableStateFlow<List<HistoryEntry>>(emptyList())
    override val entries = state.asStateFlow()
    var savedCount = 0
        private set

    override suspend fun save(entry: HistoryEntry): Long {
        savedCount++
        state.value = (state.value + entry).sortedByDescending { it.createdAt }
        return entry.id
    }

    override suspend fun delete(entry: HistoryEntry) {
        state.value = state.value.filterNot { it.id == entry.id }
    }
}

private class FakeShareSheet : ShareSheet {
    var lastSharedPath: String? = null
    override suspend fun shareImage(mediaPath: String, displayName: String) {
        lastSharedPath = mediaPath
    }
}

private fun entry(id: Long, createdAt: Long) = HistoryEntry(
    id = id,
    finalImagePath = "content://strip/$id",
    thumbnailPath = "content://strip/$id",
    filmTreatmentId = "F01",
    createdAt = createdAt,
    stamp = "JUL 25, 2026",
)

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty repo surfaces as empty ui state`() = runTest {
        val viewModel = GalleryViewModel(FakeGalleryRepo())
        runCurrent()
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals("00", viewModel.uiState.value.countLabel)
    }

    @Test
    fun `entries flow through to ui state uncapped`() = runTest {
        val repo = FakeGalleryRepo()
        val viewModel = GalleryViewModel(repo)
        runCurrent()

        // CLAUDE.md: no eviction limit - save well past the prototype's old 12-item cap.
        repeat(15) { i -> repo.save(entry(id = i.toLong(), createdAt = i.toLong())) }
        runCurrent()

        assertEquals(15, viewModel.uiState.value.entries.size)
        assertEquals("15", viewModel.uiState.value.countLabel)
    }

    @Test
    fun `delete removes only the targeted entry`() = runTest {
        val repo = FakeGalleryRepo()
        repo.save(entry(id = 1, createdAt = 1))
        repo.save(entry(id = 2, createdAt = 2))
        val viewModel = GalleryViewModel(repo)
        runCurrent()

        viewModel.onDelete(entry(id = 1, createdAt = 1))
        runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun `share forwards the entry's saved path to the share sheet`() = runTest {
        val repo = FakeGalleryRepo()
        repo.save(entry(id = 1, createdAt = 1))
        val shareSheet = FakeShareSheet()
        val viewModel = GalleryViewModel(repo, shareSheet)
        runCurrent()

        viewModel.onShare(entry(id = 1, createdAt = 1))
        runCurrent()

        assertEquals("content://strip/1", shareSheet.lastSharedPath)
    }

    @Test
    fun `share without a wired share sheet is a no-op, not a crash`() = runTest {
        val repo = FakeGalleryRepo()
        val viewModel = GalleryViewModel(repo) // no ShareSheet passed
        runCurrent()

        viewModel.onShare(entry(id = 1, createdAt = 1)) // must not throw
        runCurrent()
    }
}
