package com.kernelflux.aniflux.util

import android.widget.ImageView
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap

/**
 * Animation options
 * Configure various parameters for animation loading
 */
class AnimationOptions {
    
    // Size configuration (reserved for special cases, such as GIF size scaling, but doesn't affect cache key)
    var width: Int = 0
    var height: Int = 0
    var scaleType: ImageView.ScaleType? = null
    var cacheStrategy: AnimationCacheStrategy = AnimationCacheStrategy.BOTH
    
    // Animation playback configuration
    var repeatCount: Int = -1 // -1 means infinite loop, 0 means no loop, >0 means loop count
    var autoPlay: Boolean = true // Whether to auto play
    var retainLastFrame: Boolean = true // Whether to retain last frame (at animation end), default true
    
    // Placeholder replacement configuration
    var placeholderReplacements: PlaceholderReplacementMap? = null
    
    companion object {
        @JvmStatic
        fun create(): AnimationOptions = AnimationOptions()
    }
    
    fun size(width: Int, height: Int): AnimationOptions {
        this.width = width
        this.height = height
        return this
    }
    
    fun scaleType(scaleType: ImageView.ScaleType): AnimationOptions {
        this.scaleType = scaleType
        return this
    }
    
    fun cacheStrategy(strategy: AnimationCacheStrategy): AnimationOptions {
        this.cacheStrategy = strategy
        return this
    }

    /**
     * Set animation repeat count
     * @param count -1 means infinite loop, 0 means no loop, >0 means loop count
     */
    fun repeatCount(count: Int): AnimationOptions {
        this.repeatCount = count
        return this
    }
    
    /**
     * Set whether to auto play
     */
    fun autoPlay(auto: Boolean): AnimationOptions {
        this.autoPlay = auto
        return this
    }
    
    /**
     * Set whether to retain last frame (at animation end)
     * @param retain true means retain last frame, false means clear (default true)
     */
    fun retainLastFrame(retain: Boolean): AnimationOptions {
        this.retainLastFrame = retain
        return this
    }
    
    /**
     * Set placeholder replacement configuration (using DSL)
     * 
     * @param builder Placeholder replacement configuration builder
     * @return this, supports chain calls
     */
    fun placeholderReplacements(builder: PlaceholderReplacementMap.() -> Unit): AnimationOptions {
        val map = PlaceholderReplacementMap().apply(builder)
        this.placeholderReplacements = map
        return this
    }
    
    /**
     * Set placeholder replacement configuration (direct pass)
     * 
     * @param map Placeholder replacement map
     * @return this, supports chain calls
     */
    fun placeholderReplacements(map: PlaceholderReplacementMap): AnimationOptions {
        this.placeholderReplacements = map
        return this
    }
}