package com.furry.live2dlauncher.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.furry.live2dlauncher.core.HomeItem
import com.furry.live2dlauncher.core.Prefs

/**
 * 桌面分页适配器。
 *
 * 每个分页对应一个 [IconGridView]，实现多分页独立配置
 * （每页可配置专属 Live2D 模型、壁纸样式与图标方案）。
 */
class HomePagerAdapter(
    private val pageCount: Int,
    private val onAppClick: (String) -> Unit,
    private val onFolderClick: (HomeItem.Folder) -> Unit,
    private val onItemLongPress: (HomeItem) -> Unit
) : RecyclerView.Adapter<HomePagerAdapter.PageHolder>() {

    private val grids = HashMap<Int, IconGridView>()

    inner class PageHolder(val grid: IconGridView) : RecyclerView.ViewHolder(grid)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val grid = IconGridView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.onAppClick = this@HomePagerAdapter.onAppClick
            this.onFolderClick = this@HomePagerAdapter.onFolderClick
            this.onItemLongPress = this@HomePagerAdapter.onItemLongPress
        }
        return PageHolder(grid)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.grid.bindPage(position)
        grids[position] = holder.grid
    }

    override fun getItemCount(): Int = pageCount

    /** 刷新指定分页 */
    fun refreshPage(position: Int) {
        grids[position]?.reload()
    }

    /** 刷新所有分页 */
    fun refreshAll() {
        grids.values.forEach { it.reload() }
    }
}
