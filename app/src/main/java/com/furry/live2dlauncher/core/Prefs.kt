package com.furry.live2dlauncher.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 配置持久化封装。所有配置以 JSON 形式存储于 SharedPreferences，
 * 便于一键备份/还原（导出/导入同一 JSON 字符串）。
 */
object Prefs {
    private const val NAME = "furry_launcher_prefs"
    private const val KEY_CONFIG = "launcher_config"
    private const val KEY_LAST_MODEL = "last_model_path"

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    fun loadConfig(): LauncherConfig {
        val raw = sp.getString(KEY_CONFIG, null)
        return try {
            if (raw.isNullOrBlank()) LauncherConfig() else LauncherConfig.fromJson(raw)
        } catch (e: Exception) {
            LauncherConfig()
        }
    }

    fun saveConfig(cfg: LauncherConfig) {
        sp.edit().putString(KEY_CONFIG, cfg.toJson().toString()).apply()
    }

    fun lastModelPath(): String? = sp.getString(KEY_LAST_MODEL, null)

    fun setLastModelPath(path: String?) {
        sp.edit().putString(KEY_LAST_MODEL, path).apply()
    }

    /** 导出完整配置 JSON（用于备份） */
    fun exportConfig(): String = loadConfig().toJson().toString()

    /** 从 JSON 还原配置 */
    fun importConfig(json: String): Boolean = try {
        saveConfig(LauncherConfig.fromJson(json))
        true
    } catch (e: Exception) {
        false
    }
}
