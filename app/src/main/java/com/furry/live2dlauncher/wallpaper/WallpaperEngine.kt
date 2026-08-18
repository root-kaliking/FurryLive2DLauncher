package com.furry.live2dlauncher.wallpaper

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import com.furry.live2dlauncher.core.Prefs
import com.furry.live2dlauncher.core.SceneType
import com.furry.live2dlauncher.core.WallpaperType

/**
 * 壁纸引擎：管理桌面壁纸层。
 * 支持三种壁纸来源：内置场景、本地图片、本地视频。
 * 每个桌面分页可独立配置壁纸（对应 PRD 多页面差异化展示）。
 */
class WallpaperEngine(private val container: FrameLayout) {

    private val context: Context = container.context
    private var sceneView: SceneWallpaperView? = null
    private var imageView: ImageWallpaperView? = null
    private var videoView: VideoWallpaperView? = null
    private var currentType = -1

    /** 应用指定分页的壁纸配置 */
    fun applyPage(page: Int) {
        val cfg = Prefs.loadConfig()
        val pageCfg = cfg.pages.getOrNull(page) ?: return
        apply(pageCfg.wallpaperType, pageCfg.sceneType, pageCfg.wallpaperPath)
    }

    fun apply(type: Int, sceneType: Int, path: String?) {
        if (type == currentType && type != WallpaperType.SCENE) {
            // 同类型切换（仅更新内容）
            when (type) {
                WallpaperType.IMAGE -> imageView?.loadImage(path?.let { Uri.parse(it) })
                WallpaperType.VIDEO -> videoView?.loadVideo(path?.let { Uri.parse(it) })
            }
            return
        }
        currentType = type
        container.removeAllViews()
        sceneView = null; imageView = null; videoView = null

        when (type) {
            WallpaperType.SCENE -> {
                val sv = SceneWallpaperView(context)
                sv.setScene(sceneType)
                container.addView(sv, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                sceneView = sv
            }
            WallpaperType.IMAGE -> {
                val iv = ImageWallpaperView(context)
                iv.loadImage(path?.let { Uri.parse(it) })
                container.addView(iv, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                imageView = iv
            }
            WallpaperType.VIDEO -> {
                val vv = VideoWallpaperView(context)
                vv.loadVideo(path?.let { Uri.parse(it) })
                container.addView(vv, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                videoView = vv
            }
        }
    }

    /** 配置变更后刷新（粒子开关、帧率等） */
    fun reloadConfig() {
        sceneView?.reloadConfig()
    }

    /** 释放资源（Activity 销毁时） */
    fun release() {
        videoView?.release()
        container.removeAllViews()
    }
}
