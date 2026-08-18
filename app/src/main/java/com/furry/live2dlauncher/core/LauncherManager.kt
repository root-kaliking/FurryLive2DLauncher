package com.furry.live2dlauncher.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 桌面项：应用图标或文件夹 */
sealed class HomeItem {
    abstract val position: Int

    data class AppIcon(
        val packageName: String,
        override val position: Int
    ) : HomeItem()

    data class Folder(
        val id: String,
        var name: String,
        override val position: Int,
        val appPackages: MutableList<String>
    ) : HomeItem()
}

/**
 * 桌面布局管理器：管理每个分页的图标网格与文件夹。
 * 布局以 JSON 持久化，支持多分页独立配置。
 */
class LauncherManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("furry_home_layout", Context.MODE_PRIVATE)
    private val keyPrefix = "page_"

    /** 获取某分页的桌面项列表（按位置排序） */
    fun loadPage(page: Int): List<HomeItem> {
        val raw = prefs.getString(keyPrefix + page, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                when (o.optString("type")) {
                    "app" -> HomeItem.AppIcon(o.getString("pkg"), o.getInt("pos"))
                    "folder" -> HomeItem.Folder(
                        o.getString("id"),
                        o.optString("name", "文件夹"),
                        o.getInt("pos"),
                        (o.optJSONArray("apps")?.let { a ->
                            (0 until a.length()).map { a.getString(it) }.toMutableList()
                        } ?: mutableListOf())
                    )
                    else -> null
                }
            }.sortedBy { it.position }
        } catch (e: Exception) { emptyList() }
    }

    /** 保存某分页的桌面项 */
    fun savePage(page: Int, items: List<HomeItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            when (item) {
                is HomeItem.AppIcon -> arr.put(JSONObject().apply {
                    put("type", "app"); put("pkg", item.packageName); put("pos", item.position)
                })
                is HomeItem.Folder -> arr.put(JSONObject().apply {
                    put("type", "folder"); put("id", item.id); put("name", item.name); put("pos", item.position)
                    put("apps", JSONArray().apply { item.appPackages.forEach { put(it) } })
                })
            }
        }
        prefs.edit().putString(keyPrefix + page, arr.toString()).apply()
    }

    /** 在指定分页添加应用图标 */
    fun addApp(page: Int, packageName: String, position: Int) {
        val items = loadPage(page).toMutableList()
        if (items.any { it is HomeItem.AppIcon && it.packageName == packageName }) return
        items.add(HomeItem.AppIcon(packageName, position))
        savePage(page, items)
    }

    /** 移除应用图标 */
    fun removeApp(page: Int, packageName: String) {
        val items = loadPage(page).filterNot { it is HomeItem.AppIcon && it.packageName == packageName }
        savePage(page, items)
    }

    /** 创建文件夹 */
    fun createFolder(page: Int, position: Int, name: String = "文件夹"): HomeItem.Folder {
        val folder = HomeItem.Folder(
            id = "folder_${System.currentTimeMillis()}",
            name = name,
            position = position,
            appPackages = mutableListOf()
        )
        val items = loadPage(page).toMutableList()
        items.add(folder)
        savePage(page, items)
        return folder
    }

    /** 向文件夹添加应用 */
    fun addAppToFolder(page: Int, folderId: String, packageName: String) {
        val items = loadPage(page).toMutableList()
        val idx = items.indexOfFirst { it is HomeItem.Folder && it.id == folderId }
        if (idx >= 0) {
            val f = items[idx] as HomeItem.Folder
            if (packageName !in f.appPackages) f.appPackages.add(packageName)
            savePage(page, items)
        }
    }

    /** 从文件夹移除应用 */
    fun removeAppFromFolder(page: Int, folderId: String, packageName: String) {
        val items = loadPage(page).toMutableList()
        val idx = items.indexOfFirst { it is HomeItem.Folder && it.id == folderId }
        if (idx >= 0) {
            val f = items[idx] as HomeItem.Folder
            f.appPackages.remove(packageName)
            savePage(page, items)
        }
    }

    /** 重命名文件夹 */
    fun renameFolder(page: Int, folderId: String, name: String) {
        val items = loadPage(page).toMutableList()
        val idx = items.indexOfFirst { it is HomeItem.Folder && it.id == folderId }
        if (idx >= 0) {
            (items[idx] as HomeItem.Folder).name = name
            savePage(page, items)
        }
    }

    /** 删除文件夹（保留内部应用，仅移除文件夹容器） */
    fun deleteFolder(page: Int, folderId: String) {
        val items = loadPage(page).filterNot { it is HomeItem.Folder && it.id == folderId }
        savePage(page, items)
    }
}
