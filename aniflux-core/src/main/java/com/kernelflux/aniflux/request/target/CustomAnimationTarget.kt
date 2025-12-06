package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
// AnimationPlayListenerSetupHelper is in aniflux module, format-specific cleanup is handled by format modules
import com.kernelflux.aniflux.util.Util

/**
 * @author: kerneflux
 * @date: 2025/10/13
 *
 */
abstract class CustomAnimationTarget<T>(
    width: Int = AnimationTarget.SIZE_ORIGINAL,
    height: Int = AnimationTarget.SIZE_ORIGINAL
) : AnimationTarget<T> {
    protected val width: Int
    protected val height: Int
    private var request: AnimationRequest? = null
    
    // Animation play listener (directly held, no Manager wrapper needed)
    @Volatile
    var playListener: AnimationPlayListener? = null
        private set
    
    // Animation configuration options (for playback settings)
    @Volatile
    var animationOptions: com.kernelflux.aniflux.util.AnimationOptions? = null
        internal set

    init {
        if (!Util.isValidDimensions(width, height)) {
            throw IllegalArgumentException(
                ("Width and height must both be > 0 or Target#SIZE_ORIGINAL, but given"
                        + " width: "
                        + width
                        + " and height: "
                        + height)
            )
        }
        this.width = width
        this.height = height
    }

    override fun onStart() {
        //
    }

    override fun onStop() {
        //
    }

    override fun onDestroy() {
        // Clear listeners
        cleanupPlayListeners()
    }

    override fun onLoadStarted(placeholder: Drawable?) {
        //
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        //
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        // Clear listener setup
        cleanupPlayListeners()
    }

    override fun getSize(cb: AnimationSizeReadyCallback) {
        cb.onSizeReady(width, height)
    }

    override fun removeCallback(cb: AnimationSizeReadyCallback) {
        //
    }

    override fun setRequest(request: AnimationRequest?) {
        this.request = request
    }

    override fun getRequest(): AnimationRequest? {
        return this.request
    }
    
    /**
     * Get target width
     * Uses width() method name to avoid Kotlin get method conflicts
     */
    fun width(): Int {
        return width
    }
    
    /**
     * Get target height
     * Uses height() method name to avoid Kotlin get method conflicts
     */
    fun height(): Int {
        return height
    }


    fun setPlayListener(listener: AnimationPlayListener?): Boolean {
        if (listener == null) return false
        playListener = listener
        return true
    }

    /**
     * Clear listener
     */
    fun clearPlayListener() {
        playListener = null
    }

    /**
     * Cleanup listener setup
     * Automatically called in onLoadCleared, also called in onDestroy
     * 
     * Note: Specific cleanup logic is in aniflux module's AnimationPlayListenerSetupHelper
     * Here only does basic cleanup, format-specific cleanup is handled by format modules
     */
    internal fun cleanupPlayListeners() {
        // Basic cleanup: clear listener reference
        // Format-specific cleanup (e.g., remove animation listeners) is handled by format modules' AnimationPlayListenerSetupHelper
        playListener = null
    }
}
