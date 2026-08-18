package com.furry.live2dlauncher.live2d

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sin

/**
 * 内置占位兽耳角色渲染器。
 *
 * 无需任何第三方 SDK，使用原生 Canvas 绘制一只带呼吸、眨眼、摇尾、
 * 触摸反馈动画的狐狸兽人角色。用于：
 *  1. 未导入模型时的演示效果（对应 PRD"不内置预制模型资源"的过渡体验）
 *  2. 作为 Cubism SDK 未接入时的降级渲染
 *
 * 坐标约定：模型中心位于 (0,0)，单位高度 100，由外部矩阵做位置/缩放变换。
 */
class PlaceholderRenderer : Live2DEngine {

    override var isReady: Boolean = true
    override var modelName: String = "占位兽耳角色"

    // 动画状态
    private var time = 0f
    private var blinkTimer = 2f
    private var blinkPhase = 0f
    private var touchBoost = 0f
    private var targetFps = 30
    private var lastFrameTime = 0L

    // 位置与缩放（归一化）
    private var posX = 0.5f
    private var posY = 0.45f
    private var scale = 1f

    // 画布尺寸（用于归一化坐标换算）
    private var viewW = 1080f
    private var viewH = 1920f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 角色配色
    private val furColor = Color.rgb(255, 176, 96)        // 橙毛
    private val furDark = Color.rgb(232, 140, 60)         // 深橙
    private val bellyColor = Color.rgb(255, 236, 210)     // 白肚
    private val earInner = Color.rgb(255, 200, 160)       // 耳内
    private val blushColor = Color.argb(120, 255, 120, 120)
    private val eyeColor = Color.rgb(60, 40, 30)
    private val noseColor = Color.rgb(80, 50, 40)

    override fun init() {}

    override fun release() {}

    override fun update(dt: Float) {
        time += dt
        // 眨眼周期
        blinkTimer -= dt
        if (blinkTimer <= 0f) {
            blinkPhase = 1f
            blinkTimer = 2.5f + (Math.random() * 2f).toFloat()
        }
        if (blinkPhase > 0f) {
            blinkPhase -= dt * 6f
            if (blinkPhase < 0f) blinkPhase = 0f
        }
        // 触摸反馈衰减
        if (touchBoost > 0f) {
            touchBoost -= dt * 2f
            if (touchBoost < 0f) touchBoost = 0f
        }
    }

    override fun render(canvas: Canvas) {
        viewW = canvas.width.toFloat()
        viewH = canvas.height.toFloat()
        val cx = posX * viewW
        val cy = posY * viewH
        val s = scale * viewH / 100f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(s, s)

        // 呼吸：身体轻微上下浮动
        val breathe = sin(time * 2.2f) * 1.2f

        drawTail(canvas)
        drawBody(canvas, breathe)
        drawHead(canvas, breathe)

        canvas.restore()
    }

    private fun drawTail(canvas: Canvas) {
        val wag = sin(time * 3.0f) * 4f * (1f + touchBoost * 0.5f)
        val path = Path()
        path.moveTo(-14f, 12f)
        path.cubicTo(-30f, 8f + wag, -36f, -6f + wag, -26f, -14f + wag)
        path.cubicTo(-18f, -20f + wag, -12f, -10f, -12f, 6f)
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = furColor
        canvas.drawPath(path, paint)
        // 尾尖白
        paint.color = Color.WHITE
        val tip = Path()
        tip.moveTo(-30f, -4f + wag)
        tip.cubicTo(-36f, -6f + wag, -34f, -16f + wag, -26f, -14f + wag)
        tip.cubicTo(-28f, -8f + wag, -26f, -6f + wag, -30f, -4f + wag)
        tip.close()
        canvas.drawPath(tip, paint)
    }

    private fun drawBody(canvas: Canvas, breathe: Float) {
        // 身体
        paint.color = furColor
        canvas.drawOval(RectF(-18f, 6f + breathe, 18f, 46f + breathe), paint)
        // 肚皮
        paint.color = bellyColor
        canvas.drawOval(RectF(-10f, 14f + breathe, 10f, 42f + breathe), paint)
        // 领毛
        paint.color = furDark
        canvas.drawOval(RectF(-16f, 2f + breathe, 16f, 14f + breathe), paint)
    }

