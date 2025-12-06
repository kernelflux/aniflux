package com.kernelflux.aniflux.request.listener

/**
 * Unified animation play listener interface
 * Provides various event callbacks during animation playback
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
interface AnimationPlayListener {

    /**
     * Animation starts playing
     * Called when animation first starts playing
     */
    fun onAnimationStart() {
        // Default empty implementation, subclasses can optionally override
    }

    /**
     * Animation playback ends
     * Called when animation completes normally (excluding cancellation)
     */
    fun onAnimationEnd() {
        // Default empty implementation, subclasses can optionally override
    }

    /**
     * Animation playback is cancelled
     * Called when animation playback is cancelled (e.g., calling stop() method)
     */
    fun onAnimationCancel() {
        // Default empty implementation, subclasses can optionally override
    }

    /**
     * Animation playback repeats
     * Called when animation loop restarts
     */
    fun onAnimationRepeat() {
        // Default empty implementation, subclasses can optionally override
    }

    /**
     * Animation playback per frame
     * Called when animation loop refreshes each frame
     */
    fun onAnimationUpdate(currentFrame: Int, totalFrames: Int) {
    }

    /**
     * Animation playback fails
     * Called when an error occurs during animation playback
     *
     * @param error Error information
     */
    fun onAnimationFailed(error: Throwable?) {
        // Default empty implementation, subclasses can optionally override
    }
}

