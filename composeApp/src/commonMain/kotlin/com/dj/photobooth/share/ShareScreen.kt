package com.dj.photobooth.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.Sparkle
import com.dj.photobooth.ui.TapeCorner
import com.dj.photobooth.ui.Toast

// This screen has no live chaos slider (that's a prototype-only tweak, per architecture.md /
// the design handoff's Interactions section) - fixed at the dc.html default, same constant
// LandingScreen.kt already uses for its own strip-preview graphic.
private const val ShareChaos = 0.65f

/**
 * The Share screen (design/handoff/README.md § "4. Share"), verified against
 * `Photobooth Rebrand.dc.html`'s `screenIs.share` block (lines ~300-339). Dark gradient
 * background matching Capture - "the payoff moment" after Customize's continue hands off a
 * finished [ShareUiState].
 *
 * Pure View in the MVVM sense: [state] comes from [ShareViewModel.uiState], every tap calls a
 * ViewModel method or one of the navigation callbacks. [onBack] is a plain `popBackStack()` -
 * Share is a normal forward destination from Preview, not a replace, so returning to Customize
 * is a normal back-nav pop (PhotoboothNavHost's Route.Share composable).
 */
@Composable
fun ShareScreen(
    state: ShareUiState,
    onBack: () -> Unit,
    onSaveToPhotos: () -> Unit,
    onShare: () -> Unit,
    onMakeAnother: () -> Unit,
    onToastDismissed: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(PhotoboothColors.DarkGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            EditBackLink(onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, alignment = Alignment.CenterVertically),
            ) {
                ReadyHeadline()
                ShareMount(state = state)
            }
            ActionsBlock(
                state = state,
                onSaveToPhotos = onSaveToPhotos,
                onShare = onShare,
                onMakeAnother = onMakeAnother,
            )
        }
        // Bottom-anchored, self-dismissing pill (com.dj.photobooth.ui.Toast) - driven by
        // ShareViewModel's toastMessage, set on a successful save and on share.
        Toast(message = state.toastMessage, onDismissRequest = onToastDismissed)
    }
}

// dc.html line 303: "‹ edit" translucent-white link, top-left.
@Composable
private fun EditBackLink(onBack: () -> Unit) {
    Text(
        text = "‹ edit",
        style = PhotoboothType.bodyBold12(),
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier
            .padding(top = 14.dp, start = 18.dp)
            .clickable(onClick = onBack),
    )
}

// dc.html lines 306-310: sparkle + "strip's ready!" headline, subcopy below.
@Composable
private fun ReadyHeadline() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Sparkle(size = 20.dp, tint = PhotoboothColors.Gold)
        Text("strip's ready!", style = PhotoboothType.display26(), color = Color.White)
    }
    Text(
        text = "save it, send it, stick it on something.",
        style = PhotoboothType.bodySmall(),
        color = Color.White.copy(alpha = 0.65f),
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 260.dp),
    )
}

// dc.html lines 312-324 / 565-570: the strip mount - width 152dp (strip) or 240dp (grid),
// frame-color background, 18dp radius, 13dp padding, a hard 6px ledge + soft blur shadow, and
// (unlike Customize's mount) DOES rotate: -3*chaos degrees. Tape corners are translucent white
// here (not ink), fixed at -25/20deg same as Customize - only the mount itself is chaos-scaled.
@Composable
private fun ShareMount(state: ShareUiState) {
    val mountWidth = if (state.layout == StripLayout.Strip) 152.dp else 240.dp
    val mountRotation = -3f * ShareChaos

    TapeCorner(
        modifier = Modifier.width(mountWidth),
        topLeftTapeColor = Color.White.copy(alpha = 0.3f),
        bottomRightTapeColor = Color.White.copy(alpha = 0.3f),
    ) {
        Box(
            modifier = Modifier
                .width(mountWidth)
                .graphicsLayer { rotationZ = mountRotation },
        ) {
            // Hard drop-shadow ledge - "0 6px 0 rgba(0,0,0,.25)" - same two-layer technique as
            // Customize's mount (an offset same-shaped copy underneath, drawn first).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 6.dp)
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
            )
            Box(
                modifier = Modifier
                    // The soft half of "0 22px 40px rgba(0,0,0,.35)".
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(18.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.35f),
                    )
                    .background(state.frameColor.background, RoundedCornerShape(18.dp))
                    .padding(13.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    // No per-cell geometry needed here (unlike Customize, Share has no retake
                    // buttons) - the composed bitmap's own pixel dimensions are enough to size
                    // the display Image at the right aspect ratio.
                    val aspect = state.image.width.toFloat() / state.image.height.toFloat()
                    val displayHeight = maxWidth / aspect

                    Image(
                        painter = BitmapPainter(state.image),
                        contentDescription = null,
                        modifier = Modifier.width(maxWidth).height(displayHeight),
                    )

                    MountFooter(
                        brand = state.brand,
                        stamp = state.stamp,
                        frameColor = state.frameColor,
                        modifier = Modifier.align(Alignment.BottomCenter).width(maxWidth),
                    )
                }
            }
        }
    }
}

// dc.html lines 319-322: brand name (left) + date stamp (right), both colored to the mount's
// own frame-contrast text color - same shape as StripPreviewScreen's private MountFooter (see
// that file's doc comment for why this isn't extracted into one shared component).
@Composable
private fun MountFooter(
    brand: String,
    stamp: String,
    frameColor: FrameColorPreset,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = frameColor.dim,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f),
                )
            }
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(brand, style = PhotoboothType.display11(), color = frameColor.text)
        Text(stamp, style = PhotoboothType.stamp13(), color = frameColor.text)
    }
}

// dc.html lines 326-332: "save to photos" (white pill, primary) + "share" (outline pill), then
// "make another strip" secondary link.
@Composable
private fun ActionsBlock(
    state: ShareUiState,
    onSaveToPhotos: () -> Unit,
    onShare: () -> Unit,
    onMakeAnother: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                // Hard drop-shadow ledge - "0 5px 0 rgba(0,0,0,.25)" (neutral gray/black since
                // this is a white button, unlike Customize's hot-pink "continue").
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .offset(y = 5.dp)
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(50)),
                )
                Button(
                    onClick = onSaveToPhotos,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PhotoboothColors.Ink,
                        disabledContainerColor = Color.White.copy(alpha = 0.6f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                ) {
                    Text(
                        text = if (state.isSaving) "saving…" else "save to photos",
                        style = PhotoboothType.display13().copy(fontSize = 14.sp),
                    )
                }
            }
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("share", style = PhotoboothType.display13().copy(fontSize = 14.sp))
            }
        }
        Text(
            text = "make another strip",
            style = PhotoboothType.bodyBold12(),
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onMakeAnother)
                .padding(vertical = 4.dp),
        )
        // Not in the dc.html prototype (which has no real save/network failure mode to show) -
        // a small addition so a genuine MediaRepo/GalleryRepo failure isn't silently swallowed.
        state.saveError?.let { message ->
            Text(
                text = message,
                style = PhotoboothType.bodyCaption(),
                color = PhotoboothColors.HotPink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
