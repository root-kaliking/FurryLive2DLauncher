package com.furry.live2dlauncher.wallpaper

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.furry.live2dlauncher.core.Prefs
import com.furry.live2dlauncher.core.SceneType
import com.furry.live2dlauncher.core.WallpaperType

/**
 * 动态壁纸服务。
 *
 * 用户可将其设为系统壁纸，锁屏界面即同步加载当前生效的 Live2D 模型与
 * 动态壁纸（对应 PRD"锁屏界面可同步加载当前生效的 Live2D 模型与动态壁纸"）。
 *
 * 注意：Android 锁屏壁纸由系统渲染，本服务通过 WallpaperService 提供
 * 与桌面一致的场景壁纸渲染；Live2D 模型层在锁屏上由系统限制，桌面端
 * 通过 [com.furry.live2dlauncher.lockscreen.LockScreenService] 做低帧率联动。
 */
class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SceneEngine()

    private inner class SceneEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var sceneView: SceneWallpaperView? = null
        private var visible = false
        private val frameRunnable = object : Runnable {
            override fun run() {
                if (!visible) return
                sceneView?.invalidate()
                handler.postDelayed(this, 1000L / targetFps)
            }
        }
        private var targetFps = 24

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val cfg = Prefs.loadConfig()
            targetFps = cfg.wallpaperFps
            sceneView = SceneWallpaperView(this@LiveWallpaperService).apply {
                setScene(cfg.pages.getOrNull(0)?.sceneType ?: SceneType.STARFIELD)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(frameRunnable)
            } else {
                handler.removeCallbacks(frameRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onDraw(surfaceHolder: SurfaceHolder) {
            val canvas: Canvas = try {
                surfaceHolder.lockCanvas()
            } catch (e: Exception) {
                return
            }
            canvas.drawColor(0xFF17120F.toInt())
            sceneView?.let {
                it.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(surfaceHolder.surfaceFrame.width(), android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(surfaceHolder.surfaceFrame.height(), android.view.View.MeasureSpec.EXACTLY)
                )
                it.layout(0, 0, surfaceHolder.surfaceFrame.width(), surfaceHolder.surfaceFrame.height())
                it.draw(canvas)
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frameRunnable)
            sceneView = null
            super.onDestroy()
        }
    }
}
