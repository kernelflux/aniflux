package com.kernelflux.aniflux.placeholder

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.pag.PAGFile
import com.kernelflux.svga.SVGADrawable

/**
 * Base class for placeholder managers
 * Responsible for managing the loading and application of placeholders
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
abstract class PlaceholderManager protected constructor(
    protected val view: View,
    protected val resource: Any,
    protected val replacements: PlaceholderReplacementMap,
    protected val imageLoader: PlaceholderImageLoader,
    protected val lifecycle: Lifecycle?
) {
    protected val activeRequests = mutableListOf<PlaceholderImageLoadRequest>()
    protected var isCleared = false
    private var lifecycleObserver: LifecycleEventObserver? = null
    
    companion object {
        // Shared main thread Handler, avoids creating a new Handler for each Manager
        @JvmStatic
        private val MAIN_HANDLER = android.os.Handler(android.os.Looper.getMainLooper())
        
        /**
         * Gets the shared main thread Handler
         */
        @JvmStatic
        fun getMainHandler(): android.os.Handler = MAIN_HANDLER


        /**
         * Creates placeholder manager
         * Automatically selects corresponding manager based on resource type
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

                is PAGFile -> {
                    PAGPlaceholderManager(view, resource, replacements, imageLoader, lifecycle)
                }

                is LottieDrawable -> {
                    LottiePlaceholderManager(view, resource, replacements, imageLoader, lifecycle)
                }

                else -> null
            }
        }

    }

    init {
        // Listen to lifecycle, automatically clear
        try {
            lifecycle?.addObserver(LifecycleEventObserver { source, event ->
                try {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        clear()
                    }
                } catch (e: Exception) {
                    // Ignore exceptions in lifecycle callbacks
                }
            }.also { lifecycleObserver = it })
        } catch (e: Exception) {
            // Ignore exceptions when adding lifecycle listener
        }
    }

    /**
     * Applies placeholder replacements
     * Subclasses implement specific replacement logic
     */
    abstract fun applyReplacements()

    /**
     * Clears resources
     */
    fun clear() {
        if (isCleared) return

        isCleared = true

        // Cancel all loading requests
        activeRequests.forEach { request ->
            try {
                imageLoader.cancel(request)
            } catch (e: Exception) {
                // Ignore exceptions during cancellation
            }
        }
        activeRequests.clear()

        // Remove lifecycle listener
        try {
            lifecycleObserver?.let { observer ->
                lifecycle?.removeObserver(observer)
            }
        } catch (e: Exception) {
            // Ignore exceptions when removing listener
        }
        lifecycleObserver = null

        // Subclasses can override this method for additional cleanup
        onCleared()
    }

    /**
     * Subclasses can override this method for additional cleanup
     */
    protected open fun onCleared() {
        // Default empty implementation
    }
}

