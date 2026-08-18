package com.furry.live2dlauncher.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.furry.live2dlauncher.core.Prefs
import com.furry.live2dlauncher.core.SceneType
import kotlin.math.sin
import kotlin.random.Random

/**
 * 内置场景壁纸渲染器。
 * 支持五套默认场景：纯色、星空、卧室、森林、赛博街道。
 * 场景为程序化绘制（零素材依赖），叠加粒子特效与镜头缓动光点。
 */
class SceneWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = ParticleSystem()
    private var sceneType = SceneType.STARFIELD
    private var time = 0f
    private var targetFps = 24
    private var lastFrame = 0L
    private val stars = mutableListOf<Triple<Float, Float, Float>>() // x, y, twinklePhase

    init {
        val cfg = Prefs.loadConfig()
        targetFps = cfg.wallpaperFps
        particles.setEnabled(cfg.particlesEnabled)
        particles.setLightDotsEnabled(cfg.lightDotsEnabled)
        // 生成星空
        repeat(90) {
            stars.add(Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 6.28f))
        }
    }

    fun setScene(type: Int) {
        sceneType = type
        invalidate()
    }

    fun reloadConfig() {
        val cfg = Prefs.loadConfig()
        targetFps = cfg.wallpaperFps
        particles.setEnabled(cfg.particlesEnabled)
        particles.setLightDotsEnabled(cfg.lightDotsEnabled)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        if (now - lastFrame < 1000L / targetFps) return
        lastFrame = now
        time += 0.016f

        when (sceneType) {
            SceneType.PURE -> drawPure(canvas)
            SceneType.STARFIELD -> drawStarfield(canvas)
            SceneType.BEDROOM -> drawBedroom(canvas)
            SceneType.FOREST -> drawForest(canvas)
            SceneType.CYBER -> drawCyber(canvas)
            else -> drawStarfield(canvas)
        }
        particles.update(0.016f, width, height)
        particles.updateLight(0.016f)
        particles.draw(canvas)
        postInvalidateOnAnimation()
    }

    private fun drawPure(canvas: Canvas) {
        paint.shader = null
        paint.color = Color.rgb(38, 30, 26)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawStarfield(canvas: Canvas) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.rgb(10, 14, 34), Color.rgb(30, 24, 60), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        // 星星闪烁
        for ((sx, sy, ph) in stars) {
            val tw = (sin(time * 2f + ph) + 1f) / 2f
            val alpha = (60 + tw * 150).toInt()
            paint.color = Color.argb(alpha, 255, 255, 240)
            val r = 1f + tw * 1.5f
            canvas.drawCircle(sx * width, sy * height, r, paint)
        }
        // 银河带
        paint.color = Color.argb(28, 160, 180, 255)
        canvas.drawOval(
            android.graphics.RectF(-width * 0.2f, height * 0.55f, width * 1.2f, height * 0.75f),
            paint
        )
    }

    private fun drawBedroom(canvas: Canvas) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.rgb(60, 44, 60), Color.rgb(120, 84, 90), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        // 暖光台灯
        paint.color = Color.argb(50, 255, 200, 140)
        canvas.drawCircle(width * 0.78f, height * 0.32f, width * 0.3f, paint)
        // 床剪影
        paint.color = Color.argb(120, 40, 28, 40)
        canvas.drawRoundRect(
            width * 0.08f, height * 0.62f, width * 0.92f, height * 0.9f,
            24f, 24f, paint
        )
        // 枕头
        paint.color = Color.argb(150, 200, 170, 170)
        canvas.drawRoundRect(
            width * 0.12f, height * 0.55f, width * 0.42f, height * 0.66f,
            20f, 20f, paint
        )
        // 窗户月光
        paint.color = Color.argb(60, 220, 230, 255)
        canvas.drawRect(width * 0.12f, height * 0.08f, width * 0.4f, height * 0.4f, paint)
        paint.color = Color.argb(80, 30, 20, 30)
        canvas.drawRect(width * 0.26f, height * 0.08f, width * 0.28f, height * 0.4f, paint)
        canvas.drawRect(width * 0.12f, height * 0.23f, width * 0.4f, height * 0.25f, paint)
    }

    private fun drawForest(canvas: Canvas) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.rgb(12, 34, 26), Color.rgb(30, 60, 40), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        // 树影（多层）
        drawTreeLayer(canvas, 0.30f, Color.argb(90, 20, 50, 32), 0.9f)
        drawTreeLayer(canvas, 0.55f, Color.argb(120, 16, 44, 28), 1.1f)
        // 萤火虫
        for ((sx, sy, ph) in stars.take(20)) {
            val tw = (sin(time * 3f + ph) + 1f) / 2f
            paint.color = Color.argb((40 + tw * 160).toInt(), 200, 255, 150)
            canvas.drawCircle(sx * width, sy * height * 0.7f, 1.5f + tw * 2f, paint)
        }
    }

    private fun drawTreeLayer(canvas: Canvas, baseY: Float, color: Int, scale: Float) {
        paint.color = color
        val count = 5
        for (i in 0 until count) {
            val x = width * (i + 0.5f) / count
            val h = height * 0.5f * scale
            val path = Path()
            path.moveTo(x, height * baseY + h)
            path.lineTo(x - width * 0.09f, height * baseY + h * 0.5f)
            path.lineTo(x - width * 0.05f, height * baseY + h * 0.5f)
            path.lineTo(x - width * 0.11f, height * baseY + h * 0.2f)
            path.lineTo(x, height * baseY)
            path.lineTo(x + width * 0.11f, height * baseY + h * 0.2f)
            path.lineTo(x + width * 0.05f, height * baseY + h * 0.5f)
            path.lineTo(x + width * 0.09f, height * baseY + h * 0.5f)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private fun drawCyber(canvas: Canvas) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.rgb(16, 12, 40), Color.rgb(50, 20, 60), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        // 霓虹地平线
        paint.color = Color.argb(120, 255, 60, 200)
        canvas.drawRect(0f, height * 0.7f, width.toFloat(), height * 0.72f, paint)
        paint.color = Color.argb(120, 60, 200, 255)
        canvas.drawRect(0f, height * 0.72f, width.toFloat(), height * 0.735f, paint)
        // 建筑剪影
        paint.color = Color.argb(160, 12, 8, 30)
        val seed = Random(7)
        var bx = 0f
        while (bx < width) {
            val bw = width * (0.06f + seed.nextFloat() * 0.08f)
            val bh = height * (0.1f + seed.nextFloat() * 0.25f)
            canvas.drawRect(bx, height * 0.72f - bh, bx + bw, height * 0.72f, paint)
            // 窗户光点
            paint.color = Color.argb(150, 255, 200, 120)
            val rows = (bh / (height * 0.03f)).toInt()
            for (r in 0 until rows) {
                if (seed.nextFloat() > 0.5f) {
                    canvas.drawRect(
                        bx + bw * 0.15f, height * 0.72f - bh + r * height * 0.03f + height * 0.01f,
                        bx + bw * 0.4f, height * 0.72f - bh + r * height * 0.03f + height * 0.02f,
                        paint
                    )
                }
            }
            paint.color = Color.argb(160, 12, 8, 30)
            bx += bw + width * 0.01f
        }
        // 扫描线
        paint.color = Color.argb(40, 255, 80, 220)
        canvas.drawRect(0f, (time * 120f) % height, width.toFloat(), (time * 120f) % height + 2f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                particles.setLightTarget(event.x, event.y)
            }
        }
        return true
    }
}
