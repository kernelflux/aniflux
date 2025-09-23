package com.kernelflux.aniflux.config

import android.graphics.drawable.Drawable

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体加载错误信息
 */
data class MediaError(
    val code: Int,
    val message: String,
    val throwable: Throwable? = null,
    val errorDrawable: Drawable? = null,
    val retry: Boolean = true
) {
    companion object {
        // 错误码定义
        const val CODE_NETWORK_ERROR = 1001
        const val CODE_DECODE_ERROR = 1002
        const val CODE_CACHE_ERROR = 1003
        const val CODE_UNKNOWN_ERROR = 1004
        const val CODE_TIMEOUT_ERROR = 1005
        const val CODE_FILE_NOT_FOUND = 1006
        const val CODE_INVALID_FORMAT = 1007

        // 便捷创建方法
        @JvmStatic
        fun networkError(message: String, throwable: Throwable? = null): MediaError {
            return MediaError(CODE_NETWORK_ERROR, message, throwable, retry = true)
        }

        @JvmStatic
        fun decodeError(message: String, throwable: Throwable? = null): MediaError {
            return MediaError(CODE_DECODE_ERROR, message, throwable, retry = false)
        }

        @JvmStatic
        fun timeoutError(message: String = "Request timeout"): MediaError {
            return MediaError(CODE_TIMEOUT_ERROR, message, retry = true)
        }

        @JvmStatic
        fun fileNotFoundError(message: String = "File not found"): MediaError {
            return MediaError(CODE_FILE_NOT_FOUND, message, retry = false)
        }

        @JvmStatic
        fun invalidFormatError(message: String = "Invalid format"): MediaError {
            return MediaError(CODE_INVALID_FORMAT, message, retry = false)
        }

        @JvmStatic
        fun unknownError(
            message: String = "Unknown error",
            throwable: Throwable? = null
        ): MediaError {
            return MediaError(CODE_UNKNOWN_ERROR, message, throwable, retry = true)
        }
    }
}