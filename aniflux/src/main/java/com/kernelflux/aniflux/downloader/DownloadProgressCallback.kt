package com.kernelflux.aniflux.downloader

import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 下载进度回调
 */
interface DownloadProgressCallback {
    fun onProgress(url: String, progress: Float)
    fun onComplete(url: String, file: File)
    fun onError(url: String, exception: Throwable)
}