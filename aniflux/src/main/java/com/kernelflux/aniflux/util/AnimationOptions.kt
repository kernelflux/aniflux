package com.kernelflux.aniflux.util

import android.widget.ImageView
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap

/**
 * Animation options
 * Configures various parameters for animation loading
 */
class AnimationOptions {
    
    // Size configuration (reserved for special cases, such as GIF size scaling, but does not affect cache key)
    var width: Int = 0
    var height: Int = 0
    var scaleType: ImageView.ScaleType? = null
    var cacheStrategy: AnimationCacheStrategy = AnimationCacheStrategy.BOTH
    
    // Animation playback configuration
    var repeatCount: Int = -1 // -1 for infinite loop, 0 for no loop, >0 for number of loops
    var autoPlay: Boolean = true // Whether to autoplay
    var retainLastFrame: Boolean = true // Whether to retain last frame (when animation ends), default true
    
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
     * Sets animation repeat count
     * @param count -1 for infinite loop, 0 for no loop, >0 for number of loops
     */
    fun repeatCount(count: Int): AnimationOptions {
        this.repeatCount = count
        return this
    }
    
    /**
     * Sets whether to autoplay
     */
    fun autoPlay(auto: Boolean): AnimationOptions {
        this.autoPlay = auto
        return this
    }
    
    /**
     * Sets whether to retain last frame (when animation ends)
     * @param retain true to retain last frame, false to clear (default true)
     */
    fun retainLastFrame(retain: Boolean): AnimationOptions {
        this.retainLastFrame = retain
        return this
    }
    
    /**
     * Sets placeholder replacement configuration (using DSL)
     * 
     * @param builder Builder for placeholder replacement configuration
     * @return this, supports fluent calls
     */
    fun placeholderReplacements(builder: PlaceholderReplacementMap.() -> Unit): AnimationOptions {
        val map = PlaceholderReplacementMap().apply(builder)
        this.placeholderReplacements = map
        return this
    }
    
    /**
     * Sets placeholder replacement configuration (direct pass-in)
     * 
     * @param map Placeholder replacement map
     * @return this, supports fluent calls
     */
    fun placeholderReplacements(map: PlaceholderReplacementMap): AnimationOptions {
        this.placeholderReplacements = map
        return this
    }
}