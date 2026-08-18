package com.furry.live2dlauncher.lockscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import com.furry.live2dlauncher.R
import com.furry.live2dlauncher.core.Prefs
import com.furry.live2dlauncher.ui.LauncherActivity

/**
 * 锁屏联动服务。
 *
 * 监听屏幕开关广播：
 *  - 屏幕熄灭（息屏）：通知桌面引擎进入低帧率模式，兼顾展示效果与功耗
 *  - 屏幕点亮（亮屏）：恢复全帧率
 *
 * 对应 PRD"息屏状态下支持低帧率动画运行，兼顾展示效果与设备功耗"。
 */
class LockScreenService : Service() {

    companion object {
        const val CHANNEL_ID = "lockscreen_channel"
        const val ACTION_LOW_FPS = "com.furry.live2dlauncher.action.LOW_FPS"
        const val ACTION_NORMAL_FPS = "com.furry.live2dlauncher.action.NORMAL_FPS"
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // 息屏：低帧率
                    val cfg = Prefs.loadConfig()
                    val lowFps = (cfg.live2dFps / 3).coerceAtLeast(5)
                    sendBroadcast(Intent(ACTION_LOW_FPS).setPackage(context.packageName))
                }
                Intent.ACTION_SCREEN_ON -> {
                    sendBroadcast(Intent(ACTION_NORMAL_FPS).setPackage(context.packageName))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1, buildNotification())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "锁屏联动", NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, LauncherActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Furry Live2D 桌面")
            .setContentText("锁屏联动已开启，息屏自动降低帧率")
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
