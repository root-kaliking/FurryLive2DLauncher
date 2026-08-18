package com.furry.live2dlauncher.wallpaper

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * 视频壁纸视图：使用 ExoPlayer 播放本地短视频作为桌面动态背景。
 * 循环播放、静音、无控件，适配横竖屏。
 */
class VideoWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var player: ExoPlayer? = null

    fun loadVideo(uri: Uri?) {
        if (uri == null) return
        release()
        val exo = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
        player = exo
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        player?.playWhenReady = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player?.playWhenReady = false
    }

    fun release() {
        player?.release()
        player = null
    }
}
