package com.kernelflux.aniflux.placeholder

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Lifecycle
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.lottie.LottieDrawable

/**
 * Lottie placeholder manager
 * Uses Lottie's ImageAssetDelegate mechanism to replace images
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
class LottiePlaceholderManager(
    view: View,
    private val drawable: LottieDrawable,
    replacements: PlaceholderReplacementMap,
    imageLoader: PlaceholderImageLoader,
    lifecycle: Lifecycle?
) : PlaceholderManager(view, drawable, replacements, imageLoader, lifecycle) {
    
    private val mainHandler = getMainHandler()
    private val lottieView: LottieAnimationView? = view as? LottieAnimationView
    private val loadedBitmaps = mutableMapOf<String, Bitmap>()
    
    override fun applyReplacements() {
        // Safety check: if View type doesn't match, return directly, don't throw exception
        val lottieView = this.lottieView ?: return
        val composition = drawable.composition ?: return
        
        try {
            // Set ImageAssetDelegate for dynamically providing images
            lottieView.setImageAssetDelegate { asset ->
                try {
                    val key = asset.id
                    // If already loaded, return directly
                    loadedBitmaps[key]
                } catch (e: Exception) {
                    // Ignore exception, return null
                    null
                }
            }
        } catch (e: Exception) {
            // If setup fails, return directly, don't process placeholders
            return
        }
        
        // Asynchronously load all placeholder images
        replacements.getAll().forEach { (key, replacement) ->
            try {
                val imageAsset = composition.images[key] ?: return@forEach
                
                val request = imageLoader.load(
                    context = view.context,
                    source = replacement.imageSource,
                    width = imageAsset.width,
                    height = imageAsset.height,
                    callback = object : PlaceholderImageLoadCallback {
                        override fun onSuccess(bitmap: Bitmap) {
                            if (isCleared) return
                            
                            // Update on main thread
                            mainHandler.post {
                                if (isCleared) return@post
                                
                                try {
                                    // Check again if view is valid
                                    val currentView = this@LottiePlaceholderManager.lottieView
                                    // Save to cache
                                    loadedBitmaps[key] = bitmap
                                    
                                    // Update ImageAssetDelegate (re-set to trigger refresh)
                                    currentView.setImageAssetDelegate { asset ->
                                        try {
                                            loadedBitmaps[asset.id]
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    
                                    // Refresh view
                                    currentView.invalidate()
                                } catch (e: Exception) {
                                    // Ignore setup failure errors, don't affect main flow
                                }
                            }
                        }
                        
                        override fun onError(error: Throwable) {
                            // Error handling: fail silently, don't affect animation playback
                        }
                    }
                )
                activeRequests.add(request)
            } catch (e: Exception) {
                // Single placeholder load failure doesn't affect other placeholders
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        loadedBitmaps.clear()
        // Clear ImageAssetDelegate
        try {
            lottieView?.setImageAssetDelegate(null)
        } catch (e: Exception) {
            // Ignore exceptions during cleanup
        }
    }
}

