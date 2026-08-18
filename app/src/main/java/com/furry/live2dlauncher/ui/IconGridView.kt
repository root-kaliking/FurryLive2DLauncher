package com.furry.live2dlauncher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.furry.live2dlauncher.R
import com.furry.live2dlauncher.core.AppRepository
import com.furry.live2dlauncher.core.HomeItem
import com.furry.live2dlauncher.core.LauncherManager
import com.furry.live2dlauncher.core.Prefs

/**
 * 桌面图标网格。
 *
 * 按配置的行列数自动布局图标与文件夹，支持：
 *  - 点击启动应用 / 打开文件夹
 *  - 长按进入编辑模式（拖动排序、移除）
 *  - 每分页独立实例，实现多页面差异化展示
 */
class IconGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val appRepo = AppRepository(context)
    private val manager = LauncherManager(context)
    private var page = 0
    private var items: List<HomeItem> = emptyList()

    var onAppClick: ((String) -> Unit)? = null
    var onFolderClick: ((HomeItem.Folder) -> Unit)? = null
    var onItemLongPress: ((HomeItem) -> Unit)? = null

    fun bindPage(page: Int) {
        this.page = page
        reload()
    }

    fun reload() {
        items = manager.loadPage(page)
        removeAllViews()
        val cfg = Prefs.loadConfig()
        val columns = cfg.columns.coerceAtLeast(3)
        val rows = cfg.rows.coerceAtLeast(4)
        val cellW = width / columns
        val cellH = height / rows
        if (cellW <= 0 || cellH <= 0) return

        items.forEach { item ->
            val cell = createCell(item)
            val row = item.position / columns
            val col = item.position % columns
            val lp = LayoutParams(cellW, cellH)
            lp.leftMargin = col * cellW
            lp.topMargin = row * cellH
            addView(cell, lp)
        }
    }

    private fun createCell(item: HomeItem): View {
        val cell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isLongClickable = true
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50))
            background = context.getDrawable(R.drawable.bg_icon)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val label = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            maxLines = 1
            gravity = Gravity.CENTER
        }

        when (item) {
            is HomeItem.AppIcon -> {
                icon.setImageDrawable(appRepo.resolveIcon(item.packageName))
                label.text = appRepo.resolveLabel(item.packageName)
                cell.setOnClickListener {
                    onAppClick?.invoke(item.packageName)
                }
            }
            is HomeItem.Folder -> {
                icon.setImageResource(R.drawable.ic_folder)
                icon.setColorFilter(0xFFFFFFFF.toInt())
                label.text = item.name
                cell.setOnClickListener {
                    onFolderClick?.invoke(item)
                }
            }
        }
        cell.setOnLongClickListener {
            onItemLongPress?.invoke(item)
            true
        }
        cell.addView(icon)
        cell.addView(label)
        return cell
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && oldw != w) reload()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
