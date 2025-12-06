package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGView

/**
 * @author: kerneflux
 * @date: 2025/11/2
 * Dedicated Target for PAGView (for PAGView, not PAGImageView)
 */
class PAGViewTarget(view: PAGView) : CustomViewAnimationTarget<PAGView, PAGFile>(view) {

    override fun onResourceReady(resource: PAGFile) {
        // Ensure reusable container cache is set when resource is ready
        onResourceReadyInternal()
        
        // Set listener first (avoid missing onAnimationStart)
        setupPlayListeners(resource, view)

        // Get configuration options
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true
        view.apply {
            // Prevent reuse issues caused by multiple views sharing
            composition = resource.copyOriginal()
            setRepeatCount(repeatCount)
            // If auto play is set, call play()
            if (autoPlay) {
                play()
            }
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        // Handle PAG load failure
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        clearAnimationFromView()
    }
    
    override fun stopAnimation() {
        // Only pause, don't release resources
        try {
            view.pause()
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun resumeAnimation() {
        // Resume playback
        try {
            if (view.composition != null) {
                view.play()
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun clearAnimationFromView() {
        // Really release resources
        if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
            AniFluxLog.d(AniFluxLogCategory.TARGET, "PAGViewTarget.clearAnimationFromView() - releasing PAG resources")
        }
        try {
            view.pause()
            view.flush()
            view.composition = null
            view.progress = 0.0
            view.flush()  // Ensure OpenGL resources are released
            if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "PAGViewTarget.clearAnimationFromView() - resources released successfully")
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "PAGViewTarget.clearAnimationFromView() - error during cleanup", e)
        }
    }
}