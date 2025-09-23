package com.kernelflux.aniflux.cache

import android.graphics.Bitmap
import com.airbnb.lottie.LottieComposition
import com.opensource.svgaplayer.SVGAVideoEntity
import org.libpag.PAGFile
import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 缓存的媒体数据 - 支持多种媒体类型
 */
sealed class CachedMedia {
    /**
     * 文件缓存 - 用于原始文件
     */
    data class FileCache(
        val file: File,
        val contentType: String,
        val size: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : CachedMedia()

    /**
     * 图片缓存 - 用于Bitmap
     */
    data class ImageCache(
        val bitmap: Bitmap,
        val contentType: String,
        val size: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : CachedMedia()

    /**
     * PAG动画缓存 - 用于PAGFile
     */
    data class PAGCache(
        val pagFile: PAGFile,
        val contentType: String,
        val size: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : CachedMedia()

    /**
     * SVGA动画缓存 - 用于SVGAVideoEntity
     */
    data class SVGACache(
        val sVGAEntity: SVGAVideoEntity,
        val contentType: String,
        val size: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : CachedMedia()

    /**
     * Lottie动画缓存 - 用于LottieComposition
     */
    data class LottieCache(
        val lottieComposition: LottieComposition,
        val contentType: String,
        val size: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : CachedMedia()

    /**
     * 获取缓存大小
     */
    fun getSize(): Long = when (this) {
        is FileCache -> size
        is ImageCache -> size
        is PAGCache -> size
        is SVGACache -> size
        is LottieCache -> size
    }

    /**
     * 获取内容类型
     */
    fun getContentType(): String = when (this) {
        is FileCache -> contentType
        is ImageCache -> contentType
        is PAGCache -> contentType
        is SVGACache -> contentType
        is LottieCache -> contentType
    }

    /**
     * 获取时间戳
     */
    fun getTimestamp(): Long = when (this) {
        is FileCache -> timestamp
        is ImageCache -> timestamp
        is PAGCache -> timestamp
        is SVGACache -> timestamp
        is LottieCache -> timestamp
    }
}