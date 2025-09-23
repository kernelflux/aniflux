package com.kernelflux.aniflux.downloader

import com.kernelflux.aniflux.AntiFlux
import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体下载器接口
 */
interface MediaDownloader {
    /**
     * 下载媒体文件
     */
    suspend fun download(url: String, targetFile: File): DownloadResult

    /**
     * 取消下载
     */
    fun cancel(url: String)

    /**
     * 获取下载进度
     */
    fun getProgress(url: String): Float

    companion object {
        @Volatile
        private var INSTANCE: MediaDownloader? = null

        /**
         * 获取MediaDownloader单例实例
         */
        @JvmStatic
        fun getInstance(): MediaDownloader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaDownloaderImpl(AntiFlux.getApplicationContext()).also { INSTANCE = it }
            }
        }
    }

}