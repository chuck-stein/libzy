package io.libzy.ui.common.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/**
 * Load a remote image located at [url].
 *
 * @param url The image URL to load. If null, then the returned [ImageBitmap] will always be null.
 * @return An [ImageBitmap] representing the loaded image, or null if the image is still loading.
 *         It is recommended to display a placeholder image of the same size while the [ImageBitmap] is null.
 *         Once the image loads, it will trigger a recomposition so that the returned value will no longer be null.
 */
@Composable
fun loadRemoteImage(url: String?): ImageBitmap? {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(url, context) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageBitmap = resource.asImageBitmap()
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    // no-op
                }
            })
    }
    
    return imageBitmap
}
