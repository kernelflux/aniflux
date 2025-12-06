package com.kernelflux.aniflux.svga

import android.view.View
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap
import com.kernelflux.svga.SVGADrawable

/**
 * SVGA placeholder manager factory
 * Provides factory method to create SVGAPlaceholderManager
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
object PlaceholderManagerFactory {
    /**
     * Create SVGA placeholder manager
     * 
     * @param view View displaying animation
     * @param resource SVGADrawable resource
     * @param replacements Placeholder replacement map
     * @param imageLoader Image loader
     * @param lifecycle Lifecycle (optional)
     * @return PlaceholderManager instance, returns null if parameters are invalid
     */
    @JvmStatic
    fun create(
        view: View,
        resource: Any,
        replacements: PlaceholderReplacementMap,
        imageLoader: PlaceholderImageLoader?,
        lifecycle: Lifecycle?
    ): PlaceholderManager? {
        if (imageLoader == null || replacements.isEmpty()) {
            return null
        }

        return when (resource) {
            is SVGADrawable -> {
                SVGAPlaceholderManager(view, resource, replacements, imageLoader, lifecycle)
            }
            else -> null
        }
    }
}

