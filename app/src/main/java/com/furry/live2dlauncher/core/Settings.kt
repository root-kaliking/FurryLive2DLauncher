package com.furry.live2dlauncher.core

import android.graphics.RectF
import org.json.JSONArray
import org.json.JSONObject

/**
 * 全局配置数据模型。
 * 对应 PRD 中"全量配置一键备份与还原"所需的全部可持久化字段。
 */
object WallpaperType {
    const val SCENE = 0
    const val IMAGE = 1
    const val VIDEO = 2
}

object SceneType {
    const val PURE = 0
    const val STARFIELD = 1
    const val BEDROOM = 2
    const val FOREST = 3
    const val CYBER = 4
}

object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

/** 单个桌面分页的独立配置：专属模型、壁纸、图标方案 */
data class PageConfig(
    val pageIndex: Int = 0,
    val modelPath: String? = null,
    val wallpaperType: Int = WallpaperType.SCENE,
    val sceneType: Int = SceneType.STARFIELD,
    val wallpaperPath: String? = null,
    val iconScheme: String = "default"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("pageIndex", pageIndex)
        put("modelPath", modelPath ?: JSONObject.NULL)
        put("wallpaperType", wallpaperType)
        put("sceneType", sceneType)
        put("wallpaperPath", wallpaperPath ?: JSONObject.NULL)
        put("iconScheme", iconScheme)
    }

    companion object {
        fun fromJson(o: JSONObject): PageConfig = PageConfig(
            pageIndex = o.optInt("pageIndex", 0),
            modelPath = o.optString("modelPath").takeIf { it.isNotEmpty() },
            wallpaperType = o.optInt("wallpaperType", WallpaperType.SCENE),
            sceneType = o.optInt("sceneType", SceneType.STARFIELD),
            wallpaperPath = o.optString("wallpaperPath").takeIf { it.isNotEmpty() },
            iconScheme = o.optString("iconScheme", "default")
        )
    }
}

/** 启动器全部配置 */
data class LauncherConfig(
    var columns: Int = 4,
    var rows: Int = 6,
    var pageCount: Int = 3,
    var pages: List<PageConfig> = List(3) { PageConfig(pageIndex = it) },
    var live2dFps: Int = 30,
    var wallpaperFps: Int = 24,
    var batterySaver: Boolean = true,
    var particlesEnabled: Boolean = true,
    var lightDotsEnabled: Boolean = true,
    var soundEnabled: Boolean = true,
    var soundTouch: Boolean = true,
    var soundPage: Boolean = true,
    var soundIcon: Boolean = true,
    var gestureBlockRect: RectF? = null,
    var themeMode: Int = ThemeMode.SYSTEM,
    var accentColor: Int = 0xFFE8612F.toInt(),
    var modelScale: Float = 1f,
    var modelX: Float = 0f,
    var modelY: Float = 0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("columns", columns)
        put("rows", rows)
        put("pageCount", pageCount)
        put("pages", JSONArray().apply { pages.forEach { put(it.toJson()) } })
        put("live2dFps", live2dFps)
        put("wallpaperFps", wallpaperFps)
        put("batterySaver", batterySaver)
        put("particlesEnabled", particlesEnabled)
        put("lightDotsEnabled", lightDotsEnabled)
        put("soundEnabled", soundEnabled)
        put("soundTouch", soundTouch)
        put("soundPage", soundPage)
        put("soundIcon", soundIcon)
        gestureBlockRect?.let {
            put("gestureBlock", JSONObject().apply {
                put("l", it.left); put("t", it.top); put("r", it.right); put("b", it.bottom)
            })
        }
        put("themeMode", themeMode)
        put("accentColor", accentColor)
        put("modelScale", modelScale)
        put("modelX", modelX)
        put("modelY", modelY)
    }

    companion object {
        fun fromJson(s: String): LauncherConfig {
            val o = JSONObject(s)
            val pages = o.optJSONArray("pages")?.let { arr ->
                (0 until arr.length()).map { PageConfig.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList()
            val cfg = LauncherConfig(
                columns = o.optInt("columns", 4),
                rows = o.optInt("rows", 6),
                pageCount = o.optInt("pageCount", 3),
                pages = pages,
                live2dFps = o.optInt("live2dFps", 30),
                wallpaperFps = o.optInt("wallpaperFps", 24),
                batterySaver = o.optBoolean("batterySaver", true),
                particlesEnabled = o.optBoolean("particlesEnabled", true),
                lightDotsEnabled = o.optBoolean("lightDotsEnabled", true),
                soundEnabled = o.optBoolean("soundEnabled", true),
                soundTouch = o.optBoolean("soundTouch", true),
                soundPage = o.optBoolean("soundPage", true),
                soundIcon = o.optBoolean("soundIcon", true),
                themeMode = o.optInt("themeMode", ThemeMode.SYSTEM),
                accentColor = o.optInt("accentColor", 0xFFE8612F.toInt()),
                modelScale = o.optDouble("modelScale", 1.0).toFloat(),
                modelX = o.optDouble("modelX", 0.0).toFloat(),
                modelY = o.optDouble("modelY", 0.0).toFloat()
            )
            o.optJSONObject("gestureBlock")?.let { g ->
                cfg.gestureBlockRect = RectF(
                    g.optDouble("l", 0.0).toFloat(),
                    g.optDouble("t", 0.0).toFloat(),
                    g.optDouble("r", 0.0).toFloat(),
                    g.optDouble("b", 0.0).toFloat()
                )
            }
            return cfg
        }
    }
}
