package com.kernelflux.aniflux.downloader

import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 下载结果
 */
sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Error(val exception: Throwable) : DownloadResult()
    data class Cancelled(val url: String) : DownloadResult()
}