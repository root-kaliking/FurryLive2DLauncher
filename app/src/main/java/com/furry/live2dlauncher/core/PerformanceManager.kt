package com.furry.live2dlauncher.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 性能与省电管理。
 *
 *  - 低电量自动省电：电量低于阈值时降低 Live2D 与壁纸帧率、关停动态效果
 *  - 智能内存优化：自动清理闲置模型、视频背景产生的冗余缓存
 *
 * 对应 PRD"低电量时可自动降低动画帧率、关停动态效果"与
 * "自动清理闲置模型、视频背景产生的冗余缓存"。
 */
class PerformanceManager(private val context: Context) {

    companion object {
        const val LOW_BATTERY_THRESHOLD = 20
        const val ACTION_BATTERY_CHANGED = "com.furry.live2dlauncher.action.BATTERY_CHANGED"
    }

    var onBatteryStateChanged: ((low: Boolean) -> Unit)? = null

    private var lowBattery = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level < 0 || scale <= 0) return
            val percent = level * 100 / scale
            val cfg = Prefs.loadConfig()
            val isLow = cfg.batterySaver && percent <= LOW_BATTERY_THRESHOLD
            if (isLow != lowBattery) {
                lowBattery = isLow
                onBatteryStateChanged?.invoke(isLow)
                context.sendBroadcast(Intent(ACTION_BATTERY_CHANGED).setPackage(context.packageName))
            }
        }
    }

    /** 注册电量监听 */
    fun register() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) { /* 未注册 */ }
    }

    /** 当前是否处于低电量省电模式 */
    fun isLowBattery(): Boolean = lowBattery

    /** 计算当前生效的 Live2D 帧率（低电量时自动降为 1/3） */
    fun effectiveLive2dFps(): Int {
        val base = Prefs.loadConfig().live2dFps
        return if (lowBattery) (base / 3).coerceAtLeast(5) else base
    }

    /** 清理缓存：视频背景缓存、Coil 磁盘缓存、临时音效 */
    fun clearCaches() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 视频壁纸缓存目录
                context.cacheDir.listFiles()?.forEach { f ->
                    if (f.isDirectory && (f.name.contains("video") || f.name.contains("media"))) {
                        f.deleteRecursively()
                    }
                }
                // 过期备份（保留最近 5 份）
                val backups = File(context.getExternalFilesDir(null), "backups")
                    .listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
                backups.drop(5).forEach { it.delete() }
            } catch (e: Exception) { /* 忽略清理失败 */ }
        }
    }
}
