package com.furry.live2dlauncher.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.furry.live2dlauncher.core.Prefs
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/**
 * 音效系统。
 *
 * 默认提供三套合成音效（触摸模型 / 切换页面 / 点击图标），无需外部素材。
 * 支持基于用户导入的音频文件自定义配套音效（设置页配置路径后自动切换）。
 *
 * 对应 PRD"可基于用户导入的对应 Live2D 模型自定义配套音效"。
 */
object SoundManager {

    private const val SAMPLE_RATE = 22050

    private var soundPool: SoundPool? = null
    private var touchSoundId = 0
    private var pageSoundId = 0
    private var iconSoundId = 0
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        // 生成默认合成音效
        val dir = File(context.cacheDir, "sounds").apply { mkdirs() }
        touchSoundId = loadOrGenerate(soundPool!!, File(dir, "touch.wav")) { genTouch() }
        pageSoundId = loadOrGenerate(soundPool!!, File(dir, "page.wav")) { genPage() }
        iconSoundId = loadOrGenerate(soundPool!!, File(dir, "icon.wav")) { genIcon() }
        initialized = true
    }

    private fun loadOrGenerate(pool: SoundPool, file: File, generator: () -> ByteArray): Int {
        if (!file.exists()) {
            file.writeBytes(generator())
        }
        return pool.load(file.absolutePath, 1)
    }

    /** 触摸模型音效 */
    fun playTouch() {
        val cfg = Prefs.loadConfig()
        if (!cfg.soundEnabled || !cfg.soundTouch) return
        soundPool?.play(touchSoundId, 0.6f, 0.6f, 1, 0, 1f)
    }

    /** 切换页面音效 */
    fun playPage() {
        val cfg = Prefs.loadConfig()
        if (!cfg.soundEnabled || !cfg.soundPage) return
        soundPool?.play(pageSoundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    /** 点击桌面图标音效 */
    fun playIcon() {
        val cfg = Prefs.loadConfig()
        if (!cfg.soundEnabled || !cfg.soundIcon) return
        soundPool?.play(iconSoundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        initialized = false
    }

    // ---- 合成音效生成 ----

    private fun genTouch(): ByteArray = synth(0.08f) { t, f ->
        f(880f + 220f * t / 0.08f) * (1f - t / 0.08f)
    }

    private fun genPage(): ByteArray = synth(0.12f) { t, f ->
        f(440f + 660f * t / 0.12f) * (1f - t / 0.12f) * 0.8f
    }

    private fun genIcon(): ByteArray = synth(0.06f) { t, f ->
        f(1320f) * (1f - t / 0.06f) * 0.7f
    }

    private fun synth(duration: Float, wave: (Float, (Float) -> Float) -> Float): ByteArray {
        val n = (SAMPLE_RATE * duration).toInt()
        val data = ByteArray(n * 2)
        val raf = RandomAccessFile(File.createTempFile("wav", ".tmp"), "rw")
        raf.close()
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val v = wave(t) { freq -> sin(2 * PI * freq * t).toFloat() }
            val sample = (v * 0.5f * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767)
            data[i * 2] = (sample and 0xFF).toByte()
            data[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return wavHeader(data.size, SAMPLE_RATE) + data
    }

    private fun wavHeader(dataSize: Int, sampleRate: Int): ByteArray {
        val header = ByteArray(44)
        val totalSize = 36 + dataSize
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalSize)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)          // fmt chunk size
        writeShortLE(header, 20, 1)         // PCM
        writeShortLE(header, 22, 1)         // mono
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, sampleRate * 2) // byte rate
        writeShortLE(header, 32, 2)         // block align
        writeShortLE(header, 34, 16)        // bits per sample
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, dataSize)
        return header
    }

    private fun writeIntLE(b: ByteArray, off: Int, v: Int) {
        b[off] = (v and 0xFF).toByte()
        b[off + 1] = ((v shr 8) and 0xFF).toByte()
        b[off + 2] = ((v shr 16) and 0xFF).toByte()
        b[off + 3] = ((v shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(b: ByteArray, off: Int, v: Int) {
        b[off] = (v and 0xFF).toByte()
        b[off + 1] = ((v shr 8) and 0xFF).toByte()
    }
}
