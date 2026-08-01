package com.dj.photobooth.share

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.photobooth.compose.encodeImageBitmapToPng
import com.dj.photobooth.export.MediaRepo
import com.dj.photobooth.export.ShareSheet
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.gallery.GalleryRepo
import com.dj.photobooth.gallery.HistoryEntry
import com.dj.photobooth.gallery.currentEpochMillis
import com.dj.photobooth.nav.ComposedStrip
import com.dj.photobooth.theme.Brand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI state for the Share screen (design/handoff/README.md § "4. Share"). [image]/[treatmentCode]
 * /[frameColor]/[layout]/[stamp] are copied straight from the [ComposedStrip] hand-off at
 * construction time and never change afterwards - this screen only customizes save/share/toast
 * state, never the strip's content or styling (that's Customize's job, one screen back).
 */
data class ShareUiState(
    val image: ImageBitmap,
    val treatmentCode: String,
    val frameColor: FrameColorPreset,
    val layout: StripLayout,
    val stamp: String,
    val brand: String = Brand.NAME,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val savedPath: String? = null,
    val saveError: String? = null,
    // Transient toast copy (design/handoff/README.md's Share screen: "toast on save/share:
    // sparkle + confirmation copy, auto-dismiss ~1.8s") - the com.dj.photobooth.ui.Toast
    // composable owns its own dismiss timer, so this just needs to go back to null once shown.
    val toastMessage: String? = null,
)

/**
 * MVVM ViewModel for the Share screen - owns exactly the save/share responsibility
 * [com.dj.photobooth.preview.StripPreviewViewModel] used to own before this rebrand (its old
 * `onSavePng()`/`onShare()`), now split out because Customize's "continue" no longer saves
 * anything itself: it hands the already-composed strip over via
 * [com.dj.photobooth.nav.SessionHandoffViewModel.composedStrip], and this screen is where
 * "save to photos" and "share" actually happen.
 *
 * [composedStrip] is a plain constructor value, not a StateFlow - it is already-composed,
 * immutable data handed over exactly once when this ViewModel is created (see
 * PhotoboothNavHost's `Route.Share` composable), never mutated after that; only [uiState]'s
 * save-related fields change over this screen's lifetime.
 *
 * [galleryRepo]/[mediaRepo]/[shareSheet] are the same dependency shape
 * StripPreviewViewModel already had - just narrowed to only this screen's responsibility.
 * [shareSheet] stays nullable for the same reason it always has been: the screen (and tests)
 * can be exercised without a platform share sheet wired in.
 */
class ShareViewModel(
    private val composedStrip: ComposedStrip,
    private val galleryRepo: GalleryRepo,
    private val mediaRepo: MediaRepo,
    private val shareSheet: ShareSheet? = null,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ShareUiState(
            image = composedStrip.image,
            treatmentCode = composedStrip.treatmentCode,
            frameColor = composedStrip.frameColor,
            layout = composedStrip.layout,
            stamp = composedStrip.stamp,
        )
    )
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    /** "save to photos" - PNG-encode + MediaRepo write + GalleryRepo archive: exactly what
     *  StripPreviewViewModel's old onSavePng() did inline on Customize's former "SAVE PNG"
     *  button, moved here unchanged in substance. */
    fun onSaveToPhotos() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val pngBytes = withContext(workDispatcher) { encodeImageBitmapToPng(composedStrip.image) }
                val displayName = "photobooth-${currentEpochMillis()}"
                val savedPath = mediaRepo.savePng(pngBytes, displayName)
                galleryRepo.save(
                    HistoryEntry(
                        finalImagePath = savedPath,
                        // TODO(follow-up): generate a real downsampled thumbnail
                        // (architecture.md's "Performance" non-functional note) instead of
                        // reusing the full-res path - the same deferred item
                        // StripPreviewViewModel's old onSavePng() TODO'd, unchanged by this move.
                        thumbnailPath = savedPath,
                        filmTreatmentId = composedStrip.treatmentCode,
                        createdAt = currentEpochMillis(),
                        stamp = composedStrip.stamp,
                    )
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saved = true,
                        savedPath = savedPath,
                        // dc.html's doSave(): "saved! find it in strips".
                        toastMessage = "saved! find it in strips",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message ?: "Save failed") }
            }
        }
    }

    /** "share" - native share sheet, same shape/limitation as StripPreviewViewModel's old
     *  onShare(): only meaningful once a save has actually produced a path, and only on
     *  platforms that wired a [ShareSheet] in. */
    fun onShare() {
        val path = _uiState.value.savedPath ?: return
        val sheet = shareSheet ?: return
        viewModelScope.launch {
            sheet.shareImage(path, "photobooth-strip")
            // dc.html's doShare(): "opening your share sheet…".
            _uiState.update { it.copy(toastMessage = "opening your share sheet…") }
        }
    }

    /** The Toast composable's own auto-dismiss timer calls this back once it elapses. */
    fun onToastDismissed() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
