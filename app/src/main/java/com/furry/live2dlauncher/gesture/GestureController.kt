package com.furry.live2dlauncher.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

/**
 * 桌面级手势控制器。
 *
 * 负责桌面整体手势：
 *  - 上滑：打开应用抽屉
 *  - 长按：进入桌面编辑模式（预留）
 *  - 单击空白：收起抽屉
 *
 * 注：Live2D 模型的单指拖动/双指缩放/多点触控骨骼反馈由
 * [com.furry.live2dlauncher.live2d.Live2DOverlayView] 独立处理。
 */
class GestureController(
    context: Context,
    private val onSwipeUp: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onSingleTap: () -> Unit
) {

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onSingleTap()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongPress()
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            if (e1 != null && e2 != null) {
                val dy = e2.y - e1.y
                val dx = e2.x - e1.x
                if (Math.abs(dy) > Math.abs(dx) && dy < -120f && Math.abs(velocityY) > 600f) {
                    onSwipeUp()
                    return true
                }
            }
            return false
        }
    })

    fun attach(view: View) {
        view.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            true
        }
    }
}
