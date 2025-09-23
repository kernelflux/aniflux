package com.kernelflux.aniflux.request

import com.kernelflux.aniflux.config.CacheStrategy
import com.kernelflux.aniflux.config.LoadOptions
import com.kernelflux.aniflux.config.LoadResource
import com.kernelflux.aniflux.config.Priority
import com.kernelflux.aniflux.lifecycle.LifecycleTarget
import com.kernelflux.aniflux.listener.AnimationPlaybackListener
import com.kernelflux.aniflux.listener.MediaRequestListener

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *  请求Builder构造类
 */
abstract class MediaRequestBuilder<T : LifecycleTarget>(
    protected val lifecycleTarget: T
) {
    private val optionsBuilder = LoadOptionsBuilder()

    fun loadResource(loadResource: LoadResource): MediaRequestBuilder<T> {
        optionsBuilder.loadResource = loadResource
        return this
    }

    fun placeholder(resourceId: Int): MediaRequestBuilder<T> {
        optionsBuilder.placeholderResId = resourceId
        return this
    }

    fun error(resourceId: Int): MediaRequestBuilder<T> {
        optionsBuilder.errorResId = resourceId
        return this
    }

    fun override(width: Int, height: Int): MediaRequestBuilder<T> {
        optionsBuilder.apply {
            overrideWidth = width
            overrideHeight = height
        }
        return this
    }

    fun cacheStrategy(cacheStrategy: CacheStrategy): MediaRequestBuilder<T> {
        optionsBuilder.cacheStrategy = cacheStrategy
        return this
    }

    fun priority(priority: Priority): MediaRequestBuilder<T> {
        optionsBuilder.priority = priority
        return this
    }

    fun autoPlay(autoPlay: Boolean): MediaRequestBuilder<T> {
        optionsBuilder.autoPlay = autoPlay
        return this
    }

    fun repeatCount(repeatCount: Int): MediaRequestBuilder<T> {
        optionsBuilder.repeatCount = repeatCount
        return this
    }

    fun startFrame(startFrame: Int): MediaRequestBuilder<T> {
        optionsBuilder.startFrame = startFrame
        return this
    }

    fun endFrame(endFrame: Int): MediaRequestBuilder<T> {
        optionsBuilder.endFrame = endFrame
        return this
    }

    fun skipMemory(skipMemory: Boolean): MediaRequestBuilder<T> {
        optionsBuilder.skipMemory = skipMemory
        return this
    }

    fun loadFirstFrame(loadFirstFrameMode: Boolean): MediaRequestBuilder<T> {
        optionsBuilder.loadFirstFrameMode = loadFirstFrameMode
        return this
    }


    fun requestListener(listener: MediaRequestListener): MediaRequestBuilder<T> {
        optionsBuilder.listener = listener
        return this
    }

    fun animationListener(animationPlaybackListener: AnimationPlaybackListener): MediaRequestBuilder<T> {
        optionsBuilder.animationPlaybackListener = animationPlaybackListener
        return this
    }


    protected fun buildLoadOptions(): LoadOptions {
        return optionsBuilder.build()
    }

    private class LoadOptionsBuilder {
        var loadResource: LoadResource? = null
        var placeholderResId: Int? = null
        var errorResId: Int? = null
        var overrideWidth: Int? = null
        var overrideHeight: Int? = null
        var cacheStrategy: CacheStrategy = CacheStrategy.ALL
        var priority: Priority = Priority.NORMAL
        var listener: MediaRequestListener? = null
        var animationPlaybackListener: AnimationPlaybackListener? = null
        var autoPlay: Boolean = true
        var repeatCount: Int = -1
        var startFrame: Int = 0
        var endFrame: Int = -1
        var skipMemory: Boolean = false
        var loadFirstFrameMode: Boolean = false

        fun build(): LoadOptions {
            return LoadOptions(
                loadResource = loadResource,
                placeholderResId = placeholderResId,
                errorResId = errorResId,
                overrideWidth = overrideWidth,
                overrideHeight = overrideHeight,
                cacheStrategy = cacheStrategy,
                priority = priority,
                listener = listener,
                animationPlaybackListener = animationPlaybackListener,
                autoPlay = autoPlay,
                repeatCount = repeatCount,
                startFrame = startFrame,
                endFrame = endFrame,
                skipMemory = skipMemory,
                loadFirstFrameMode = loadFirstFrameMode,
            )
        }
    }

}