package com.furry.live2dlauncher.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全量配置备份与还原。
 * 备份内容：桌面布局、模型参数、壁纸设置、音效配置等所有自定义数据。
 */
class BackupManager(private val context: Context) {

    private val backupDir: File
        get() = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    /** 一键备份：导出完整配置到备份目录 */
    suspend fun backup(): File? = withContext(Dispatchers.IO) {
        try {
            val cfgJson = Prefs.exportConfig()
            val layoutJson = exportHomeLayout()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "furry_backup_$stamp.json")
            file.writeText("""{"config":$cfgJson,"home":$layoutJson}""")
            file
        } catch (e: Exception) { null }
    }

    /** 一键还原：从备份文件恢复 */
    suspend fun restore(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext false
            val root = org.json.JSONObject(text)
            val cfg = root.optJSONObject("config")?.toString()
            if (cfg != null) Prefs.importConfig(cfg)
            root.optJSONObject("home")?.let { importHomeLayout(it) }
            true
        } catch (e: Exception) { false }
    }

    private fun exportHomeLayout(): String {
        val root = org.json.JSONObject()
        val manager = LauncherManager(context)
        val pages = Prefs.loadConfig().pageCount
        val arr = org.json.JSONArray()
        for (p in 0 until pages) {
            arr.put(org.json.JSONObject().apply {
                put("page", p)
                put("items", org.json.JSONArray(manager.loadPage(p).map { item ->
                    when (item) {
                        is HomeItem.AppIcon -> org.json.JSONObject().apply {
                            put("type", "app"); put("pkg", item.packageName); put("pos", item.position)
                        }
                        is HomeItem.Folder -> org.json.JSONObject().apply {
                            put("type", "folder"); put("id", item.id); put("name", item.name); put("pos", item.position)
                            put("apps", org.json.JSONArray(item.appPackages))
                        }
                    }
                }))
            })
        }
        root.put("pages", arr)
        return root.toString()
    }

    private fun importHomeLayout(root: org.json.JSONObject) {
        val manager = LauncherManager(context)
        val pages = root.optJSONArray("pages") ?: return
        for (i in 0 until pages.length()) {
            val p = pages.getJSONObject(i)
            val page = p.getInt("page")
            val items = p.optJSONArray("items") ?: continue
            val list = mutableListOf<HomeItem>()
            for (j in 0 until items.length()) {
                val o = items.getJSONObject(j)
                when (o.optString("type")) {
                    "app" -> list.add(HomeItem.AppIcon(o.getString("pkg"), o.getInt("pos")))
                    "folder" -> list.add(HomeItem.Folder(
                        o.getString("id"), o.optString("name", "文件夹"), o.getInt("pos"),
                        (o.optJSONArray("apps")?.let { a ->
                            (0 until a.length()).map { a.getString(it) }.toMutableList()
                        } ?: mutableListOf())
                    ))
                }
            }
            manager.savePage(page, list)
        }
    }

    /** 列出已有备份文件 */
    fun listBackups(): List<File> = backupDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
}
