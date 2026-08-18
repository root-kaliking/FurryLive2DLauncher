package com.furry.live2dlauncher.wallpaper

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import coil.load
import coil.size.Scale

/**
 * 图片壁纸视图：加载本地图片作为桌面动态背景。
 * 使用 Coil 异步加载，居中裁剪填充全屏。
 */
class ImageWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ImageView(context, attrs) {

    init {
        scaleType = ScaleType.CENTER_CROP
    }

    fun loadImage(uri: Uri?) {
        if (uri == null) return
        load(uri) {
            scale(Scale.FILL)
            crossfade(true)
        }
    }
}
