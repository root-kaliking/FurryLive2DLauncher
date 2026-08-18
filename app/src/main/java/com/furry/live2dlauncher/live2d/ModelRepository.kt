package com.furry.live2dlauncher.live2d

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Live2D 模型仓库：导入与管理本地模型工程文件。
 *
 * 支持导入：
 *  - 标准 Live2D 工程压缩包（.zip，内含 .model3.json / .model.json 及贴图）
 *  - 单个 .model3.json / .model.json 工程文件
 *
 * 导入后解压/复制到应用私有目录，避免依赖外部文件被移动导致模型丢失。
 */
class ModelRepository(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    /** 已导入的模型列表 */
    fun listModels(): List<File> =
        modelsDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** 导入模型：返回模型入口文件路径，失败返回 null */
    suspend fun importModel(uri: Uri, displayName: String?): String? = withContext(Dispatchers.IO) {
        try {
            val name = displayName?.substringBeforeLast('.') ?: "model_${System.currentTimeMillis()}"
            val safeName = name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
            val targetDir = File(modelsDir, safeName).apply { mkdirs() }

            val entryFile = if (displayName?.endsWith(".zip", true) == true) {
                // 解压 zip 工程
                unzipTo(uri, targetDir)
            } else {
                // 复制单个工程文件
                copyTo(uri, targetDir, displayName)
            }

            if (entryFile == null) {
                targetDir.deleteRecursively()
                null
            } else {
                entryFile.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun unzipTo(uri: Uri, targetDir: File): File? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        var entryFile: File? = null
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = File(targetDir, entry.name)
                    out.parentFile?.mkdirs()
                    out.outputStream().use { os -> zis.copyTo(os) }
                    if (entry.name.endsWith(".model3.json") || entry.name.endsWith(".model.json")) {
                        entryFile = out
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return entryFile
    }

    private fun copyTo(uri: Uri, targetDir: File, displayName: String?): File? {
        val fileName = displayName ?: "model.json"
        val out = File(targetDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { os -> input.copyTo(os) }
        }
        return if (out.exists() && out.length() > 0) out else null
    }

    /** 删除模型 */
    fun deleteModel(path: String) {
        val f = File(path)
        f.parentFile?.deleteRecursively()
    }
}
