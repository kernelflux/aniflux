package com.kernelflux.aniflux.cache

import com.kernelflux.aniflux.AntiFlux
import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体缓存接口
 * 支持内存缓存和磁盘缓存
 */
interface MediaCache {
    /**
     * 获取缓存
     */
    suspend fun get(key: String): CachedMedia?

    /**
     * 存储缓存
     */
    suspend fun put(key: String, media: CachedMedia)

    /**
     * 移除缓存
     */
    suspend fun remove(key: String)

    /**
     * 清空所有缓存
     */
    suspend fun clear()

    /**
     * 清空指定类型的缓存
     */
    suspend fun clearByType(type: MediaType)

    /**
     * 获取缓存大小
     */
    fun getSize(): Long

    /**
     * 获取指定类型的缓存大小
     */
    fun getSizeByType(type: MediaType): Long

    /**
     * 获取最大缓存大小
     */
    fun getMaxSize(): Long

    /**
     * 设置缓存大小限制
     */
    fun setMaxSize(maxSize: Long)

    /**
     * 获取缓存目录
     */
    val cacheDir: File

    companion object {
        @Volatile
        private var INSTANCE: MediaCache? = null

        /**
         * 获取MediaCache单例实例
         */
        @JvmStatic
        fun getInstance(): MediaCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaCacheImpl(AntiFlux.getApplicationContext()).also { INSTANCE = it }
            }
        }
    }

}
