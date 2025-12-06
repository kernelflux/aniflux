package com.kernelflux.aniflux.gif

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView

/**
 * Dedicated ViewTarget for GIF animation
 * Automatically handles GifDrawable resource setup to GifImageView
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class GifViewTarget(view: GifImageView) : CustomViewAnimationTarget<GifImageView, GifDrawable>(view) {
    
    override fun onResourceReady(resource: GifDrawable) {
        // Get configuration options
        val repeatCount = animationOptions?.repeatCount ?: -1
        // ✅ GIF's loopCount semantics: 0=infinite loop, N=play N times (total play count)
        // Unified API semantics: repeatCount <= 0 = infinite loop, N = play N times total
        resource.loopCount = when {
            repeatCount <= 0 -> 0  // Infinite loop
            else -> repeatCount     // Total play count
        }
        // Set listener first (avoid missing onAnimationStart)
        setupPlayListeners(resource, view)
        // Set drawable (GIF will automatically start playing)
        view.setImageDrawable(resource)
    }
    
    override fun onLoadFailed(errorDrawable: Drawable?) {
        // Handle GIF load failure
        view.setImageDrawable(errorDrawable)
    }
    
    override fun onResourceCleared(placeholder: Drawable?) {
        clearAnimationFromView()
        view.setImageDrawable(placeholder)
    }
    
    override fun stopAnimation() {
        // Only stop, don't release resources
        try {
            val drawable = view.drawable
            if (drawable is GifDrawable) {
                drawable.stop()
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun resumeAnimation() {
        // Resume playback
        try {
            val drawable = view.drawable
            if (drawable is GifDrawable) {
                drawable.start()
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun clearAnimationFromView() {
        // Really release resources
        if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
            AniFluxLog.d(AniFluxLogCategory.TARGET, "GifViewTarget.clearAnimationFromView() - releasing GIF resources")
        }
        try {
            val drawable = view.drawable
            if (drawable is GifDrawable) {
                drawable.stop()
                drawable.recycle()
                if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
                    AniFluxLog.d(AniFluxLogCategory.TARGET, "GifViewTarget.clearAnimationFromView() - resources released successfully")
                }
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "GifViewTarget.clearAnimationFromView() - error during cleanup", e)
        }
    }
}

