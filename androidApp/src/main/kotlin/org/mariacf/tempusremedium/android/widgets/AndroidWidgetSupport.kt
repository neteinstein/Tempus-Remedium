package org.mariacf.tempusremedium.android.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import org.mariacf.tempusremedium.ui.WidgetSupport

/** Finds the app's widgets on any home or lock screen and asks the launcher to add one. */
class AndroidWidgetSupport(
    private val context: Context,
) : WidgetSupport {
    private val manager get() = AppWidgetManager.getInstance(context)

    override fun isPlaced(): Boolean =
        listOf(HojeWidgetReceiver::class.java, ReporEmBreveWidgetReceiver::class.java).any { receiver ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
        }

    override fun canPin(): Boolean = manager.isRequestPinAppWidgetSupported

    override fun pin() {
        manager.requestPinAppWidget(ComponentName(context, HojeWidgetReceiver::class.java), null, null)
    }
}
