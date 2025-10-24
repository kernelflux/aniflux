package com.kernelflux.aniflux.load

import android.net.Uri
import java.io.File

/**
 * 动画格式检测器
 * 负责根据数据源自动识别动画格式
 * @author: kernelflux
 * @date: 2025/10/13
 */
object AnimationFormatDetector {
    
    /**
     * 支持的动画格式
     */
    enum class AnimationFormat {
        GIF,
        SVGA,
        PAG,
        LOTTIE,
        UNKNOWN
    }

    /**
     * 根据模型检测动画格式
     */
    fun detectFormat(model: Any?): AnimationFormat {
        return when (model) {
            is String -> detectFormatFromUrl(model)
            is Uri -> detectFormatFromUri(model)
            is File -> detectFormatFromFile(model)
            is Int -> detectFormatFromResourceId(model)
            else -> AnimationFormat.UNKNOWN
        }
    }

    /**
     * 从URL检测格式
     */
    private fun detectFormatFromUrl(url: String): AnimationFormat {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.endsWith(".gif") -> AnimationFormat.GIF
            lowerUrl.endsWith(".svga") -> AnimationFormat.SVGA
            lowerUrl.endsWith(".pag") -> AnimationFormat.PAG
            lowerUrl.endsWith(".json") || lowerUrl.contains("lottie") -> AnimationFormat.LOTTIE
            else -> AnimationFormat.UNKNOWN
        }
    }

    /**
     * 从URI检测格式
     */
    private fun detectFormatFromUri(uri: Uri): AnimationFormat {
        val path = uri.path ?: return AnimationFormat.UNKNOWN
        return detectFormatFromUrl(path)
    }

    /**
     * 从文件检测格式
     */
    private fun detectFormatFromFile(file: File): AnimationFormat {
        val fileName = file.name.lowercase()
        return when {
            fileName.endsWith(".gif") -> AnimationFormat.GIF
            fileName.endsWith(".svga") -> AnimationFormat.SVGA
            fileName.endsWith(".pag") -> AnimationFormat.PAG
            fileName.endsWith(".json") -> AnimationFormat.LOTTIE
            else -> AnimationFormat.UNKNOWN
        }
    }

    /**
     * 从资源ID检测格式
     * 注意：这里无法直接检测，需要根据资源名称或内容来判断
     */
    private fun detectFormatFromResourceId(resourceId: Int): AnimationFormat {
        // TODO: 实现资源ID的格式检测
        // 可能需要通过资源名称或文件内容来判断
        return AnimationFormat.UNKNOWN
    }

    /**
     * 获取格式对应的MIME类型
     */
    fun getMimeType(format: AnimationFormat): String {
        return when (format) {
            AnimationFormat.GIF -> "image/gif"
            AnimationFormat.SVGA -> "application/octet-stream"
            AnimationFormat.PAG -> "application/octet-stream"
            AnimationFormat.LOTTIE -> "application/json"
            AnimationFormat.UNKNOWN -> "application/octet-stream"
        }
    }

    /**
     * 获取格式对应的文件扩展名
     */
    fun getFileExtension(format: AnimationFormat): String {
        return when (format) {
            AnimationFormat.GIF -> ".gif"
            AnimationFormat.SVGA -> ".svga"
            AnimationFormat.PAG -> ".pag"
            AnimationFormat.LOTTIE -> ".json"
            AnimationFormat.UNKNOWN -> ""
        }
    }
}
