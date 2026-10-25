package org.mariacf.tempusremedium.android.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import kotlinx.coroutines.runBlocking

/**
 * Draws Glance [content] into a bitmap as a launcher lays it out at [size]: composes it to
 * RemoteViews, inflates them and draws the view. Shared by the widget preview images and the
 * store listing's home screens.
 */
internal fun renderWidget(
    context: Context,
    size: DpSize,
    content: @Composable () -> Unit,
): Bitmap {
    val views = runBlocking { ContentWidget(content).compose(context, size = size) }
    val density = context.resources.displayMetrics.density
    val width = (size.width.value * density).toInt()
    val height = (size.height.value * density).toInt()
    val view = views.apply(context, FrameLayout(context))
    view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
    )
    view.layout(0, 0, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return bitmap
}

private class ContentWidget(
    private val content: @Composable () -> Unit,
) : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) = provideContent { content() }
}
