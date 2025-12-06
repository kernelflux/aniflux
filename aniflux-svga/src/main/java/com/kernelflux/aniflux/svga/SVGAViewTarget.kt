package com.kernelflux.aniflux.svga

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
import android.view.View
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.svga.SVGACallback
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView

/**
 * Dedicated ViewTarget for SVGA animation
 * Automatically handles SVGADrawable resource setup to SVGAImageView
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
class SVGAViewTarget(view: SVGAImageView) :
    CustomViewAnimationTarget<SVGAImageView, SVGADrawable>(view) {

    private var placeholderManager: PlaceholderManager? = null
    private var currentAdapter: SVGAPlayListenerAdapter? = null
    private var currentCallback: SVGACallback? = null

    override fun setupPlayListeners(resource: Any, view: View?) {
        val svgaView = view as? SVGAImageView ?: return
        val listener = playListener ?: return

        // Remove old listener
        currentCallback?.let { oldCallback ->
            try {
                svgaView.callback = null
            } catch (e: Exception) {
                // Ignore exceptions when removing
            }
        }

        // Create new adapter
        val adapter = SVGAPlayListenerAdapter(listener)
        val svgaCallback = adapter.createAnimatorListener()
        svgaView.callback = svgaCallback

        // Save reference for cleanup
        currentAdapter = adapter
        currentCallback = svgaCallback
    }

    override fun onResourceReady(resource: SVGADrawable) {
        // Set listener first (avoid missing onAnimationStart)
        setupPlayListeners(resource, view)

        // Get configuration options
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true
        val retainLastFrame = animationOptions?.retainLastFrame ?: true

        view.apply {
            setVideoItem(resource.videoItem)
            setPlayRepeatCount(
                when {
                    repeatCount < 0 -> ValueAnimator.INFINITE  // -1
                    repeatCount == 0 -> 0  // Play once
                    else -> repeatCount  // ✅ Directly use total play count, let SVGAImageView.play() uniformly convert to ValueAnimator's repeat count
                }
            )
            // ✅ Set fillMode based on retainLastFrame configuration
            fillMode = if (retainLastFrame) {
                SVGAImageView.FillMode.Forward  // Retain last frame
            } else {
                SVGAImageView.FillMode.Clear    // Clear
            }
            // If auto play is set, call startAnimation()
            if (autoPlay) {
                startAnimation()
            }
        }

        // Handle placeholder replacement
        animationOptions?.placeholderReplacements?.let { replacements ->
            // Clear old placeholder manager first (if exists)
            placeholderManager?.clear()
            placeholderManager = null
            
            val imageLoader = AniFlux.get(view.context).getPlaceholderImageLoader()
            if (imageLoader != null) {
                val lifecycle = getLifecycle()

                placeholderManager = PlaceholderManagerFactory.create(
                    view = view,
                    resource = resource,
                    replacements = replacements,
                    imageLoader = imageLoader,
                    lifecycle = lifecycle
                )

                placeholderManager?.applyReplacements()
            }
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        // Handle SVGA load failure
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // Ignore exceptions during cleanup
        }
        placeholderManager = null
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        // Clear listener
        currentCallback?.let { callback ->
            try {
                view.callback = null
            } catch (e: Exception) {
                // Ignore exceptions during cleanup
            }
        }
        currentAdapter?.onClear()
        currentAdapter = null
        currentCallback = null

        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // Ignore exceptions during cleanup
        }
        placeholderManager = null
        
        clearAnimationFromView()
    }
    
    override fun stopAnimation() {
        // Only stop, don't release resources
        try {
            view.stopAnimation()
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun resumeAnimation() {
        // Resume playback
        try {
            // Check if drawable exists by checking if the view has a SVGADrawable
            val drawable = view.drawable as? SVGADrawable
            if (drawable != null) {
                view.startAnimation()
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun clearAnimationFromView() {
        // Really release resources
        if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
            AniFluxLog.d(AniFluxLogCategory.TARGET, "SVGAViewTarget.clearAnimationFromView() - releasing SVGA resources")
        }
        try {
            view.stopAnimation()
            view.setVideoItem(null)
            if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "SVGAViewTarget.clearAnimationFromView() - resources released successfully")
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "SVGAViewTarget.clearAnimationFromView() - error during cleanup", e)
        }
    }
}

