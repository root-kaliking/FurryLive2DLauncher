package com.furry.live2dlauncher.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.furry.live2dlauncher.R
import com.furry.live2dlauncher.ui.LauncherActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Furry 时钟小组件：显示实时时间与日期。
 */
class FurryClockWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FurryClockWidget::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            val now = Date()
            views.setTextViewText(
                R.id.widget_time,
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            )
            views.setTextViewText(
                R.id.widget_date,
                SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault()).format(now)
            )
            // 点击回到桌面
            val intent = Intent(context, LauncherActivity::class.java)
            val pi = PendingIntent.getActivity(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_time, pi)
            manager.updateAppWidget(id, views)
        }
    }
}
