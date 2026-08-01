package com.dj.photobooth.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.camera.CameraPreviewSurface
import com.dj.photobooth.compose.decodeJpegToImageBitmap
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Capture screen (design/handoff/README.md § "2. Capture", verified against
 * `Photobooth Rebrand.dc.html`'s `screenIs.capture` block): full-bleed dark gradient screen,
 * no bottom tab bar (a session is modal). Pure View in the MVVM sense - all state comes from
 * [viewModel]'s [CaptureUiState]; every tap just calls a ViewModel method. The capture/keep/
 * retake state machine itself is untouched by this re-skin - only the visuals below it change.
 *
 * [onSessionComplete] fires once, with every accepted frame, the moment
 * [CaptureUiState.sessionComplete] flips to true. Starting the session is deliberately NOT
 * done here - the caller (NavHost) picks fresh session vs. single-slot retake.
 */
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    cameraController: CameraController,
    onExitToLanding: () -> Unit,
    onSessionComplete: (frames: List<CaptureFrame>) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    // Shared across every rememberDecodedFrame call site (proof overlay + thumbnail row) so the
    // same accepted frame is decoded once, not twice.
    val decodedFrameCache = remember { mutableMapOf<CaptureFrame, ImageBitmap>() }

    LaunchedEffect(state.sessionComplete) {
        if (state.sessionComplete) {
            onSessionComplete(state.frames.filterNotNull())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.DarkGradient),
    ) {
        CaptureTopBar(state = state, onExit = { viewModel.onExit(); onExitToLanding() })

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Viewfinder(state = state, cameraController = cameraController, decodedFrameCache = decodedFrameCache)
        }

        CaptureBottomBar(
            state = state,
            onShutter = viewModel::onShutter,
            onKeep = viewModel::onKeep,
            onShootAgain = viewModel::onShootAgain,
            decodedFrameCache = decodedFrameCache,
        )
    }
}

@Composable
private fun CaptureTopBar(state: CaptureUiState, onExit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onExit)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                "exit",
                style = PhotoboothType.bodyBold11(),
                color = PhotoboothColors.Cream,
            )
        }

        val captureStatusText = when {
            state.review != null -> "reviewing shot ${state.frameLabel(state.review.index)}"
            state.queue.isNotEmpty() -> "shot ${state.frameLabel(state.queue.first())} of ${state.shotCountLabel()}"
            state.acceptedCount > 0 -> "${state.acceptedCount} of ${state.shotCount} kept"
            else -> "ready when you are"
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.14f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(captureStatusText, style = PhotoboothType.display13().copy(fontSize = 12.5.sp), color = PhotoboothColors.Cream)
        }

        Box(modifier = Modifier.width(52.dp))
    }
}

@Composable
private fun Viewfinder(
    state: CaptureUiState,
    cameraController: CameraController,
    decodedFrameCache: MutableMap<CaptureFrame, ImageBitmap>,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp)),
    ) {
        CameraPreviewSurface(
            controller = cameraController,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = state.countdown.isEmpty() && !state.flash && state.review == null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = "tap \"shoot ${state.shotCount} pics\" when you're ready",
                style = PhotoboothType.bodyCaption().copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp),
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }

        BounceInCountdown(
            text = state.countdown,
            modifier = Modifier.align(Alignment.Center),
        )

        AnimatedVisibility(
            visible = state.flash,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(PhotoboothColors.FlashOverlay))
        }

        state.review?.let { review ->
            ReviewBadgeRow(review = review, state = state)
        }
    }
}

