package com.furry.live2dlauncher.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sin
import kotlin.random.Random

/**
 * 辅助动效系统：
 *  - 漂浮粒子：缓慢上升飘动的光点（可开关）
 *  - 镜头缓动光点：跟随触摸位置缓动的大光斑（可开关）
 *
 * 对应 PRD"自带漂浮粒子、镜头缓动光点等辅助动效，用户可自由开关特效"。
 */
class ParticleSystem {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var enabled = true
    private var lightDotsEnabled = true
    private var w = 0
    private var h = 0

    // 镜头缓动光点
    private var lightX = 0f
    private var lightY = 0f
    private var targetX = 0f
    private var targetY = 0f
    private var hasLight = false

    private data class Particle(
        var x: Float, var y: Float,
        var speed: Float, var drift: Float, var phase: Float,
        var size: Float, var alpha: Int, var color: Int
    )

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) particles.clear()
    }

    fun setLightDotsEnabled(enabled: Boolean) {
        this.lightDotsEnabled = enabled
        if (!enabled) hasLight = false
    }

    /** 更新粒子（dt 秒） */
    fun update(dt: Float, width: Int, height: Int) {
        w = width; h = height
        if (!enabled) return
        // 补充粒子
        while (particles.size < 40) {
            particles.add(Particle(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                speed = 8f + Random.nextFloat() * 18f,
                drift = 4f + Random.nextFloat() * 10f,
                phase = Random.nextFloat() * 6.28f,
                size = 1.5f + Random.nextFloat() * 3f,
                alpha = 40 + Random.nextInt(120),
                color = if (Random.nextBoolean()) Color.rgb(255, 220, 180) else Color.rgb(180, 230, 255)
            ))
        }
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.phase += dt * 1.5f
            p.y -= p.speed * dt
            p.x += sin(p.phase) * p.drift * dt
            if (p.y < -10f) {
                p.y = height + 10f
                p.x = Random.nextFloat() * width
            }
        }
    }

    /** 设置镜头缓动目标（触摸位置） */
    fun setLightTarget(x: Float, y: Float) {
        targetX = x; targetY = y
        if (!hasLight) { lightX = x; lightY = y; hasLight = true }
    }

    fun updateLight(dt: Float) {
        if (!lightDotsEnabled) return
        lightX += (targetX - lightX) * dt * 1.8f
        lightY += (targetY - lightY) * dt * 1.8f
    }

    fun draw(canvas: Canvas) {
        if (enabled) {
            for (p in particles) {
                paint.color = p.color
                paint.alpha = p.alpha
                canvas.drawCircle(p.x, p.y, p.size, paint)
            }
        }
        if (lightDotsEnabled && hasLight) {
            paint.color = Color.argb(36, 255, 220, 160)
            canvas.drawCircle(lightX, lightY, 90f, paint)
            paint.color = Color.argb(22, 255, 220, 160)
            canvas.drawCircle(lightX, lightY, 150f, paint)
        }
        paint.alpha = 255
    }
}
