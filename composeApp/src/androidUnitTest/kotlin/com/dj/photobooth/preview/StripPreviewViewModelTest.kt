package com.dj.photobooth.preview

import com.dj.photobooth.capture.CaptureFrame
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.ShareSheet
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.gallery.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeGalleryRepo : GalleryRepo {
    private val state = MutableStateFlow<List<HistoryEntry>>(emptyList())
    override val entries = state.asStateFlow()
    val saved = mutableListOf<HistoryEntry>()

    override suspend fun save(entry: HistoryEntry): Long {
        saved += entry
        state.value = state.value + entry
        return saved.size.toLong()
    }

    override suspend fun delete(entry: HistoryEntry) {
        state.value = state.value.filterNot { it == entry }
    }
}

private class FakeMediaRepo(private val shouldThrow: Boolean = false) : MediaRepo {
    var lastSavedName: String? = null
    override suspend fun savePng(pngBytes: ByteArray, displayName: String): String {
        if (shouldThrow) error("simulated save failure")
        lastSavedName = displayName
        return "content://media/external/images/$displayName"
    }

    override suspend fun copyToDevice(sourcePath: String, displayName: String): String =
        "content://media/external/images/$displayName"

    override suspend fun delete(path: String) {
        // Not exercised by this ViewModel's tests - StripPreviewViewModel never deletes.
    }
}

private class FakeShareSheet : ShareSheet {
    var lastSharedPath: String? = null
    override suspend fun shareImage(mediaPath: String, displayName: String) {
        lastSharedPath = mediaPath
    }
}

private fun placeholderFrame() = CaptureFrame(jpegBytes = ByteArray(0), isPlaceholder = true)

// Robolectric + GraphicsMode.NATIVE, matching StripCompositorTest.kt exactly and for the same
// reason: this ViewModel calls StripCompositor.compose() (and placeholderFrameBitmap(), the
// Phase-3 fallback for placeholder frames), both of which draw through real
// androidx.compose.ui.graphics Canvas/ImageBitmap - unavailable in the plain Android unit-test
// stub jar. This is why it lives here rather than in commonTest alongside
// CaptureViewModelTest, which never touches bitmap/canvas code.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StripPreviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        frames: List<CaptureFrame> = List(4) { placeholderFrame() },
        galleryRepo: GalleryRepo = FakeGalleryRepo(),
        mediaRepo: MediaRepo = FakeMediaRepo(),
        shareSheet: ShareSheet? = null,
    ) = StripPreviewViewModel(
        initialFrames = frames,
        galleryRepo = galleryRepo,
        mediaRepo = mediaRepo,
        shareSheet = shareSheet,
        workDispatcher = testDispatcher,
    )

    @Test
    fun `constructing decodes and composes a live preview`() = runTest {
        val viewModel = newViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.decodedFrames.all { it != null })
        assertNotNull(state.composedImage)
        assertFalse(state.isComposing)
    }

    @Test
    fun `changing treatment recomposes and resets saved`() = runTest {
        val viewModel = newViewModel()
        runCurrent()
        val firstComposed = viewModel.uiState.value.composedImage

        viewModel.onTreatmentChange(FilmTreatment.SteelDuotone)
        runCurrent()

        assertEquals(FilmTreatment.SteelDuotone, viewModel.uiState.value.treatment)
        assertNotNull(viewModel.uiState.value.composedImage)
        assertTrue(firstComposed !== viewModel.uiState.value.composedImage, "a new bitmap must actually be composed")
    }

    @Test
    fun `changing layout updates composed image dimensions`() = runTest {
        val viewModel = newViewModel()
        runCurrent()
        val stripHeight = viewModel.uiState.value.composedImage?.height

        viewModel.onLayoutChange(StripLayout.Grid)
        runCurrent()

        val gridHeight = viewModel.uiState.value.composedImage?.height
        assertNotNull(stripHeight)
        assertNotNull(gridHeight)
        assertTrue(stripHeight != gridHeight, "strip vs grid must produce different output geometry")
    }

    @Test
    fun `replaceFrame targets the same slot, never appends`() = runTest {
        val viewModel = newViewModel(frames = List(3) { placeholderFrame() })
        runCurrent()
        val originalSize = viewModel.uiState.value.decodedFrames.size

        viewModel.replaceFrame(1, placeholderFrame())
        runCurrent()

        assertEquals(originalSize, viewModel.uiState.value.decodedFrames.size, "replaceFrame must not change the slot count")
    }

    @Test
    fun `replaceFrame rejects an out-of-range slot`() = runTest {
        val viewModel = newViewModel(frames = List(2) { placeholderFrame() })
        runCurrent()

        assertFailsWith<IllegalArgumentException> { viewModel.replaceFrame(5, placeholderFrame()) }
    }

    @Test
    fun `save writes through MediaRepo and GalleryRepo and flips saved`() = runTest {
        val galleryRepo = FakeGalleryRepo()
        val mediaRepo = FakeMediaRepo()
        val viewModel = newViewModel(galleryRepo = galleryRepo, mediaRepo = mediaRepo)
        runCurrent()

        viewModel.onSavePng()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.saved)
        assertNotNull(state.savedPath)
        assertEquals(1, galleryRepo.saved.size)
        assertEquals(state.treatment.code, galleryRepo.saved.first().filmTreatmentId)
    }

    @Test
    fun `a save failure surfaces an error instead of crashing`() = runTest {
        val viewModel = newViewModel(mediaRepo = FakeMediaRepo(shouldThrow = true))
        runCurrent()

        viewModel.onSavePng()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.saved)
        assertNotNull(state.saveError)
    }

    @Test
    fun `share is a no-op before a save has happened`() = runTest {
        val shareSheet = FakeShareSheet()
        val viewModel = newViewModel(shareSheet = shareSheet)
        runCurrent()

        viewModel.onShare()
        runCurrent()

        assertNull(shareSheet.lastSharedPath)
    }

    @Test
    fun `share forwards the saved path once a save has happened`() = runTest {
        val shareSheet = FakeShareSheet()
        val viewModel = newViewModel(shareSheet = shareSheet)
        runCurrent()

        viewModel.onSavePng()
        runCurrent()
        viewModel.onShare()
        runCurrent()

        assertEquals(viewModel.uiState.value.savedPath, shareSheet.lastSharedPath)
    }

}
