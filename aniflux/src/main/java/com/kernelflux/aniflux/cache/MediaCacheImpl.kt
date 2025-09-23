package com.kernelflux.aniflux.cache

import android.content.Context
import com.kernelflux.aniflux.AntiFlux
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体缓存实现
 * 支持内存缓存和磁盘缓存
 */
class MediaCacheImpl(
    private var maxMemorySize: Long = 50 * 1024 * 1024, // 50MB
    private var maxDiskSize: Long = 200 * 1024 * 1024   // 200MB
) : MediaCache {

    // 内存缓存 - 使用统一的缓存接口
    private val memoryCaches = mutableMapOf<MediaType, LruMemoryCache<CachedMedia>>()

    // 磁盘缓存索引
    private val diskCacheIndex = ConcurrentHashMap<String, CachedMedia.FileCache>()

    // 互斥锁
    private val mutex = Mutex()

    init {
        // 初始化内存缓存
        initializeMemoryCaches()

        // 加载磁盘缓存索引
        loadDiskCacheIndex()
    }

    /**
     * 初始化内存缓存
     */
    private fun initializeMemoryCaches() {
        memoryCaches[MediaType.FILE] = LruMemoryCache(maxMemorySize / 5)
        memoryCaches[MediaType.IMAGE] = LruMemoryCache(maxMemorySize / 5)
        memoryCaches[MediaType.PAG] = LruMemoryCache(maxMemorySize / 3)
        memoryCaches[MediaType.SVGA] = LruMemoryCache(maxMemorySize / 3)
        memoryCaches[MediaType.LOTTIE] = LruMemoryCache(maxMemorySize / 5)
    }

    private fun checkCacheDir() {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    override val cacheDir = File(context.cacheDir, "media_cache")

    override suspend fun get(key: String): CachedMedia? {
        return mutex.withLock {
            // 1. 先检查内存缓存
            for (cache in memoryCaches.values) {
                val result = cache.get(key)
                if (result != null) {
                    return@withLock result
                }
            }

            // 2. 检查磁盘缓存（只支持文件缓存）
            val diskResult = diskCacheIndex[key]
            if (diskResult != null && diskResult.file.exists()) {
                return@withLock diskResult
            }

            // 3. 清理无效的磁盘缓存索引
            if (diskResult != null && !diskResult.file.exists()) {
                diskCacheIndex.remove(key)
            }

            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun put(key: String, media: CachedMedia) {
        mutex.withLock {
            when (media) {
                is CachedMedia.FileCache -> {
                    // 文件缓存：存储到内存和磁盘
                    val fileCache =
                        memoryCaches[MediaType.FILE] as? LruMemoryCache<CachedMedia.FileCache>
                    fileCache?.put(key, media)
                    diskCacheIndex[key] = media
                    checkDiskCacheSize()
                }

                is CachedMedia.ImageCache -> {
                    // 图片缓存：只存储到内存
                    val imageCache =
                        memoryCaches[MediaType.IMAGE] as? LruMemoryCache<CachedMedia.ImageCache>
                    imageCache?.put(key, media)
                }

                is CachedMedia.PAGCache -> {
                    // PAG缓存：只存储到内存
                    val pagCache =
                        memoryCaches[MediaType.PAG] as? LruMemoryCache<CachedMedia.PAGCache>
                    pagCache?.put(key, media)
                }

                is CachedMedia.SVGACache -> {
                    // SVGA缓存：只存储到内存
                    val sVGACache =
                        memoryCaches[MediaType.SVGA] as? LruMemoryCache<CachedMedia.SVGACache>
                    sVGACache?.put(key, media)
                }

                is CachedMedia.LottieCache -> {
                    // Lottie缓存：只存储到内存
                    val lottieCache =
                        memoryCaches[MediaType.LOTTIE] as? LruMemoryCache<CachedMedia.LottieCache>
                    lottieCache?.put(key, media)
                }
            }
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            // 从所有内存缓存中移除
            memoryCaches.values.forEach { cache ->
                cache.remove(key)
            }

            // 从磁盘缓存中移除
            val diskResult = diskCacheIndex.remove(key)
            if (diskResult != null) {
                try {
                    diskResult.file.delete()
                } catch (e: Exception) {
                    // 忽略删除失败
                }
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            // 清空所有内存缓存
            memoryCaches.values.forEach { cache ->
                cache.clear()
            }

            // 清空磁盘缓存
            diskCacheIndex.clear()

            // 删除所有缓存文件
            try {
                checkCacheDir()
                cacheDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                // 忽略删除失败
            }
        }
    }

    override suspend fun clearByType(type: MediaType) {
        mutex.withLock {
            memoryCaches[type]?.clear()

            if (type == MediaType.FILE) {
                // 清空磁盘缓存
                diskCacheIndex.clear()
                try {
                    checkCacheDir()
                    cacheDir.listFiles()?.forEach { it.delete() }
                } catch (e: Exception) {
                    // 忽略删除失败
                }
            }
        }
    }

    override fun getSize(): Long {
        return memoryCaches.values.sumOf { it.size() } + diskCacheIndex.values.sumOf { it.size }
    }

    override fun getSizeByType(type: MediaType): Long {
        return memoryCaches[type]?.size() ?: 0L
    }

    override fun getMaxSize(): Long {
        return maxMemorySize + maxDiskSize
    }

    override fun setMaxSize(maxSize: Long) {
        // 动态调整缓存大小
        val newMemorySize = (maxSize * 0.8).toLong() // 80%给内存缓存
        val newDiskSize = (maxSize * 0.2).toLong()   // 20%给磁盘缓存

        // 更新最大大小
        maxMemorySize = newMemorySize
        maxDiskSize = newDiskSize

        // 重新初始化内存缓存
        initializeMemoryCaches()
    }

    /**
     * 加载磁盘缓存索引
     */
    private fun loadDiskCacheIndex() {
        try {
            checkCacheDir()
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val key = file.name
                    val cachedMedia = CachedMedia.FileCache(
                        file = file,
                        contentType = getContentType(file),
                        size = file.length()
                    )
                    diskCacheIndex[key] = cachedMedia
                }
            }
        } catch (e: Exception) {
            // 忽略加载失败
        }
    }

    /**
     * 检查磁盘缓存大小
     */
    private fun checkDiskCacheSize() {
        val currentSize = diskCacheIndex.values.sumOf { it.size }
        if (currentSize > maxDiskSize) {
            // 清理最旧的缓存
            val sortedEntries = diskCacheIndex.entries.sortedBy { it.value.timestamp }
            val toRemove = sortedEntries.take((sortedEntries.size * 0.3).toInt()) // 清理30%

            toRemove.forEach { (key, cachedMedia) ->
                diskCacheIndex.remove(key)
                try {
                    cachedMedia.file.delete()
                } catch (e: Exception) {
                    // 忽略删除失败
                }
            }
        }
    }

    /**
     * 获取文件内容类型
     */
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


}
