package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager

/**
 * Dedicated ViewTarget for Lottie animation
 * Automatically handles LottieDrawable resource setup to LottieAnimationView
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
class LottieViewTarget(view: LottieAnimationView) :
    CustomViewAnimationTarget<LottieAnimationView, LottieDrawable>(view) {

    private var placeholderManager: PlaceholderManager? = null

    override fun onResourceReady(resource: LottieDrawable) {
        // Ensure reusable container cache is set when resource is ready
        onResourceReadyInternal()
        
        // Set listener first (avoid missing onAnimationStart)
        setupPlayListeners(resource, view)

        // Get configuration options
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true

        view.apply {
            // ✅ Compatibility: Ignore disabled system animations to ensure Lottie animations work
            // even when system animations are disabled in developer options
            // IMPORTANT: Set this BEFORE setComposition() because setComposition() may trigger autoPlay
            try {
                @Suppress("DEPRECATION")
                setIgnoreDisabledSystemAnimations(true)
            } catch (e: Exception) {
                AniFluxLog.w(
                    AniFluxLogCategory.TARGET,
                    "Failed to set ignoreDisabledSystemAnimations for Lottie (may not be available in this Lottie version)",
                    e
                )
            }
            
            resource.composition?.let { setComposition(it) }
            this.repeatCount = when {
                repeatCount < 0 -> LottieDrawable.INFINITE  // -1
                repeatCount <= 1 -> 0  // Play once (no repeat)
                else -> repeatCount - 1  // Total count N → repeat count N-1
            }

            // If auto play is set, call playAnimation()
            // Note: setComposition() may have already triggered playAnimation() if autoPlay is true
            // But we call it again here to ensure it plays (in case autoPlay was false in setComposition)
            if (autoPlay) {
                playAnimation()
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
                
                placeholderManager = PlaceholderManager.create(
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
        // Handle Lottie load failure
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // Ignore exceptions during cleanup
        }
        placeholderManager = null
    }

    override fun onResourceCleared(placeholder: Drawable?) {
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
            view.pauseAnimation()
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun resumeAnimation() {
        // Resume playback
        try {
            if (view.composition != null) {
                // Ensure ignoreDisabledSystemAnimations is set before resuming
                // This is critical for Lottie animations when system animations are disabled
                try {
                    @Suppress("DEPRECATION")
                    view.setIgnoreDisabledSystemAnimations(true)
                } catch (e: Exception) {
                    AniFluxLog.w(
                        AniFluxLogCategory.TARGET,
                        "Failed to set ignoreDisabledSystemAnimations for Lottie (may not be available in this Lottie version)",
                        e
                    )
                }
                view.resumeAnimation()
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun clearAnimationFromView() {
        // Really release resources
        if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
            AniFluxLog.d(AniFluxLogCategory.TARGET, "LottieViewTarget.clearAnimationFromView() - releasing Lottie resources")
        }
        try {
            view.cancelAnimation()
            view.setImageDrawable(null)
            if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "LottieViewTarget.clearAnimationFromView() - resources released successfully")
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "LottieViewTarget.clearAnimationFromView() - error during cleanup", e)
        }
    }
}

