package com.kernelflux.aniflux.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.aniflux.cache.AnimationDiskCache
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.OkHttpAnimationDownloader
import com.kernelflux.aniflux.util.AnimationKey
import java.io.File

/**
 * AnimationJob 的下载和缓存辅助类
 */
internal class AnimationJobDownloadHelper(
    private val context: Context,
    private val key: AnimationKey,
    private val animationDiskCache: AnimationDiskCache?,
    private val downloader: AnimationDownloader = OkHttpAnimationDownloader()
) {
    
    companion object {
        private const val TAG = "AnimationJobDownloadHelper"
    }
    
    /**
     * 下载网络资源并保存到磁盘缓存（如果需要）
     * @param url 网络 URL
     * @return Pair(下载的文件, 是否来自缓存)
     */
    @SuppressLint("LongLogTag")
    fun downloadAndCache(url: String): Pair<File?, Boolean> {
        return try {
            // 1. 先尝试从磁盘缓存获取
            if (animationDiskCache != null && shouldUseDiskCache()) {
                val cachedFile = animationDiskCache.get(key.toCacheKey())
                if (cachedFile != null && cachedFile.exists()) {
                    Log.d(TAG, "Using cached file: ${cachedFile.absolutePath}")
                    return Pair(cachedFile, true)
                }
            }
            
            // 2. 网络下载
            val downloadedFile = downloader.download(context, url)
            
            // 3. 如果需要磁盘缓存，保存到缓存
            if (animationDiskCache != null && shouldUseDiskCache()) {
                animationDiskCache.put(key.toCacheKey(), downloadedFile)
                Log.d(TAG, "Cached downloaded file: ${downloadedFile.absolutePath}")
            }
            
            Pair(downloadedFile, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and cache: $url", e)
            Pair(null, false)
        }
    }
    
    /**
     * 判断是否应该使用磁盘缓存
     */
    private fun shouldUseDiskCache(): Boolean {
        return key.cacheStrategy == AnimationCacheStrategy.DISK_ONLY ||
               key.cacheStrategy == AnimationCacheStrategy.BOTH
    }
}

