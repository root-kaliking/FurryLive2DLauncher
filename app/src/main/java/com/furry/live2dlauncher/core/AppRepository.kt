package com.furry.live2dlauncher.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 桌面上的一个应用项 */
data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val launchIntent: Intent?
)

/**
 * 应用信息仓库：查询已安装应用、解析启动 Intent。
 * 桌面图标与应用抽屉共用。
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** 查询所有可启动应用（排除系统设置类与自身） */
    suspend fun loadLaunchableApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val selfPkg = context.packageName
        resolveInfos
            .asSequence()
            .filter { it.activityInfo.packageName != selfPkg }
            .mapNotNull { ri ->
                val ai = ri.activityInfo
                val label = try {
                    ai.loadLabel(pm).toString()
                } catch (e: Exception) { ai.packageName }
                val launch = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(ai.packageName, ai.name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                AppItem(
                    packageName = ai.packageName,
                    label = label,
                    icon = try { ai.loadIcon(pm) } catch (e: Exception) { null },
                    launchIntent = launch
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** 解析包名对应的图标与标签（用于桌面图标持久化） */
    fun resolveIcon(packageName: String): Drawable? = try {
        pm.getApplicationIcon(packageName)
    } catch (e: Exception) { null }

    fun resolveLabel(packageName: String): String = try {
        val ai = pm.getApplicationInfo(packageName, 0)
        ai.loadLabel(pm).toString()
    } catch (e: Exception) { packageName }

    fun isSystemApp(packageName: String): Boolean = try {
        val ai = pm.getApplicationInfo(packageName, 0)
        (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    } catch (e: Exception) { false }
}
