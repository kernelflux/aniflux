package com.kernelflux.aniflux.downloader

import android.content.Context
import com.kernelflux.aniflux.AntiFlux
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体下载器实现
 * 使用OkHttp进行网络下载
 */
class MediaDownloaderImpl(
    private val context: Context,
    private val maxConcurrentDownloads: Int = 3
) : MediaDownloader {

    // OkHttp客户端
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // 并发控制
    private val downloadSemaphore = Semaphore(maxConcurrentDownloads)

    // 下载任务管理
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val downloadProgress = ConcurrentHashMap<String, AtomicLong>()
    private val downloadCallbacks = ConcurrentHashMap<String, DownloadProgressCallback>()

    // 临时文件目录
    private val tempDir = File(context.cacheDir, "downloads")

    init {
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
    }

    override suspend fun download(url: String, targetFile: File): DownloadResult {
        return withContext(Dispatchers.IO) {
            downloadSemaphore.withPermit {
                try {
                    // 检查是否已经在下载
                    if (downloadJobs.containsKey(url)) {
                        return@withPermit DownloadResult.Error(IllegalStateException("Download already in progress"))
                    }

                    // 创建下载任务
                    val job = CoroutineScope(Dispatchers.IO).async {
                        performDownload(url, targetFile)
                    }

                    downloadJobs[url] = job
                    val result = job.await()
                    downloadJobs.remove(url)
                    downloadProgress.remove(url)
                    downloadCallbacks.remove(url)

                    result
                } catch (e: Exception) {
                    downloadJobs.remove(url)
                    downloadProgress.remove(url)
                    downloadCallbacks.remove(url)
                    DownloadResult.Error(e)
                }
            }
        }
    }

    override fun cancel(url: String) {
        downloadJobs[url]?.cancel()
        downloadJobs.remove(url)
        downloadProgress.remove(url)
        downloadCallbacks.remove(url)
    }

    override fun getProgress(url: String): Float {
        val progress = downloadProgress[url]?.get() ?: 0L
        return if (progress > 0) {
            progress.toFloat() / 100f
        } else {
            0f
        }
    }

    /**
     * 执行下载
     */
    private suspend fun performDownload(url: String, targetFile: File): DownloadResult {
        return try {
            // 创建临时文件
            val tempFile = File(tempDir, generateTempFileName(url))

            // 构建请求
            val request = Request.Builder()
                .url(url)
                .build()

            // 执行请求
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return DownloadResult.Error(IOException("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body
            val contentLength = body.contentLength()

            // 下载到临时文件
            body.byteStream().use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 更新进度
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            downloadProgress[url]?.set(progress.toLong())
                            downloadCallbacks[url]?.onProgress(url, progress / 100f)
                        }
                    }
                }
            }

            // 原子性地移动到目标文件
            if (tempFile.renameTo(targetFile)) {
                DownloadResult.Success(targetFile)
            } else {
                // 如果重命名失败，尝试复制
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
                DownloadResult.Success(targetFile)
            }

        } catch (e: Exception) {
            DownloadResult.Error(e)
        }
    }

    /**
     * 生成临时文件名
     */
    private fun generateTempFileName(url: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(url.toByteArray())
        return "temp_${hash.joinToString("") { "%02x".format(it) }}"
    }

    /**
     * 设置下载进度回调
     */
    fun setProgressCallback(url: String, callback: DownloadProgressCallback) {
        downloadCallbacks[url] = callback
    }

    /**
     * 清理临时文件
     */
    fun cleanupTempFiles() {
        try {
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("temp_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 忽略清理失败
        }
    }
}