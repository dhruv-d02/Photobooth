package com.dj.photobooth.gallery

import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.MediaViewer
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

private class FakeMediaRepo(private val shouldThrow: Boolean = false) : MediaRepo {
    var lastCopiedSource: String? = null

    override suspend fun savePng(pngBytes: ByteArray, displayName: String): String =
        "content://media/$displayName"

    override suspend fun copyToDevice(sourcePath: String, displayName: String): String {
        if (shouldThrow) error("simulated copy failure")
        lastCopiedSource = sourcePath
        return "content://media/$displayName"
    }
}

private class FakeMediaViewer : MediaViewer {
    var lastOpenedPath: String? = null
    override suspend fun openImage(mediaPath: String) {
        lastOpenedPath = mediaPath
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

private fun newViewModel(
    repo: GalleryRepo,
    mediaRepo: MediaRepo = FakeMediaRepo(),
    mediaViewer: MediaViewer? = null,
) = GalleryViewModel(repo = repo, mediaRepo = mediaRepo, mediaViewer = mediaViewer)

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
        val viewModel = newViewModel(FakeGalleryRepo())
        runCurrent()
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(0, viewModel.uiState.value.entries.size)
    }

    @Test
    fun `entries flow through to ui state uncapped`() = runTest {
        val repo = FakeGalleryRepo()
        val viewModel = newViewModel(repo)
        runCurrent()

        // CLAUDE.md: no eviction limit - save well past the prototype's old 12-item cap.
        repeat(15) { i -> repo.save(entry(id = i.toLong(), createdAt = i.toLong())) }
        runCurrent()

        assertEquals(15, viewModel.uiState.value.entries.size)
    }

    @Test
    fun `delete removes only the targeted entry`() = runTest {
        val repo = FakeGalleryRepo()
        repo.save(entry(id = 1, createdAt = 1))
        repo.save(entry(id = 2, createdAt = 2))
        val viewModel = newViewModel(repo)
        runCurrent()

        viewModel.onDelete(entry(id = 1, createdAt = 1))
        runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun `save copies the archived strip back to device storage`() = runTest {
        val repo = FakeGalleryRepo()
        repo.save(entry(id = 1, createdAt = 1))
        val mediaRepo = FakeMediaRepo()
        val viewModel = newViewModel(repo, mediaRepo = mediaRepo)
        runCurrent()

        viewModel.onSaveCopy(entry(id = 1, createdAt = 1))
        runCurrent()

        assertEquals("content://strip/1", mediaRepo.lastCopiedSource)
        assertNotNull(viewModel.uiState.value.message, "a successful save must report back")
    }

    @Test
    fun `a save failure surfaces a message instead of crashing`() = runTest {
        val repo = FakeGalleryRepo()
        repo.save(entry(id = 1, createdAt = 1))
        val viewModel = newViewModel(repo, mediaRepo = FakeMediaRepo(shouldThrow = true))
        runCurrent()

        viewModel.onSaveCopy(entry(id = 1, createdAt = 1))
        runCurrent()

        assertNotNull(viewModel.uiState.value.message)
    }

    @Test
    fun `opening an entry hands its path to the platform viewer`() = runTest {
        val repo = FakeGalleryRepo()
        repo.save(entry(id = 1, createdAt = 1))
        val viewer = FakeMediaViewer()
        val viewModel = newViewModel(repo, mediaViewer = viewer)
        runCurrent()

        viewModel.onOpen(entry(id = 1, createdAt = 1))
        runCurrent()

        assertEquals("content://strip/1", viewer.lastOpenedPath)
    }

    @Test
    fun `opening without a wired viewer is a no-op, not a crash`() = runTest {
        val repo = FakeGalleryRepo()
        val viewModel = newViewModel(repo) // no MediaViewer passed
        runCurrent()

        viewModel.onOpen(entry(id = 1, createdAt = 1)) // must not throw
        runCurrent()

        assertNull(viewModel.uiState.value.message)
    }
}
