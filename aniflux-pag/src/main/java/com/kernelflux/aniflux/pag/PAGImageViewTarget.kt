package com.kernelflux.aniflux.pag

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
import android.view.View
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView

/**
 * Dedicated ViewTarget for PAG animation
 * Automatically handles PAGFile resource setup to PAGImageView/PAGView
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
@SuppressLint("LongLogTag")
class PAGImageViewTarget(view: PAGImageView) : CustomViewAnimationTarget<PAGImageView, PAGFile>(view) {
    
    private var placeholderManager: PlaceholderManager? = null
    private var currentAdapter: PAGImageViewPlayListenerAdapter? = null
    private var currentListener: PAGImageView.PAGImageViewListener? = null
    
    override fun setupPlayListeners(resource: Any, view: View?) {
        val pagImageView = view as? PAGImageView ?: return
        val listener = playListener ?: return
        
        // Remove old listener
        currentListener?.let { oldListener ->
            try {
                pagImageView.removeListener(oldListener)
            } catch (e: Exception) {
                // Ignore exceptions when removing
            }
        }
        
        // Get retainLastFrame configuration
        val retainLastFrame = animationOptions?.retainLastFrame ?: true
        
        // Create new adapter
        val adapter = PAGImageViewPlayListenerAdapter(listener, pagImageView, retainLastFrame)
        val pagListener = adapter.createAnimatorListener()
        pagImageView.addListener(pagListener)
        
        // Save reference for cleanup
        currentAdapter = adapter
        currentListener = pagListener
    }
    
    override fun onResourceReady(resource: PAGFile) {
        // Set listener first (avoid missing onAnimationStart)
        setupPlayListeners(resource, view)
        
        // Ensure animation compatibility fix is applied before playing
        // This is critical for PAG animations when system animations are disabled
        // Note: PAG source code has been modified to automatically detect if ValueAnimator.durationScale
        // has been fixed, so we only need to ensure the fix is applied here
        val context = view.context
        if (context != null) {
            com.kernelflux.aniflux.util.AnimationCompatibilityHelper.ensureValueAnimatorCompatibility(
                context.contentResolver
            )
        }
        
        // Get configuration options
        val repeatCount = animationOptions?.repeatCount ?: 0
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
        // Handle PAG load failure
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // Ignore exceptions during cleanup
        }
        placeholderManager = null
    }
    
    override fun onResourceCleared(placeholder: Drawable?) {
        // Clear listener
        currentListener?.let { listener ->
            try {
                view.removeListener(listener)
            } catch (e: Exception) {
                // Ignore exceptions during cleanup
            }
        }
        currentAdapter?.onClear()
        currentAdapter = null
        currentListener = null
        
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // Ignore exceptions during cleanup
        }
        placeholderManager = null
        
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
                // Ensure animation compatibility fix is applied before playing
                // This is critical for PAG animations when system animations are disabled
                // Note: PAG source code has been modified to automatically detect if ValueAnimator.durationScale
                // has been fixed, so we only need to ensure the fix is applied here
                val context = view.context
                if (context != null) {
                    com.kernelflux.aniflux.util.AnimationCompatibilityHelper.ensureValueAnimatorCompatibility(
                        context.contentResolver
                    )
                }
                view.play()
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    @SuppressLint("Range")
    override fun clearAnimationFromView() {
        // Really release resources
        if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
            AniFluxLog.d(AniFluxLogCategory.TARGET, "PAGImageViewTarget.clearAnimationFromView() - releasing PAG resources")
        }
        try {
            view.pause()
            view.composition = null
            if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "PAGImageViewTarget.clearAnimationFromView() - resources released successfully")
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "PAGImageViewTarget.clearAnimationFromView() - error during cleanup", e)
        }
    }
}

