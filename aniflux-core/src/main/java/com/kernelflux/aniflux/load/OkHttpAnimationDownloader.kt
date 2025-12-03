package com.kernelflux.aniflux.load

import android.content.Context
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import java.io.FileOutputStream

/**
 * 基于OkHttp的动画资源下载器实现
 * 提供健壮的下载逻辑，支持超时、重试等机制
 */
class OkHttpAnimationDownloader(
    okHttpClient: OkHttpClient? = null
) : AnimationDownloader {

    private val client: OkHttpClient = okHttpClient ?: createDefaultClient()

    private fun createDefaultClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Throws(Exception::class)
    override fun download(context: Context, url: String): File {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "AniFlux/1.0")
            .build()


        // 准备临时文件目录（下载到临时目录，由调用者决定是否保存到磁盘缓存）
        val tempDirName = "aniflux_temp"
        val dir = File(context.cacheDir, tempDirName)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Failed to create temp dir: ${dir.absolutePath}")
        }

        // 解析 URL，决定文件名与扩展名
        val lastSeg = url.toUri().lastPathSegment ?: ""
        val hasExt = lastSeg.contains('.') && !lastSeg.endsWith(".")
        val ext = if (hasExt) {
            "." + lastSeg.substringAfterLast('.')
        } else {
            ".tmp"
        }
        val baseName = run {
            val raw = lastSeg.substringBeforeLast('.', missingDelimiterValue = "")
            raw.ifBlank { "download" }
        }
        val fileName = "$baseName-${System.currentTimeMillis()}$ext"
        val outFile = File(dir, fileName)
        // 执行下载并写入文件
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            val body = response.body
            // 将网络流写到文件（流式拷贝，避免一次性读入内存）
            try {
                FileOutputStream(outFile).use { fos ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE) // 8K
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                        fos.fd.sync() // 尽量落盘（可选）
                    }
                }
            } catch (e: Exception) {
                // 出错时清理半成品
                runCatching { outFile.delete() }
                throw IOException("Failed to save file from $url", e)
            }
        }
        return outFile
    }

    @Throws(Exception::class)
    override fun downloadToBytes(context: Context, url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "AniFlux/1.0")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            response.body.bytes()
        } catch (e: Exception) {
            throw IOException("Failed to download animation from $url", e)
        }
    }
}
