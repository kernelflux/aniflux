package com.kernelflux.aniflux.load

import android.content.Context
import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.engine.AnimationResource
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.io.File
import java.io.InputStream

/**
 * Animation loader interface - references loading approaches of various animation libraries
 * Unifies loading logic for different animation types
 */
interface AnimationLoader<T> {

    /**
     * Load animation from file path
     */
    fun loadFromPath(context: Context, path: String): T?

    /**
     * Load animation from file
     */
    fun loadFromFile(context: Context, file: File): T?

    /**
     * Load animation from resource ID
     */
    fun loadFromResource(context: Context, resourceId: Int): T?

    /**
     * Load animation from byte array
     */
    fun loadFromBytes(context: Context, bytes: ByteArray): T?

    /**
     * Load animation from input stream
     */
    fun loadFromInputStream(context: Context, inputStream: InputStream): T?

    /**
     * Load animation from network URL
     */
    fun loadFromUrl(context: Context, url: String, downloader: AnimationDownloader): T?

    /**
     * Load animation from Asset path
     */
    fun loadFromAssetPath(context: Context, assetPath: String): T?

    /**
     * Get animation type
     */
    fun getAnimationType(): AnimationTypeDetector.AnimationType
}
