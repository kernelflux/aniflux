package com.kernelflux.aniflux.placeholder

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Base class for placeholder manager
 * Responsible for managing placeholder loading and application
 * 
 * This is a generic base class that doesn't depend on any specific animation format
 * Specific implementations (such as PAGPlaceholderManager, LottiePlaceholderManager) should be in their respective format modules
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
        // Shared main thread Handler to avoid creating new Handler for each Manager
        @JvmStatic
        private val MAIN_HANDLER = android.os.Handler(android.os.Looper.getMainLooper())
        
        /**
         * Get shared main thread Handler
         */
        @JvmStatic
        fun getMainHandler(): android.os.Handler = MAIN_HANDLER
    }

    init {
        // Listen to lifecycle for automatic cleanup
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
     * Apply placeholder replacements
     * Subclasses implement specific replacement logic
     */
    abstract fun applyReplacements()

    /**
     * Clear resources
     */
    fun clear() {
        if (isCleared) return

        isCleared = true

        // Cancel all loading requests
        activeRequests.forEach { request ->
            try {
                imageLoader.cancel(request)
            } catch (e: Exception) {
                // Ignore exceptions when canceling
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

