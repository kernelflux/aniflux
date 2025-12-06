package com.kernelflux.aniflux

import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader

/**
 * AniFlux configuration class
 * Used to configure various components of the framework
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
class AniFluxConfiguration {
    /**
     * Placeholder image loader (implemented by business)
     */
    var placeholderImageLoader: PlaceholderImageLoader? = null
    
    /**
     * Whether to enable animation compatibility handling
     * When enabled, the framework will automatically handle system animation settings
     * to ensure animations work correctly even when system animations are disabled in developer options.
     * 
     * Default: true (enabled)
     */
    var enableAnimationCompatibility: Boolean = true
    
    /**
     * Set placeholder image loader
     * 
     * @param loader Placeholder image loader implementation
     * @return this, supports method chaining
     */
    fun setPlaceholderImageLoader(loader: PlaceholderImageLoader): AniFluxConfiguration {
        this.placeholderImageLoader = loader
        return this
    }
    
    /**
     * Set whether to enable animation compatibility handling
     * 
     * @param enable true to enable (default), false to disable
     * @return this, supports method chaining
     */
    fun setEnableAnimationCompatibility(enable: Boolean): AniFluxConfiguration {
        this.enableAnimationCompatibility = enable
        return this
    }
}

