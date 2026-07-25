package com.dj.photobooth.capture

import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.camera.LensFacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A CameraController double that captures instantly with a distinguishable byte payload
 *  instead of touching real camera hardware - lets the session-sequence logic in
 *  CaptureViewModel be tested without CameraX/AVFoundation at all. */
private class FakeCameraController(
    granted: Boolean = true,
    private val autoGrantOnRequest: Boolean = true,
) : CameraController {
    override val hasCameraPermission = MutableStateFlow(granted)
    var captureCount = 0
        private set

    override fun requestCameraPermission() {
        if (autoGrantOnRequest) hasCameraPermission.value = true
    }

    override val lensFacing: StateFlow<LensFacing> = MutableStateFlow(LensFacing.Front)

    var shouldThrowOnCapture = false

    override suspend fun capturePhoto(): ByteArray {
        if (shouldThrowOnCapture) error("simulated capture failure")
        captureCount++
        return byteArrayOf(captureCount.toByte())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `full session accepts every frame in order`() = runTest {
        val camera = FakeCameraController()
        val viewModel = CaptureViewModel(camera, initialShotCount = 3)

        viewModel.onStartSession()
        runCurrent()
        assertEquals(CameraState.Live, viewModel.uiState.value.cameraState)

        viewModel.onShutter()
        repeat(3) { frameIndex ->
            advanceThroughCountdownAndCapture()
            val review = viewModel.uiState.value.review
            assertEquals(frameIndex, review?.index, "expected review for frame $frameIndex")
            viewModel.onKeep()
            // 420ms accept pause, then - once the queue is actually empty, on the final
            // frame - a further 260ms before the session is declared complete
            // (design/handoff/README.md § Interactions & Behaviour, step 4).
            advanceTimeBy(420 + 260)
            runCurrent()
        }

        val finalState = viewModel.uiState.value
        assertTrue(finalState.sessionComplete)
        assertEquals(3, finalState.acceptedCount)
        assertNull(finalState.review)
        assertEquals(3, camera.captureCount)
    }

    @Test
    fun `shoot again re-queues the same index instead of advancing`() = runTest {
        val camera = FakeCameraController()
        val viewModel = CaptureViewModel(camera, initialShotCount = 2)

        viewModel.onStartSession()
        runCurrent()
        viewModel.onShutter()
        advanceThroughCountdownAndCapture()

        assertEquals(0, viewModel.uiState.value.review?.index)
        viewModel.onShootAgain()
        advanceTimeBy(320)
        runCurrent()
        advanceThroughCountdownAndCapture()

        // Still frame 0 under review - a reshoot must never let the queue skip ahead, so
        // the strip can never end up with a gap (CLAUDE.md's reshoot-same-slot invariant).
        assertEquals(0, viewModel.uiState.value.review?.index)
        assertEquals(2, camera.captureCount)
        assertNull(viewModel.uiState.value.frames[0])
    }

    @Test
    fun `denied camera falls back to placeholder frames`() = runTest {
        val camera = FakeCameraController(granted = false, autoGrantOnRequest = false)
        val viewModel = CaptureViewModel(camera, initialShotCount = 1)

        viewModel.onStartSession()
        advanceTimeBy(10_001) // past the 10s permission-wait timeout
        runCurrent()
        assertEquals(CameraState.Denied, viewModel.uiState.value.cameraState)

        viewModel.onShutter()
        advanceThroughCountdownAndCapture()

        val review = viewModel.uiState.value.review
        assertTrue(review?.frame?.isPlaceholder == true)
        assertEquals(0, camera.captureCount, "placeholder frames must not call the real camera")
    }

    @Test
    fun `retake arms the queue without auto-firing the countdown`() = runTest {
        val camera = FakeCameraController()
        val viewModel = CaptureViewModel(camera, initialShotCount = 3)
        viewModel.onStartSession()
        runCurrent()

        viewModel.onRetake(1)
        runCurrent()

        // Armed (queue set) but not shooting - the user still has to tap the shutter,
        // design/handoff/README.md line 183 ("the shutter reads SHOOT 0N").
        assertEquals(listOf(1), viewModel.uiState.value.queue)
        assertEquals(false, viewModel.uiState.value.shooting)
        assertEquals(0, camera.captureCount, "onRetake must not capture until the shutter is actually tapped")

        viewModel.onShutter()
        advanceThroughCountdownAndCapture()

        assertEquals(1, viewModel.uiState.value.review?.index)
        assertEquals(1, camera.captureCount)
    }

    @Test
    fun `a capture failure falls back to a placeholder frame instead of crashing`() = runTest {
        val camera = FakeCameraController().apply { shouldThrowOnCapture = true }
        val viewModel = CaptureViewModel(camera, initialShotCount = 1)
        viewModel.onStartSession()
        runCurrent()
        assertEquals(CameraState.Live, viewModel.uiState.value.cameraState)

        viewModel.onShutter()
        advanceThroughCountdownAndCapture()

        val review = viewModel.uiState.value.review
        assertTrue(review?.frame?.isPlaceholder == true, "a thrown capturePhoto() should yield a placeholder frame, not propagate")
    }

    @Test
    fun `shot count is clamped to the 2 to 8 range`() {
        val viewModel = CaptureViewModel(FakeCameraController(), initialShotCount = 4)

        viewModel.onShotCountChange(1)
        assertEquals(2, viewModel.uiState.value.shotCount)

        viewModel.onShotCountChange(9)
        assertEquals(8, viewModel.uiState.value.shotCount)

        viewModel.onShotCountChange(6)
        assertEquals(6, viewModel.uiState.value.shotCount)
        assertEquals(6, viewModel.uiState.value.frames.size)
    }

    /** Advances past the 3 countdown steps (760ms each) and the flash (180ms), landing on
     *  the proof-overlay state, matching design/handoff/README.md's exact timings. */
    private fun kotlinx.coroutines.test.TestScope.advanceThroughCountdownAndCapture() {
        advanceTimeBy(3 * 760L + 180L + 50L)
        runCurrent()
    }
}
