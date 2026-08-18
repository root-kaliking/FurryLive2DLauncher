package com.furry.live2dlauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.furry.live2dlauncher.R
import com.furry.live2dlauncher.audio.SoundManager
import com.furry.live2dlauncher.core.HomeItem
import com.furry.live2dlauncher.core.LauncherManager
import com.furry.live2dlauncher.core.PerformanceManager
import com.furry.live2dlauncher.core.Prefs
import com.furry.live2dlauncher.core.ThemeManager
import com.furry.live2dlauncher.databinding.ActivityLauncherBinding
import com.furry.live2dlauncher.gesture.GestureController
import com.furry.live2dlauncher.lockscreen.LockScreenService
import com.furry.live2dlauncher.wallpaper.WallpaperEngine

/**
 * 主桌面 Activity（系统 HOME）。
 *
 * 整合：壁纸引擎、桌面分页、Live2D 覆盖层、Dock、应用抽屉、
 * 页面指示器、手势交互、沉浸式全屏与锁屏联动。
 */
class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var wallpaperEngine: WallpaperEngine
    private lateinit var pagerAdapter: HomePagerAdapter
    private lateinit var drawerFragment: AppDrawerFragment
    private var drawerOpen = false
    private var currentPage = 0

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val performanceManager by lazy { PerformanceManager(this) }

    private val fpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                LockScreenService.ACTION_LOW_FPS,
                PerformanceManager.ACTION_BATTERY_CHANGED -> {
                    // 息屏或低电量：Live2D 与壁纸降帧
                    binding.live2dOverlay.reloadConfig()
                }
                LockScreenService.ACTION_NORMAL_FPS -> {
                    binding.live2dOverlay.reloadConfig()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyMode(Prefs.loadConfig().themeMode)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 沉浸式全屏：隐藏状态栏与导航栏
        ThemeManager.applyImmersive(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // 壁纸引擎
        wallpaperEngine = WallpaperEngine(binding.wallpaperContainer)

        // 桌面分页
        val cfg = Prefs.loadConfig()
        pagerAdapter = HomePagerAdapter(
            cfg.pageCount,
            onAppClick = { launchApp(it) },
            onFolderClick = { openFolder(it) },
            onItemLongPress = { onIconLongPress(it) }
        )
        binding.homePager.adapter = pagerAdapter
        binding.homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                applyPageConfig(position)
                updateIndicator(position)
                SoundManager.playPage()
            }
        })

        // 应用抽屉
        drawerFragment = AppDrawerFragment().apply {
            onAppSelected = { item ->
                closeDrawer()
                item.launchIntent?.let { startActivity(it) }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.drawer_container, drawerFragment)
            .commit()

        // Dock：设置入口 + 应用抽屉入口
        setupDock()

        // 手势：上滑打开抽屉
        val gesture = GestureController(
            this,
            onSwipeUp = { openDrawer() },
            onLongPress = { /* 编辑模式预留 */ },
            onSingleTap = { if (drawerOpen) closeDrawer() }
        )
        gesture.attach(binding.launcherRoot)

        // 页面指示器
        buildIndicator(cfg.pageCount)

        // 初始应用当前分页配置
        applyPageConfig(0)
        updateIndicator(0)

        // 请求存储权限（导入素材）
        requestPermissionsIfNeeded()

        // 注册锁屏低帧率广播
        registerReceiver(fpsReceiver, IntentFilter().apply {
            addAction(LockScreenService.ACTION_LOW_FPS)
            addAction(LockScreenService.ACTION_NORMAL_FPS)
            addAction(PerformanceManager.ACTION_BATTERY_CHANGED)
        })

        // 注册电量监听（低电量自动省电）
        performanceManager.register()
        performanceManager.onBatteryStateChanged = { low ->
            binding.live2dOverlay.reloadConfig()
        }
    }

    private fun applyPageConfig(page: Int) {
        wallpaperEngine.applyPage(page)
        // 每页独立模型配置（预留：模型路径按页切换）
        binding.live2dOverlay.reloadConfig()
    }

    private fun setupDock() {
        binding.dock.removeAllViews()
        // 设置按钮
        binding.dock.addView(makeDockButton(R.drawable.ic_settings, "设置") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })
        // 应用抽屉按钮
        binding.dock.addView(makeDockButton(R.drawable.ic_search, "应用") {
            openDrawer()
        })
        // 壁纸设置
        binding.dock.addView(makeDockButton(R.drawable.ic_wallpaper, "壁纸") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })
    }

    private fun makeDockButton(iconRes: Int, label: String, onClick: () -> Unit): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(14), dp(6), dp(14), dp(6))
            isClickable = true
            setOnClickListener { onClick() }
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
        }
        val text = TextView(this).apply {
            text = label
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 10f
        }
        item.addView(icon)
        item.addView(text)
        return item
    }

    private fun buildIndicator(count: Int) {
        binding.pageIndicator.removeAllViews()
        for (i in 0 until count) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(dp(7), dp(7))
            lp.setMargins(dp(4), 0, dp(4), 0)
            dot.layoutParams = lp
            dot.background = ContextCompat.getDrawable(this, R.drawable.bg_drawer_handle)
            binding.pageIndicator.addView(dot)
        }
    }

    private fun updateIndicator(position: Int) {
        for (i in 0 until binding.pageIndicator.childCount) {
            val dot = binding.pageIndicator.getChildAt(i)
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            lp.width = if (i == position) dp(18) else dp(7)
            dot.layoutParams = lp
        }
    }

    private fun openDrawer() {
        if (drawerOpen) return
        drawerOpen = true
        binding.drawerContainer.visibility = View.VISIBLE
        binding.drawerContainer.alpha = 0f
        binding.drawerContainer.animate().alpha(1f).setDuration(200).start()
    }

    private fun closeDrawer() {
        if (!drawerOpen) return
        drawerOpen = false
        binding.drawerContainer.animate().alpha(0f).setDuration(150)
            .withEndAction { binding.drawerContainer.visibility = View.GONE }
            .start()
    }

    private fun launchApp(packageName: String) {
        SoundManager.playIcon()
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            // 应用可能已被卸载
        }
    }

    private fun openFolder(folder: HomeItem.Folder) {
        // 文件夹弹窗：展示内部应用
        lateinit var popup: android.widget.PopupWindow
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE2A211C.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            folder.appPackages.forEach { pkg ->
                val row = LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, dp(8))
                    isClickable = true
                    setOnClickListener {
                        popup.dismiss()
                        launchApp(pkg)
                    }
                }
                val icon = ImageView(this@LauncherActivity).apply {
                    setImageDrawable(packageManager.getApplicationIcon(pkg))
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                }
                val name = TextView(this@LauncherActivity).apply {
                    text = packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    )
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 14f
                    setPadding(dp(12), 0, 0, 0)
                }
                row.addView(icon)
                row.addView(name)
                this@apply.addView(row)
            }
        }
        popup = android.widget.PopupWindow(
            content,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.showAtLocation(binding.launcherRoot, android.view.Gravity.CENTER, 0, 0)
    }

    private fun onIconLongPress(item: HomeItem) {
        // 长按：提供删除/移动操作（简化：长按弹出删除确认）
        android.app.AlertDialog.Builder(this)
            .setTitle("编辑桌面项")
            .setMessage("是否从桌面移除？")
            .setPositiveButton("移除") { _, _ ->
                when (item) {
                    is HomeItem.AppIcon -> LauncherManager(this).removeApp(currentPage, item.packageName)
                    is HomeItem.Folder -> LauncherManager(this).deleteFolder(currentPage, item.id)
                }
                pagerAdapter.refreshPage(currentPage)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun requestPermissionsIfNeeded() {
        val missing = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) missing.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED
            ) missing.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) missing.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyImmersive(this)
        // 从设置页返回后刷新
        pagerAdapter.refreshAll()
        wallpaperEngine.reloadConfig()
        binding.live2dOverlay.reloadConfig()
    }

    override fun onDestroy() {
        unregisterReceiver(fpsReceiver)
        performanceManager.unregister()
        wallpaperEngine.release()
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
