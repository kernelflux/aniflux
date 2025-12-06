package com.kernelflux.aniflux.engine

import android.annotation.SuppressLint
import android.content.Context
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.aniflux.cache.AnimationDiskCache
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.OkHttpAnimationDownloader
import com.kernelflux.aniflux.util.AnimationKey
import java.io.File

/**
 * Download and cache helper class for AnimationJob
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
     * Download network resource and save to disk cache (if needed)
     * @param url Network URL
     * @return Pair(downloaded file, whether from cache)
     */
    @SuppressLint("LongLogTag")
    fun downloadAndCache(url: String): Pair<File?, Boolean> {
        return try {
            // 1. First try to get from disk cache
            if (animationDiskCache != null && shouldUseDiskCache()) {
                val cachedFile = animationDiskCache.get(key.toCacheKey())
                if (cachedFile != null && cachedFile.exists()) {
                    AniFluxLog.d(AniFluxLogCategory.ENGINE, "Using cached file: ${cachedFile.absolutePath}")
                    return Pair(cachedFile, true)
                }
            }
            
            // 2. Network download
            val downloadedFile = downloader.download(context, url)
            
            // 3. If disk cache is needed, save to cache
            if (animationDiskCache != null && shouldUseDiskCache()) {
                animationDiskCache.put(key.toCacheKey(), downloadedFile)
                AniFluxLog.d(AniFluxLogCategory.ENGINE, "Cached downloaded file: ${downloadedFile.absolutePath}")
            }
            
            Pair(downloadedFile, false)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.ENGINE, "Failed to download and cache: $url", e)
            Pair(null, false)
        }
    }
    
    /**
     * Determine whether should use disk cache
     */
    private fun shouldUseDiskCache(): Boolean {
        return key.cacheStrategy == AnimationCacheStrategy.DISK_ONLY ||
               key.cacheStrategy == AnimationCacheStrategy.BOTH
    }
}

