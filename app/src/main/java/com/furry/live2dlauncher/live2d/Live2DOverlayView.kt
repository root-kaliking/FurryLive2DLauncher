package com.furry.live2dlauncher.live2d

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.furry.live2dlauncher.audio.SoundManager
import com.furry.live2dlauncher.core.Prefs
import kotlin.math.hypot

/**
 * 覆盖在桌面上的 Live2D 模型视图。
 *
 * 职责：
 *  - 按目标帧率驱动渲染循环（支持省电降帧）
 *  - 单指拖动模型位置
 *  - 双指缩放模型尺寸
 *  - 区分单点/多点触摸，触发差异化骨骼动作反馈
 *  - 支持手势屏蔽区域，避免模型遮挡图标导致误触
 */
class Live2DOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var engine: Live2DEngine = CubismRenderer()
    private var renderThread: Thread? = null
    private var running = false
    private var targetFps = 30

    // 手势状态
    private val activePointers = HashMap<Int, Pair<Float, Float>>()
    private var dragging = false
    private var pinchStartDist = 0f
    private var pinchStartScale = 1f
    private var lastTouchTime = 0L

    // 触摸回调（供音效系统使用）
    var onModelTouched: ((pointers: Int) -> Unit)? = null

    init {
        engine.init()
        // 从配置加载模型参数
        val cfg = Prefs.loadConfig()
        targetFps = cfg.live2dFps
        engine.setTargetFps(targetFps)
        engine.setPosition(cfg.modelX, cfg.modelY)
        engine.setScale(cfg.modelScale)
        engine.loadModel(Prefs.lastModelPath())
        startRenderLoop()
    }

    /** 重新加载配置（设置页修改后调用） */
    fun reloadConfig() {
        val cfg = Prefs.loadConfig()
        targetFps = cfg.live2dFps
        engine.setTargetFps(targetFps)
        engine.setPosition(cfg.modelX, cfg.modelY)
        engine.setScale(cfg.modelScale)
        engine.loadModel(Prefs.lastModelPath())
    }

    /** 切换引擎实现（预留：接入 Cubism SDK 后切换） */
    fun setEngine(newEngine: Live2DEngine) {
        engine.release()
        engine = newEngine
        engine.init()
        val cfg = Prefs.loadConfig()
        engine.setTargetFps(cfg.live2dFps)
        engine.setPosition(cfg.modelX, cfg.modelY)
        engine.setScale(cfg.modelScale)
    }

    /** 当前模型绘制区域（用于屏蔽区域计算） */
    fun getModelBounds(): RectF = engine.getBounds()

    private fun startRenderLoop() {
        running = true
        renderThread = Thread {
            var last = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val dt = (now - last) / 1_000_000_000f
                last = now
                engine.update(dt.coerceAtMost(0.05f))
                postInvalidateOnAnimation()
                val interval = 1000L / targetFps
                try {
                    Thread.sleep(interval.coerceAtLeast(8))
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { isDaemon = true; name = "live2d-render" }
        renderThread?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        engine.render(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        running = false
        renderThread?.interrupt()
        engine.release()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cfg = Prefs.loadConfig()
        // 手势屏蔽区域：区域内不响应模型触摸，避免遮挡图标误触
        cfg.gestureBlockRect?.let { block ->
            if (block.width() > 0 && block.height() > 0 &&
                block.contains(event.x, event.y)
            ) {
                return false
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointers[event.getPointerId(0)] = event.x to event.y
                dragging = true
                lastTouchTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val id = event.getPointerId(event.actionIndex)
                activePointers[id] = event.getX(event.actionIndex) to event.getY(event.actionIndex)
                if (activePointers.size == 2) {
                    dragging = false
                    val p = activePointers.values.toList()
                    pinchStartDist = hypot(p[0].first - p[1].first, p[0].second - p[1].second)
                    pinchStartScale = engine.getBounds().width() / maxOf(width, 1)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    activePointers[id] = event.getX(i) to event.getY(i)
                }
                if (activePointers.size == 1 && dragging) {
                    // 单指拖动
                    val (x, y) = activePointers.values.first()
                    engine.setPosition(x / width, y / height)
                } else if (activePointers.size == 2) {
                    // 双指缩放
                    val p = activePointers.values.toList()
                    val dist = hypot(p[0].first - p[1].first, p[0].second - p[1].second)
                    if (pinchStartDist > 0f) {
                        val ratio = dist / pinchStartDist
                        val base = Prefs.loadConfig().modelScale
                        engine.setScale((base * ratio).coerceIn(0.3f, 3f))
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                activePointers.remove(id)
                if (activePointers.size == 1) dragging = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 触摸结束：触发骨骼动作反馈 + 音效
                val pointers = activePointers.size.coerceAtLeast(1)
                engine.onTouch(pointers)
                onModelTouched?.invoke(pointers)
                SoundManager.playTouch()
                activePointers.clear()
                dragging = false
                // 持久化位置
                val cfg = Prefs.loadConfig()
                cfg.modelX = engine.getBounds().centerX() / maxOf(width, 1)
                cfg.modelY = engine.getBounds().centerY() / maxOf(height, 1)
                cfg.modelScale = (engine.getBounds().width() / maxOf(width, 1)).coerceIn(0.3f, 3f)
                Prefs.saveConfig(cfg)
            }
        }
        return true
    }
}