    private fun drawHead(canvas: Canvas, breathe: Float) {
        val headY = -8f + breathe
        val earLift = touchBoost * 4f  // 触摸时耳朵竖起

        // 耳朵（左）
        drawEar(canvas, -14f, headY, -1f, earLift)
        // 耳朵（右）
        drawEar(canvas, 14f, headY, 1f, earLift)

        // 头
        paint.color = furColor
        canvas.drawOval(RectF(-24f, headY - 22f, 24f, headY + 16f), paint)

        // 刘海/顶部深色
        paint.color = furDark
        canvas.drawArc(RectF(-24f, headY - 22f, 24f, headY + 4f), 180f, 180f, true, paint)

        // 眼睛（眨眼时眼睑下压）
        val eyeOpen = 1f - blinkPhase * 0.9f
        val eyeY = headY + 2f
        drawEye(canvas, -10f, eyeY, eyeOpen)
        drawEye(canvas, 10f, eyeY, eyeOpen)

        // 鼻子
        paint.color = noseColor
        canvas.drawOval(RectF(-2.5f, headY + 8f, 2.5f, headY + 11f), paint)

        // 腮红（触摸时加深）
        paint.color = blushColor
        val blushAlpha = (90 + touchBoost * 120).toInt().coerceAtMost(200)
        paint.alpha = blushAlpha
        canvas.drawOval(RectF(-20f, headY + 4f, -11f, headY + 10f), paint)
        canvas.drawOval(RectF(11f, headY + 4f, 20f, headY + 10f), paint)
        paint.alpha = 255
    }

    private fun drawEar(canvas: Canvas, x: Float, headY: Float, dir: Float, lift: Float) {
        val path = Path()
        val baseY = headY - 16f - lift
        path.moveTo(x - 7f, headY - 14f)
        path.lineTo(x + dir * 3f, baseY - 14f)
        path.lineTo(x + 7f, headY - 14f)
        path.close()
        paint.color = furColor
        canvas.drawPath(path, paint)
        // 耳内
        paint.color = earInner
        val inner = Path()
        inner.moveTo(x - 3f, headY - 12f)
        inner.lineTo(x + dir * 2f, baseY - 10f)
        inner.lineTo(x + 3f, headY - 12f)
        inner.close()
        canvas.drawPath(inner, paint)
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, open: Float) {
        if (open <= 0.05f) {
            // 闭眼：一条弧线
            paint.color = eyeColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.6f
            canvas.drawArc(RectF(x - 4f, y - 1f, x + 4f, y + 3f), 0f, 180f, false, paint)
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 1f
            return
        }
        // 眼睛
        paint.color = Color.WHITE
        canvas.drawOval(RectF(x - 4.5f, y - 5f, x + 4.5f, y + 5f), paint)
        // 瞳孔（跟随视线轻微移动）
        val lookX = sin(time * 1.3f) * 1.2f
        paint.color = eyeColor
        canvas.drawOval(RectF(x - 2.5f + lookX, y - 3.5f, x + 2.5f + lookX, y + 3.5f), paint)
        // 高光
        paint.color = Color.WHITE
        canvas.drawCircle(x - 1.5f + lookX, y - 2f, 1.1f, paint)
    }

    override fun setPosition(x: Float, y: Float) {
        posX = x.coerceIn(0.05f, 0.95f)
        posY = y.coerceIn(0.05f, 0.95f)
    }

    override fun setScale(scale: Float) {
        this.scale = scale.coerceIn(0.3f, 3f)
    }

    override fun onTouch(pointers: Int) {
        touchBoost = 1f
    }

    override fun setTargetFps(fps: Int) {
        targetFps = fps.coerceIn(1, 60)
    }

    override fun getBounds(): RectF {
        val w = 48f * scale * viewH / 100f
        val h = 70f * scale * viewH / 100f
        return RectF(posX * viewW - w / 2f, posY * viewH - h / 2f, posX * viewW + w / 2f, posY * viewH + h / 2f)
    }

    override fun loadModel(path: String?): Boolean = true

    fun getTargetFps(): Int = targetFps

    /** 是否应渲染新帧（按目标帧率节流） */
    fun shouldRenderFrame(now: Long): Boolean {
        val interval = 1000L / targetFps
        if (now - lastFrameTime >= interval) {
            lastFrameTime = now
            return true
        }
        return false
    }
}
