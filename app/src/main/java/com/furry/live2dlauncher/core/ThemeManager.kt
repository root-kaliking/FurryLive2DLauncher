package com.furry.live2dlauncher.core

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题管理：深色/浅色/跟随系统三模式切换。
 * 默认跟随系统自动切换，适配日常护眼与不同使用场景。
 */
object ThemeManager {

    /** 应用主题模式（需在 Activity 创建前调用） */
    fun applyMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /** 当前是否深色模式 */
    fun isDark(context: Context): Boolean {
        val cfg = context.resources.configuration
        return cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    /** 根据当前主题返回背景色（供壁纸层透明化参考） */
    fun backgroundColor(context: Context): Int =
        if (isDark(context)) 0xFF17120F.toInt() else 0xFFFBF6F0.toInt()

    /** 沉浸式全屏：隐藏状态栏与导航栏 */
    fun applyImmersive(activity: Activity) {
        activity.window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}
