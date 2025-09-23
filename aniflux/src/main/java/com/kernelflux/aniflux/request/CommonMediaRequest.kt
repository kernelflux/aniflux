package com.kernelflux.aniflux.request

import com.kernelflux.aniflux.cache.CachedMedia
import com.kernelflux.aniflux.cache.MediaCache
import com.kernelflux.aniflux.config.LoadOptions
import com.kernelflux.aniflux.config.LoadResource
import com.kernelflux.aniflux.config.MediaError
import com.kernelflux.aniflux.config.MediaTarget
import com.kernelflux.aniflux.downloader.DownloadResult
import com.kernelflux.aniflux.downloader.MediaDownloader
import com.kernelflux.aniflux.loader.MediaLoaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 统一的媒体请求实现
 */
class CommonMediaRequest(
    private val source: LoadResource,
    private val options: LoadOptions,
    private val target: MediaTarget,
    private val mediaCache: MediaCache,
    private val mediaDownloader: MediaDownloader,
    private val requestSemaphore: Semaphore
) : MediaRequest {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var status: RequestStatus = RequestStatus.PENDING

    override fun begin() {
        if (status == RequestStatus.RUNNING) return
        status = RequestStatus.RUNNING
        job = scope.launch {
            try {
                // 使用并发控制
                requestSemaphore.withPermit {
                    performLoad()
                }
            } catch (e: Exception) {
                status = RequestStatus.CANCELLED
                withContext(Dispatchers.Main) {
                    target.onLoadFail(
                        MediaError.Companion.unknownError(
                            e.message ?: "Unknown error", e
                        )
                    )
                }
            }
        }
    }

    private suspend fun performLoad() = withContext(Dispatchers.IO) {
        // 1. 检查缓存
        val cacheKey = generateCacheKey(source, options)
        val cachedMedia = mediaCache.get(cacheKey)
        if (cachedMedia != null) {
            withContext(Dispatchers.Main) {
                target.onLoadSuccess(cachedMedia)
                status = RequestStatus.COMPLETE
            }
            return@withContext
        }


        // 2. 下载文件（如果是远程资源）
        val finalSource = when (source) {
            is LoadResource.UrlResource -> {
                withContext(Dispatchers.Main) {
                    target.onLoadStarted()
                }

                // 使用现有的MediaDownloader接口
                val targetFile = File(mediaCache.cacheDir, generateFileName(source.url))
                val downloadResult = mediaDownloader.download(source.url, targetFile)

                when (downloadResult) {
                    is DownloadResult.Success -> {
                        LoadResource.FileResource(downloadResult.file)
                    }

                    is DownloadResult.Error -> {
                        withContext(Dispatchers.Main) {
                            target.onLoadFail(
                                MediaError.unknownError(
                                    message = "Download failed: ${downloadResult.exception.message}",
                                    throwable = downloadResult.exception
                                )
                            )
                        }
                        return@withContext
                    }

                    is DownloadResult.Cancelled -> {
                        withContext(Dispatchers.Main) {
                            target.onLoadFail(
                                MediaError.unknownError(
                                    message = "Download cancelled: ${downloadResult.url}",
                                    throwable = null
                                )
                            )
                        }
                        return@withContext
                    }
                }
            }

            else -> source
        }


        // 3. 使用MediaLoaderFactory加载媒体
        val mediaData = MediaLoaderFactory.load(finalSource, options, target)

        // 4. 缓存结果
        if (mediaData != null) {
            val cachedMedia = createCachedMedia(mediaData, finalSource)
            mediaCache.put(cacheKey, cachedMedia)
        }

        // 5. 通知目标
        withContext(Dispatchers.Main) {
            if (mediaData == null) {
                target.onLoadFail(MediaError.Companion.unknownError(message = "MediaLoaderFactory load fail..."))
            } else {
                target.onLoadSuccess(mediaData)
            }
            status = RequestStatus.COMPLETE
        }
    }

    override fun pause() {
        if (status == RequestStatus.RUNNING) {
            status = RequestStatus.PAUSED
            job?.cancel()
        }
    }

    override fun resume() {
        if (status == RequestStatus.PAUSED) {
            begin()
        }
    }

    override fun clear() {
        job?.cancel()
        status = RequestStatus.CLEARED
    }

    override fun isRunning(): Boolean = status == RequestStatus.RUNNING
    override fun isComplete(): Boolean = status == RequestStatus.COMPLETE
    override fun isCleared(): Boolean = status == RequestStatus.CLEARED
    override fun isFailed(): Boolean = status == RequestStatus.CANCELLED
    override fun isPaused(): Boolean = status == RequestStatus.PAUSED

    // 添加状态获取方法
    fun getStatus(): RequestStatus = status

    // 生成缓存key
    private fun generateCacheKey(source: LoadResource, options: LoadOptions): String {
        return "${source.hashCode()}_${options.hashCode()}"
    }

    // 生成文件名
    private fun generateFileName(url: String): String {
        val hash = url.hashCode().toString()
        val extension = when {
            url.contains(".pag") -> ".pag"
            url.contains(".svga") -> ".svga"
            url.contains(".json") -> ".json"
            url.contains(".gif") -> ".gif"
            url.contains(".png") -> ".png"
            url.contains(".jpg") || url.contains(".jpeg") -> ".jpg"
            url.contains(".webp") -> ".webp"
            else -> ".tmp"
        }
        return "${hash}${extension}"
    }

    private fun createCachedMedia(mediaData: Any, source: LoadResource): CachedMedia {
        return when (mediaData) {
            is android.graphics.Bitmap -> {
                CachedMedia.ImageCache(
                    bitmap = mediaData,
                    contentType = getContentTypeFromBitmap(mediaData),
                    size = mediaData.byteCount.toLong()
                )
            }

            is com.opensource.svgaplayer.SVGAVideoEntity -> {
                CachedMedia.SVGACache(
                    sVGAEntity = mediaData,
                    contentType = "application/svga",
                    size = 0L // SVGA实体大小无法直接获取
                )
            }

            is org.libpag.PAGFile -> {
                CachedMedia.PAGCache(
                    pagFile = mediaData,
                    contentType = "application/pag",
                    size = 0L // PAG文件大小无法直接获取
                )
            }

            is com.airbnb.lottie.LottieComposition -> {
                CachedMedia.LottieCache(
                    lottieComposition = mediaData,
                    contentType = "application/json",
                    size = 0L // Lottie组合大小无法直接获取
                )
            }

            is File -> {
                CachedMedia.FileCache(
                    file = mediaData,
                    contentType = getContentType(mediaData),
                    size = mediaData.length()
                )
            }

            else -> {
                // 对于其他类型，尝试保存为文件
                if (source is LoadResource.FileResource) {
                    CachedMedia.FileCache(
                        file = source.file,
                        contentType = getContentType(source.file),
                        size = source.file.length()
                    )
                } else {
                    // 创建临时文件
                    val tempFile = File(mediaCache.cacheDir, "temp_${System.currentTimeMillis()}")
                    CachedMedia.FileCache(
                        file = tempFile,
                        contentType = "application/octet-stream",
                        size = 0L
                    )
                }
            }
        }
    }

    // 获取文件内容类型
    private fun getContentType(file: File): String {
        return when (file.extension.lowercase()) {
            "pag" -> "application/pag"
            "svga" -> "application/svga"
            "json" -> "application/json"
            "gif" -> "image/gif"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    // 从Bitmap获取内容类型
    private fun getContentTypeFromBitmap(bitmap: android.graphics.Bitmap): String {
        return when (bitmap.config) {
            android.graphics.Bitmap.Config.ARGB_8888 -> "image/png"
            android.graphics.Bitmap.Config.RGB_565 -> "image/jpeg"
            android.graphics.Bitmap.Config.ARGB_4444 -> "image/png"
            android.graphics.Bitmap.Config.ALPHA_8 -> "image/png"
            else -> "image/png"
        }
    }


}