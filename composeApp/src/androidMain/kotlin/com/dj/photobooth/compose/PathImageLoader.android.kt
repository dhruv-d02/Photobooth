package com.dj.photobooth.compose

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AndroidPathImageLoader(private val context: Context) : PathImageLoader {
    override suspend fun load(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
actual fun rememberPathImageLoader(): PathImageLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidPathImageLoader(context) }
}
