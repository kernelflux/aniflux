package com.kernelflux.aniflux.util

import android.content.Context
import java.io.InputStream

/**
 * Animation resource type detector
 * Used to detect animation resource type based on file extension and file header characteristics
 */
object AnimationTypeDetector {

    /**
     * Animation resource type enum
     */
    enum class AnimationType {
        GIF,        // GIF animation
        LOTTIE,     // Lottie animation (.json)
        SVGA,       // SVGA animation (.svga)
        PAG,        // PAG animation (.pag)
        VAP,        // VAP animation (.mp4)
        UNKNOWN     // Unknown type
    }

    /**
     * Path type enum
     */
    enum class PathType {
        NETWORK_URL,        // Network URL: https://example.com/animation.gif
        LOCAL_FILE,         // Local file path: /storage/emulated/0/animation.gif
        ASSET_PATH,         // Asset path: animations/loading.gif
        ASSET_URI,          // Asset URI: file:///android_asset/animations/loading.gif
        CONTENT_URI,        // Content URI: content://media/external/images/media/123
        UNKNOWN             // Unknown type
    }

    /**
     * Detect path type
     * @param path Path string
     * @return Path type
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
     * Detect animation type from file path
     * @param path File path (supports network URL, local file path, Asset path, Uri string)
     * @return Animation type
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
     * Detect animation type from resource ID
     * @param context Context
     * @param resourceId Resource ID
     * @return Animation type
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
     * Detect animation type from input stream
     * @param inputStream Input stream
     * @return Animation type
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
     * Detect animation type from byte array
     * @param bytes Byte array
     * @param length Valid length
     * @return Animation type
     */
    fun detectFromBytes(bytes: ByteArray, length: Int): AnimationType {
        if (length < 4) return AnimationType.UNKNOWN

        // Detect GIF file header
        if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()
        ) {
            return AnimationType.GIF
        }

        // Detect JSON file (Lottie)
        val content = String(bytes, 0, minOf(length, 1024))
        if (content.trimStart().startsWith("{") &&
            (content.contains("\"v\":") || content.contains("\"assets\":") || content.contains("\"layers\":"))
        ) {
            return AnimationType.LOTTIE
        }

        // Detect SVGA file header (SVGA files usually start with specific byte sequence)
        if (length >= 8) {
            // SVGA files usually start with specific magic number
            if (bytes[0] == 0x53.toByte() && bytes[1] == 0x56.toByte() &&
                bytes[2] == 0x47.toByte() && bytes[3] == 0x41.toByte()
            ) {
                return AnimationType.SVGA
            }
        }

        // Detect PAG file header (PAG files usually start with specific byte sequence)
        if (length >= 8) {
            // PAG files usually start with specific magic number
            if (bytes[0] == 0x50.toByte() && bytes[1] == 0x41.toByte() &&
                bytes[2] == 0x47.toByte() && bytes[3] == 0x00.toByte()
            ) {
                return AnimationType.PAG
            }
        }

        return AnimationType.UNKNOWN
    }

}
