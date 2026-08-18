package com.furry.live2dlauncher

import android.app.Application
import com.furry.live2dlauncher.audio.SoundManager
import com.furry.live2dlauncher.core.Prefs
import com.furry.live2dlauncher.core.ThemeManager

/**
 * 应用入口：初始化配置、主题、音效。
 */
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        // 应用主题模式（深/浅/跟随系统）
        ThemeManager.applyMode(Prefs.loadConfig().themeMode)
        // 初始化音效系统
        SoundManager.init(this)
    }
}