// dc.html: `scale(.55) rotate(-8deg) opacity:0` at 0% -> `scale(1.1) rotate(3deg) opacity:1`
// at 55% -> `scale(1) rotate(0deg) opacity:1` at 100%, over 500ms - replayed every time the
// countdown digit changes. Approximated with linear keyframe interpolation between the three
// points (close enough for a decorative overshoot bounce; an exact CSS easing curve per
// segment isn't worth the extra complexity here).
@Composable
private fun BounceInCountdown(text: String, modifier: Modifier = Modifier) {
    if (text.isEmpty()) return
    val scale = remember(text) { Animatable(0.55f) }
    val rotation = remember(text) { Animatable(-8f) }
    val alpha = remember(text) { Animatable(0f) }
    LaunchedEffect(text) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 500
                    0.55f at 0
                    1.1f at 275
                    1f at 500
                },
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    -8f at 0
                    3f at 275
                    0f at 500
                },
            )
        }
        launch { alpha.animateTo(1f, animationSpec = tween(durationMillis = 275)) }
    }
    Text(
        text = text,
        style = PhotoboothType.countdownDisplay(),
        color = PhotoboothColors.Cream,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            rotationZ = rotation.value
            this.alpha = alpha.value
        },
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ReviewBadgeRow(review: ReviewState, state: CaptureUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopStart)
            .padding(top = 14.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(PhotoboothColors.ReviewBadge)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                "frame ${state.frameLabel(review.index)} of ${state.shotCountLabel()}",
                style = PhotoboothType.display11(),
                color = PhotoboothColors.Ink,
            )
        }
        Text(
            "your call — keep it or redo it",
            style = PhotoboothType.bodyCaption(),
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

/**
 * Decodes [frame]'s JPEG bytes off the main thread. Returns null while decoding is in flight
 * (explicitly reset on every key change - produceState's initialValue only applies once) or
 * for placeholder frames or on decode failure.
 *
 * [cache] is shared across every call site in this screen so a frame is decoded once, not
 * twice - CaptureFrame's content-based equals/hashCode make it a safe map key.
 */
@Composable
private fun rememberDecodedFrame(
    frame: CaptureFrame?,
    cache: MutableMap<CaptureFrame, ImageBitmap>,
): ImageBitmap? {
    val decoded by produceState<ImageBitmap?>(initialValue = null, frame) {
        value = null
        if (frame == null || frame.isPlaceholder) return@produceState
        cache[frame]?.let { value = it; return@produceState }
        val bitmap = try {
            withContext(Dispatchers.Default) { decodeJpegToImageBitmap(frame.jpegBytes) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (bitmap != null) cache[frame] = bitmap
        value = bitmap
    }
    return decoded
}

@Composable
private fun CaptureBottomBar(
    state: CaptureUiState,
    onShutter: () -> Unit,
    onKeep: () -> Unit,
    onShootAgain: () -> Unit,
    decodedFrameCache: MutableMap<CaptureFrame, ImageBitmap>,
) {
    // Thumbnails + controls collapse away while a frame is actively counting down/exposing,
    // giving the viewfinder the full remaining height, then reappear once there's something to
    // review (or immediately, if idle) - see the Industry-era version's doc comment for the
    // full "why not gate on state.shooting alone" reasoning, unchanged here.
    val isExposing = state.shooting && state.review == null

    AnimatedVisibility(
        visible = !isExposing,
        enter = fadeIn(tween(150)) + expandVertically(tween(150)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThumbnailRow(state = state, decodedFrameCache = decodedFrameCache)

            if (state.review != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onShootAgain,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PhotoboothColors.Cream),
                    ) { Text("retake", style = PhotoboothType.display13().copy(fontSize = 14.5.sp)) }

                    HardShadowPill(
                        onClick = onKeep,
                        modifier = Modifier.weight(1.4f),
                        height = 54.dp,
                        backgroundColor = PhotoboothColors.HotPink,
                        shadowColor = PhotoboothColors.HotPinkPressed,
                        contentColor = PhotoboothColors.Cream,
                    ) { Text("keep it", style = PhotoboothType.display13().copy(fontSize = 14.5.sp)) }
                }
                Text(
                    state.log,
                    style = PhotoboothType.bodyCaption(),
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                val shutterLabel = when {
                    state.shooting -> "shooting…"
                    state.acceptedCount == 0 -> "shoot ${state.shotCount} pics"
                    else -> "shoot pic ${state.acceptedCount + 1}"
                }
                if (state.shooting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(shutterLabel, style = PhotoboothType.display13().copy(fontSize = 15.sp), color = Color.White.copy(alpha = 0.7f))
                    }
                } else {
                    HardShadowPill(
                        onClick = onShutter,
                        modifier = Modifier.fillMaxWidth(),
                        height = 54.dp,
                        enabled = !state.sessionRefused,
                        backgroundColor = PhotoboothColors.Cream,
                        shadowColor = Color.Black.copy(alpha = 0.25f),
                        contentColor = PhotoboothColors.Ink,
                    ) { Text(shutterLabel, style = PhotoboothType.display13().copy(fontSize = 16.sp)) }
                }
                Text(
                    state.log,
                    style = PhotoboothType.bodyCaption(),
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * A pill button with a hard, non-blurred drop shadow (`0 5px 0 <shadowColor>` in the design,
 * not a blurred elevation shadow) - built as two stacked pills: [shadowColor] offset 5dp down
 * underneath, the real button unshifted on top with Material elevation zeroed.
 */
@Composable
private fun HardShadowPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
    backgroundColor: Color,
    shadowColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = 5.dp)
                .background(shadowColor, RoundedCornerShape(50)),
        )
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(height),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
                disabledContainerColor = backgroundColor.copy(alpha = 0.45f),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        ) { content() }
    }
}

@Composable
private fun ThumbnailRow(state: CaptureUiState, decodedFrameCache: MutableMap<CaptureFrame, ImageBitmap>) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
        state.frames.forEachIndexed { index, frame ->
            val decoded = rememberDecodedFrame(frame, decodedFrameCache)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (frame != null) Color.Transparent else Color.White.copy(alpha = 0x14 / 255f))
                    .border(
                        width = 2.dp,
                        color = if (frame != null) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (decoded != null) {
                    Image(
                        painter = BitmapPainter(decoded),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (frame != null) {
                    Text(
                        state.frameLabel(index),
                        style = PhotoboothType.display9(),
                        color = PhotoboothColors.Cream,
                        modifier = Modifier.padding(3.dp),
                    )
                }
            }
        }
    }
}
