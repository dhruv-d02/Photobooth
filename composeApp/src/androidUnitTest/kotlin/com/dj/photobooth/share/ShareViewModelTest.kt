package com.dj.photobooth.share

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.ShareSheet
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.gallery.HistoryEntry
import com.dj.photobooth.nav.ComposedStrip
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
    override suspend fun savePng(pngBytes: ByteArray, displayName: String): String {
        if (shouldThrow) error("simulated save failure")
        return "content://media/external/images/$displayName"
    }

    override suspend fun copyToDevice(sourcePath: String, displayName: String): String =
        "content://media/external/images/$displayName"

    override suspend fun delete(path: String) {
        // Not exercised by this ViewModel's tests.
    }
}

private class FakeShareSheet : ShareSheet {
    var lastSharedPath: String? = null
    override suspend fun shareImage(mediaPath: String, displayName: String) {
        lastSharedPath = mediaPath
    }
}

private fun testComposedStrip(): ComposedStrip {
    val bitmap = ImageBitmap(100, 100)
    Canvas(bitmap).drawRect(0f, 0f, 100f, 100f, Paint())
    return ComposedStrip(
        image = bitmap,
        treatmentCode = FilmTreatment.None.code,
        frameColor = FrameColorPreset.Bubblegum,
        layout = StripLayout.Strip,
        stamp = "jul 31",
    )
}

// Robolectric + GraphicsMode.NATIVE, same reason as StripPreviewViewModelTest/StripCompositorTest:
// onSaveToPhotos() PNG-encodes a real ImageBitmap, which needs actual Bitmap/Canvas support the
// plain Android unit-test stub jar doesn't provide. These tests are the save/share coverage that
// used to live on StripPreviewViewModelTest before the Boothie rebrand split that responsibility
// out into this standalone ShareViewModel.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareViewModelTest {

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
        galleryRepo: GalleryRepo = FakeGalleryRepo(),
        mediaRepo: MediaRepo = FakeMediaRepo(),
        shareSheet: ShareSheet? = null,
        composedStrip: ComposedStrip = testComposedStrip(),
    ) = ShareViewModel(
        composedStrip = composedStrip,
        galleryRepo = galleryRepo,
        mediaRepo = mediaRepo,
        shareSheet = shareSheet,
        workDispatcher = testDispatcher,
    )

    @Test
    fun `uiState is seeded straight from the composed strip hand-off`() {
        val strip = testComposedStrip()
        val viewModel = newViewModel(composedStrip = strip)

        val state = viewModel.uiState.value
        assertEquals(strip.image, state.image)
        assertEquals(strip.treatmentCode, state.treatmentCode)
        assertEquals(strip.frameColor, state.frameColor)
        assertEquals(strip.layout, state.layout)
        assertEquals(strip.stamp, state.stamp)
    }

    @Test
    fun `save writes through MediaRepo and GalleryRepo and flips saved`() = runTest {
        val galleryRepo = FakeGalleryRepo()
        val mediaRepo = FakeMediaRepo()
        val viewModel = newViewModel(galleryRepo = galleryRepo, mediaRepo = mediaRepo)

        viewModel.onSaveToPhotos()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.saved)
        assertNotNull(state.savedPath)
        assertEquals(1, galleryRepo.saved.size)
        assertEquals(state.treatmentCode, galleryRepo.saved.first().filmTreatmentId)
        assertEquals("saved! find it in strips", state.toastMessage)
    }

    @Test
    fun `a save failure surfaces an error instead of crashing`() = runTest {
        val viewModel = newViewModel(mediaRepo = FakeMediaRepo(shouldThrow = true))

        viewModel.onSaveToPhotos()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.saved)
        assertNotNull(state.saveError)
    }

    @Test
    fun `share is a no-op before a save has happened`() = runTest {
        val shareSheet = FakeShareSheet()
        val viewModel = newViewModel(shareSheet = shareSheet)

        viewModel.onShare()
        runCurrent()

        assertNull(shareSheet.lastSharedPath)
    }

    @Test
    fun `share forwards the saved path once a save has happened`() = runTest {
        val shareSheet = FakeShareSheet()
        val viewModel = newViewModel(shareSheet = shareSheet)

        viewModel.onSaveToPhotos()
        runCurrent()
        viewModel.onShare()
        runCurrent()

        assertEquals(viewModel.uiState.value.savedPath, shareSheet.lastSharedPath)
        assertEquals("opening your share sheet…", viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `onToastDismissed clears the toast message`() = runTest {
        val viewModel = newViewModel()
        viewModel.onSaveToPhotos()
        runCurrent()
        assertNotNull(viewModel.uiState.value.toastMessage)

        viewModel.onToastDismissed()

        assertNull(viewModel.uiState.value.toastMessage)
    }
}
