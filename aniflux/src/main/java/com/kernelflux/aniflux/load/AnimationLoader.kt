package com.kernelflux.aniflux.load

import android.content.Context
import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.engine.AnimationResource
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.io.File
import java.io.InputStream

/**
 * 动画加载器接口 - 参考各动画库的加载方式
 * 统一不同动画类型的加载逻辑
 */
interface AnimationLoader<T> {

    /**
     * 从文件路径加载动画
     */
    fun loadFromPath(context: Context, path: String): T?

    /**
     * 从文件加载动画
     */
    fun loadFromFile(context: Context, file: File): T?

    /**
     * 从资源ID加载动画
     */
    fun loadFromResource(context: Context, resourceId: Int): T?

    /**
     * 从字节数组加载动画
     */
    fun loadFromBytes(context: Context, bytes: ByteArray): T?

    /**
     * 从输入流加载动画
     */
    fun loadFromInputStream(context: Context, inputStream: InputStream): T?

    /**
     * 从网络URL加载动画
     */
    fun loadFromUrl(context: Context, url: String, downloader: AnimationDownloader): T?

    /**
     * 从Asset路径加载动画
     */
    fun loadFromAssetPath(context: Context, assetPath: String): T?

    /**
     * 获取动画类型
     */
    fun getAnimationType(): AnimationTypeDetector.AnimationType
}
