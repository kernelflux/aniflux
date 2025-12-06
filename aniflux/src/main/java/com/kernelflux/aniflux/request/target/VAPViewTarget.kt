package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
import com.kernelflux.vap.AnimView
import java.io.File

/**
 * Dedicated ViewTarget for VAP animation
 * Automatically handles File resource setup to AnimView
 *
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class VAPViewTarget(view: AnimView) : CustomViewAnimationTarget<AnimView, File>(view) {

    override fun onResourceReady(resource: File) {
        // Ensure reusable container cache is set when resource is ready
        onResourceReadyInternal()
        
        val repeatCount = animationOptions?.repeatCount ?: -1
        val retainLastFrame = animationOptions?.retainLastFrame ?: true
        
        // Set listener first (avoid missing onAnimationStart)
        setupPlayListeners(resource, view)
        view.apply {
            // ✅ Set retainLastFrame configuration
            this.retainLastFrame = retainLastFrame
            
            // ✅ VAP's setLoop semantics analysis (based on HardDecoder.kt:253-277):
            // playLoop = N, each EOS: loop = --playLoop, if loop > 0 then loop
            // playLoop = 2: 1st end loop=1>0 loop, 2nd end loop=0 end → total play 2 times
            // playLoop = 3: 1st end loop=2>0 loop, 2nd end loop=1>0 loop, 3rd end loop=0 end → total play 3 times
            // So setLoop(N) means total play N times, not loop N times!
            // Unified API semantics: repeatCount <= 0 = infinite loop, N = total play N times
            // Conversion: repeatCount(3) → setLoop(3) → total play 3 times
            setLoop(
                when {
                    repeatCount <= 0 -> Int.MAX_VALUE  // Infinite loop
                    else -> repeatCount  // Total play N times → setLoop(N)
                }
            )
            startPlay(resource)
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {

    }

    override fun onResourceCleared(placeholder: Drawable?) {
        clearAnimationFromView()
    }
    
    override fun stopAnimation() {
        // Only stop, don't release resources
        try {
            view.stopPlay()
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun resumeAnimation() {
        // Resume playback
        // Note: VAP doesn't have a direct resume method, need to restart
        // This is a limitation of VAP library
        try {
            // VAP library limitation: no resume method
            // Would need to restart playback with the same file
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
    
    override fun clearAnimationFromView() {
        // Really release resources
        // Note: AnimView doesn't have a release() method
        // stopPlay() should stop the player and release resources internally
        if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
            AniFluxLog.d(AniFluxLogCategory.TARGET, "VAPViewTarget.clearAnimationFromView() - releasing VAP resources")
        }
        try {
            view.stopPlay()
            // VAP library will release resources internally when stopPlay() is called
            if (AniFluxLog.isLoggable(CustomViewAnimationTarget.TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "VAPViewTarget.clearAnimationFromView() - resources released successfully")
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "VAPViewTarget.clearAnimationFromView() - error during cleanup", e)
        }
    }
}

