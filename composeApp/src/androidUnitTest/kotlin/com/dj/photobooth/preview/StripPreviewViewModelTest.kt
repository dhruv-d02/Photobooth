package com.dj.photobooth.preview

import com.dj.photobooth.capture.CaptureFrame
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.StripLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertTrue

private fun placeholderFrame() = CaptureFrame(jpegBytes = ByteArray(0), isPlaceholder = true)

// Robolectric + GraphicsMode.NATIVE, matching StripCompositorTest.kt exactly and for the same
// reason: this ViewModel calls StripCompositor.compose() (and placeholderFrameBitmap(), the
// Phase-3 fallback for placeholder frames), both of which draw through real
// androidx.compose.ui.graphics Canvas/ImageBitmap - unavailable in the plain Android unit-test
// stub jar. This is why it lives here rather than in commonTest alongside
// CaptureViewModelTest, which never touches bitmap/canvas code.
//
// Save/share tests used to live here too (StripPreviewViewModel.onSavePng()/onShare()) - they
// moved to com.dj.photobooth.share.ShareViewModelTest when the Boothie rebrand split that
// responsibility out into a standalone ShareViewModel (see StripPreviewViewModel's class doc).
// This class now only ever exercises decode/compose/style-selection.
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
    ) = StripPreviewViewModel(
        initialFrames = frames,
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
    fun `changing treatment recomposes with the new treatment`() = runTest {
        val viewModel = newViewModel()
        runCurrent()
        val firstComposed = viewModel.uiState.value.composedImage

        viewModel.onTreatmentChange(FilmTreatment.Cyber)
        runCurrent()

        assertEquals(FilmTreatment.Cyber, viewModel.uiState.value.treatment)
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
}
