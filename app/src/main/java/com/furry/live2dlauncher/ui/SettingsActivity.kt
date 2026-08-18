package com.furry.live2dlauncher.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.furry.live2dlauncher.R
import com.furry.live2dlauncher.core.BackupManager
import com.furry.live2dlauncher.core.Prefs
import com.furry.live2dlauncher.core.ThemeManager
import com.furry.live2dlauncher.databinding.ActivitySettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置页：所有配置操作均在此完成。
 * 覆盖外观、桌面布局、Live2D、壁纸、手势、音效、性能与备份还原。
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyMode(Prefs.loadConfig().themeMode)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }
}

class SettingsFragment : PreferenceFragmentCompat() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val backupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch {
                    val file = BackupManager(requireContext()).backup()
                    if (file != null) {
                        requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(file.readBytes())
                        }
                        toast(getString(R.string.backup_success))
                    } else {
                        toast(getString(R.string.backup_fail))
                    }
                }
            }
        }

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val ok = BackupManager(requireContext()).restore(uri)
                    toast(if (ok) getString(R.string.restore_success) else getString(R.string.restore_fail))
                    if (ok) {
                        ThemeManager.applyMode(Prefs.loadConfig().themeMode)
                        requireActivity().recreate()
                    }
                }
            }
        }

    private val importModelLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val name = uri.lastPathSegment?.substringAfterLast('/')
                    val path = com.furry.live2dlauncher.live2d.ModelRepository(requireContext())
                        .importModel(uri, name)
                    if (path != null) {
                        Prefs.setLastModelPath(path)
                        toast(getString(R.string.model_import_success))
                    } else {
                        toast(getString(R.string.model_import_fail))
                    }
                }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_prefs, rootKey)
        setupListeners()
    }

    private fun setupListeners() {
        // 主题模式
        findPreference<ListPreference>("theme_mode")?.setOnPreferenceChangeListener { _, newValue ->
            val mode = (newValue as String).toInt()
            val cfg = Prefs.loadConfig()
            cfg.themeMode = mode
            Prefs.saveConfig(cfg)
            ThemeManager.applyMode(mode)
            requireActivity().recreate()
            true
        }

        // 桌面列数
        findPreference<ListPreference>("columns")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.columns = (v as String).toInt()
            Prefs.saveConfig(cfg)
            true
        }
        // 桌面行数
        findPreference<ListPreference>("rows")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.rows = (v as String).toInt()
            Prefs.saveConfig(cfg)
            true
        }
        // 分页数
        findPreference<ListPreference>("page_count")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            val newCount = (v as String).toInt()
            val oldCount = cfg.pageCount
            cfg.pageCount = newCount
            // 扩展/收缩分页配置
            val pages = cfg.pages.toMutableList()
            if (newCount > oldCount) {
                for (i in oldCount until newCount) {
                    pages.add(com.furry.live2dlauncher.core.PageConfig(pageIndex = i))
                }
            } else if (newCount < oldCount) {
                while (pages.size > newCount) pages.removeAt(pages.size - 1)
            }
            cfg.pages = pages
            Prefs.saveConfig(cfg)
            true
        }

        // Live2D 帧率
        findPreference<ListPreference>("live2d_fps")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.live2dFps = (v as String).toInt()
            Prefs.saveConfig(cfg)
            true
        }
        // 壁纸帧率
        findPreference<ListPreference>("wallpaper_fps")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.wallpaperFps = (v as String).toInt()
            Prefs.saveConfig(cfg)
            true
        }

        // 低电量省电
        findPreference<SwitchPreferenceCompat>("battery_saver")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.batterySaver = v as Boolean
            Prefs.saveConfig(cfg)
            true
        }
        // 粒子特效
        findPreference<SwitchPreferenceCompat>("particles")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.particlesEnabled = v as Boolean
            Prefs.saveConfig(cfg)
            true
        }
        // 镜头光点
        findPreference<SwitchPreferenceCompat>("light_dots")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.lightDotsEnabled = v as Boolean
            Prefs.saveConfig(cfg)
            true
        }

        // 音效
        findPreference<SwitchPreferenceCompat>("sound_enable")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.soundEnabled = v as Boolean
            Prefs.saveConfig(cfg)
            true
        }
        findPreference<SwitchPreferenceCompat>("sound_touch")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.soundTouch = v as Boolean
            Prefs.saveConfig(cfg)
            true
        }
        findPreference<SwitchPreferenceCompat>("sound_page")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.soundPage = v as Boolean
            Prefs.saveConfig(cfg)
            true
        }
        findPreference<SwitchPreferenceCompat>("sound_icon")?.setOnPreferenceChangeListener { _, v ->
            val cfg = Prefs.loadConfig()
            cfg.soundIcon = v as Boolean
            Prefs.saveConfig(cfg)
            true
        }

        // 导入模型
        findPreference<Preference>("import_model")?.setOnPreferenceClickListener {
            importModelLauncher.launch(arrayOf(
                "application/zip",
                "application/octet-stream",
                "application/json"
            ))
            true
        }

        // GitHub 素材仓库
        findPreference<Preference>("github_repo")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/search?q=live2d+model&type=repositories"))
            startActivity(intent)
            true
        }

        // 备份
        findPreference<Preference>("backup")?.setOnPreferenceClickListener {
            backupLauncher.launch("furry_backup.json")
            true
        }
        // 还原
        findPreference<Preference>("restore")?.setOnPreferenceClickListener {
            restoreLauncher.launch(arrayOf("application/json"))
            true
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
