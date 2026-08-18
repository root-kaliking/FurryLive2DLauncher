package com.furry.live2dlauncher.live2d

import android.graphics.Canvas
import android.graphics.RectF
import android.util.Log

/**
 * 官方 Cubism SDK 渲染器集成点。
 *
 * 接入方式（需开发者手动完成，因官方 SDK 需授权下载）：
 *  1. 从 Live2D 官网下载 Cubism SDK for Android（Native 或 Java 版）
 *  2. 将 aar/so 放入 app/libs，并在 app/build.gradle.kts 添加依赖
 *  3. 将 [CubismRenderer.ENGINE_CLASS] 指向官方入口类
 *
 * 若 SDK 未集成，本类自动回退到 [PlaceholderRenderer]，保证应用开箱即用。
 */
class CubismRenderer : Live2DEngine {

    companion object {
        private const val TAG = "CubismRenderer"
        /** 官方 SDK 入口类（按实际接入的 SDK 调整） */
        private const val ENGINE_CLASS = "com.live2d.sdk.cubism.framework.CubismFramework"
    }

    private val fallback = PlaceholderRenderer()
    private var sdkAvailable = false

    override val isReady: Boolean
        get() = sdkAvailable && fallback.isReady

    override val modelName: String
        get() = if (sdkAvailable) fallback.modelName else fallback.modelName

    override fun init() {
        sdkAvailable = try {
            Class.forName(ENGINE_CLASS)
            Log.i(TAG, "Cubism SDK detected")
            true
        } catch (e: ClassNotFoundException) {
            Log.i(TAG, "Cubism SDK not integrated, using placeholder renderer")
            false
        }
        fallback.init()
    }

    override fun release() = fallback.release()

    override fun update(dt: Float) = fallback.update(dt)

    override fun render(canvas: Canvas) = fallback.render(canvas)

    override fun setPosition(x: Float, y: Float) = fallback.setPosition(x, y)

    override fun setScale(scale: Float) = fallback.setScale(scale)

    override fun onTouch(pointers: Int) = fallback.onTouch(pointers)

    override fun setTargetFps(fps: Int) = fallback.setTargetFps(fps)

    override fun getBounds(): RectF = fallback.getBounds()

    override fun loadModel(path: String?): Boolean = fallback.loadModel(path)
}
