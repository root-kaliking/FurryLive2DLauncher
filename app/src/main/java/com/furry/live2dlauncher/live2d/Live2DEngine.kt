package com.furry.live2dlauncher.live2d

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Live2D 渲染引擎抽象层。
 *
 * 两种实现：
 *  - [PlaceholderRenderer]：内置占位兽耳角色渲染器（无需任何 SDK，开箱即用）
 *  - [CubismRenderer]：官方 Cubism SDK 集成点（需将官方 SDK 放入 libs 后启用）
 *
 * 上层 [Live2DOverlayView] 只依赖本接口，实现可热切换。
 */
interface Live2DEngine {

    /** 引擎是否已就绪（模型加载成功） */
    val isReady: Boolean

    /** 当前模型名称（用于 UI 展示） */
    val modelName: String

    /** 初始化引擎 */
    fun init()

    /** 释放引擎资源 */
    fun release()

    /** 更新一帧动画（dt 秒） */
    fun update(dt: Float)

    /** 渲染到画布（已按模型位置/缩放变换） */
    fun render(canvas: Canvas)

    /** 设置模型位置（归一化坐标 0..1） */
    fun setPosition(x: Float, y: Float)

    /** 设置模型缩放 */
    fun setScale(scale: Float)

    /** 触发一次触摸反馈（单点/多点） */
    fun onTouch(pointers: Int)

    /** 设置渲染帧率（用于省电降帧） */
    fun setTargetFps(fps: Int)

    /** 模型实际绘制区域（用于手势屏蔽区域计算） */
    fun getBounds(): RectF

    /** 加载本地模型工程文件（Cubism 实现使用；占位实现忽略） */
    fun loadModel(path: String?): Boolean
}
