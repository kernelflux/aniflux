package com.kernelflux.aniflux.svga

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoadCallback
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap
import com.kernelflux.svga.SVGADynamicEntity
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.svga.SVGAVideoEntity

/**
 * SVGA placeholder manager
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
class SVGAPlaceholderManager(
    view: View,
    private val drawable: SVGADrawable,
    replacements: PlaceholderReplacementMap,
    imageLoader: PlaceholderImageLoader,
    lifecycle: Lifecycle?
) : PlaceholderManager(view, drawable, replacements, imageLoader, lifecycle) {

    private val mainHandler = getMainHandler()
    private val dynamicEntity = SVGADynamicEntity()
    private var appliedCount = 0
    private val totalCount = replacements.getAll().size
    private var pendingUpdate = false
    private var videoItem: SVGAVideoEntity? = null
    private var wasAnimating = false

    override fun applyReplacements() {
        videoItem = drawable.videoItem
        // Safety check: if View type doesn't match, return directly without throwing exception
        val svgaView = view as? SVGAImageView ?: return

        if (totalCount == 0) return

        // Save current playback state
        wasAnimating = try {
            svgaView.isAnimating
        } catch (e: Exception) {
            false
        }

        // Load all placeholder images
        replacements.getAll().forEach { (key, replacement) ->
            try {
                val request = imageLoader.load(
                    context = view.context,
                    source = replacement.imageSource,
                    width = 0,  // SVGA will handle based on placeholder original size
                    height = 0,
                    callback = object : PlaceholderImageLoadCallback {
                        override fun onSuccess(bitmap: Bitmap) {
                            if (isCleared) return

                            // Update on main thread
                            mainHandler.post {
                                if (isCleared) return@post

                                try {
                                    // Check again if view is valid
                                    val currentView = view as? SVGAImageView ?: return@post
                                    // Set placeholder image
                                    dynamicEntity.setDynamicImage(bitmap, key)
                                    appliedCount++
                                    pendingUpdate = true

                                    // If all placeholder images are loaded, update immediately
                                    if (appliedCount >= totalCount) {
                                        updateViewImmediately(currentView, wasAnimating)
                                    } else {
                                        // Partially loaded, delay batch update (avoid frequent setVideoItem calls)
                                        mainHandler.postDelayed({
                                            if (!isCleared && pendingUpdate) {
                                                updateViewImmediately(currentView, wasAnimating)
                                                pendingUpdate = false
                                            }
                                        }, 50) // Updates within 50ms will be merged
                                    }
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

        // Set once first (some placeholders may have already loaded or use cache)
        try {
            svgaView.setVideoItem(videoItem, dynamicEntity)

            // If was playing before, restart animation
            if (wasAnimating) {
                try {
                    svgaView.startAnimation()
                } catch (e: Exception) {
                    // Ignore animation start failure
                }
            }
        } catch (e: Exception) {
            // Ignore setup failure errors
        }
    }

    /**
     * Update view immediately (batch update, reduce setVideoItem call count)
     */
    private fun updateViewImmediately(view: SVGAImageView, shouldRestartAnimation: Boolean) {
        val currentVideoItem = videoItem ?: return
        try {
            view.setVideoItem(currentVideoItem, dynamicEntity)

            // If was playing before, restart animation
            if (shouldRestartAnimation) {
                try {
                    view.startAnimation()
                } catch (e: Exception) {
                    // Ignore animation start failure
                }
            }
        } catch (e: Exception) {
            // Ignore setup failure errors
        }
    }

    override fun onCleared() {
        super.onCleared()
        appliedCount = 0
        pendingUpdate = false
    }
}

