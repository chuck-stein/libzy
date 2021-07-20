package io.libzy.ui.common

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/**
 * Load a remote image located at [url].
 *
 * @param url The image URL to load. If null, then the returned [State] will always be null.
 * @return A [State] that will be null until the image loads, at which point it will contain the corresponding
 *         [ImageBitmap]. It is recommended to display a placeholder image of the same size while the state is null.
 */
@Composable
fun loadRemoteImage(url: String?): State<ImageBitmap?> {
    val bitmapState: MutableState<ImageBitmap?> = remember { mutableStateOf(null) }
    val context = LocalContext.current

    LaunchedEffect(url, context) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    bitmapState.value = resource.asImageBitmap()
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    // no-op
                }
            })
    }
    
    return bitmapState
}
