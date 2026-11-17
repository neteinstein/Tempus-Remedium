package org.mariacf.tempusremedium.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.mariacf.tempusremedium.ui.PhotoStore
import org.mariacf.tempusremedium.ui.theme.TempusTheme

/** A stored photo, cropped to fill [modifier]'s size; a soft placeholder while it loads or if it is gone. */
@Composable
fun SymptomPhoto(
    id: String,
    photos: PhotoStore,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
) {
    val bitmap by produceState<ImageBitmap?>(null, id, photos) {
        value = photos.load(id)?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
    }
    val shape = RoundedCornerShape(corner)
    val image = bitmap
    if (image == null) {
        Box(modifier.clip(shape).background(TempusTheme.colors.line))
    } else {
        Image(image, contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = modifier.clip(shape))
    }
}
