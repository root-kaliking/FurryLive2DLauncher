package com.furry.live2dlauncher.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.furry.live2dlauncher.R

/**
 * Furry 语录小组件：随机展示 Furry 风格短句。
 */
class FurryQuoteWidget : AppWidgetProvider() {

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
        private val QUOTES = listOf(
            "毛茸茸的一天也要元气满满！" to "Furry 桌面",
            "尾巴摇一摇，烦恼全跑掉。" to "Furry 桌面",
            "今天也是被兽耳治愈的一天。" to "Furry 桌面",
            "在森林里，每个脚印都是故事。" to "Furry 桌面",
            "月光下的尾巴，藏着温柔。" to "Furry 桌面",
            "勇敢的兽人，从不害怕黑夜。" to "Furry 桌面"
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FurryQuoteWidget::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val (quote, author) = QUOTES.random()
            val views = RemoteViews(context.packageName, R.layout.widget_quote)
            views.setTextViewText(R.id.widget_quote, quote)
            views.setTextViewText(R.id.widget_quote_author, author)
            manager.updateAppWidget(id, views)
        }
    }
}
