package com.kernelflux.aniflux.util

import android.content.Context
import java.io.InputStream

/**
 * 动画资源类型检测器
 * 用于根据文件后缀和文件头特征检测动画资源类型
 */
object AnimationTypeDetector {

    /**
     * 动画资源类型枚举
     */
    enum class AnimationType {
        GIF,        // GIF动画
        LOTTIE,     // Lottie动画 (.json)
        SVGA,       // SVGA动画 (.svga)
        PAG,        // PAG动画 (.pag)
        VAP,        // VAP动画 (.mp4)
        UNKNOWN     // 未知类型
    }

    /**
     * 路径类型枚举
     */
    enum class PathType {
        NETWORK_URL,        // 网络URL: https://example.com/animation.gif
        LOCAL_FILE,         // 本地文件路径: /storage/emulated/0/animation.gif
        ASSET_PATH,         // Asset路径: animations/loading.gif
        ASSET_URI,          // Asset URI: file:///android_asset/animations/loading.gif
        CONTENT_URI,        // Content URI: content://media/external/images/media/123
        UNKNOWN             // 未知类型
    }

    /**
     * 检测路径类型
     * @param path 路径字符串
     * @return 路径类型
     */
    fun detectPathType(path: String?): PathType {
        if (path.isNullOrEmpty()) return PathType.UNKNOWN

        return when {
            path.startsWith("http://") || path.startsWith("https://") -> PathType.NETWORK_URL
            path.startsWith("file:///android_asset/") || path.startsWith("asset://") -> PathType.ASSET_URI
            path.startsWith("content://") -> PathType.CONTENT_URI
            path.startsWith("/") -> PathType.LOCAL_FILE
            else -> PathType.ASSET_PATH
        }
    }

    /**
     * 根据文件路径检测动画类型
     * @param path 文件路径（支持网络URL、本地文件路径、Asset路径、Uri字符串）
     * @return 动画类型
     */
    fun detectFromPath(path: String?): AnimationType {
        if (path.isNullOrEmpty()) return AnimationType.UNKNOWN

        val lowerPath = path.lowercase()
        return when {
            lowerPath.endsWith(".gif") -> AnimationType.GIF
            lowerPath.endsWith(".json") || lowerPath.endsWith(".lottie") -> AnimationType.LOTTIE
            lowerPath.endsWith(".svga") -> AnimationType.SVGA
            lowerPath.endsWith(".pag") -> AnimationType.PAG
            lowerPath.endsWith(".mp4") -> AnimationType.VAP
            else -> AnimationType.UNKNOWN
        }
    }

    /**
     * 根据资源ID检测动画类型
     * @param context 上下文
     * @param resourceId 资源ID
     * @return 动画类型
     */
    fun detectFromResourceId(context: Context, resourceId: Int): AnimationType {
        return try {
            val inputStream = context.resources.openRawResource(resourceId)
            detectFromInputStream(inputStream)
        } catch (e: Exception) {
            AnimationType.UNKNOWN
        }
    }

    /**
     * 根据输入流检测动画类型
     * @param inputStream 输入流
     * @return 动画类型
     */
    fun detectFromInputStream(inputStream: InputStream): AnimationType {
        return try {
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            if (bytesRead > 0) {
                detectFromBytes(buffer, bytesRead)
            } else {
                AnimationType.UNKNOWN
            }
        } catch (e: Exception) {
            AnimationType.UNKNOWN
        }
    }

    /**
     * 根据字节数组检测动画类型
     * @param bytes 字节数组
     * @param length 有效长度
     * @return 动画类型
     */
    fun detectFromBytes(bytes: ByteArray, length: Int): AnimationType {
        if (length < 4) return AnimationType.UNKNOWN

        // 检测GIF文件头
        if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()
        ) {
            return AnimationType.GIF
        }

        // 检测JSON文件（Lottie）
        val content = String(bytes, 0, minOf(length, 1024))
        if (content.trimStart().startsWith("{") &&
            (content.contains("\"v\":") || content.contains("\"assets\":") || content.contains("\"layers\":"))
        ) {
            return AnimationType.LOTTIE
        }

        // 检测SVGA文件头（SVGA文件通常以特定字节序列开始）
        if (length >= 8) {
            // SVGA文件通常以特定的魔数开始
            if (bytes[0] == 0x53.toByte() && bytes[1] == 0x56.toByte() &&
                bytes[2] == 0x47.toByte() && bytes[3] == 0x41.toByte()
            ) {
                return AnimationType.SVGA
            }
        }

        // 检测PAG文件头（PAG文件通常以特定字节序列开始）
        if (length >= 8) {
            // PAG文件通常以特定的魔数开始
            if (bytes[0] == 0x50.toByte() && bytes[1] == 0x41.toByte() &&
                bytes[2] == 0x47.toByte() && bytes[3] == 0x00.toByte()
            ) {
                return AnimationType.PAG
            }
        }

        return AnimationType.UNKNOWN
    }

}
