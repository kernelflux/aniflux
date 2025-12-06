package com.kernelflux.aniflux

import android.content.Context
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.SingleAnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.cache.AnimationCacheStrategy

/**
 * Animation request builder
 * Provides chain API to build animation loading requests
 * 
 * This is the core version, doesn't contain format-specific code
 * Format-specific extension functions should be provided in their respective format modules
 *
 * @author: kerneflux
 * @date: 2025/10/13
 */
class AnimationRequestBuilder<T>(
    private val aniFlux: AniFlux,
    private val requestManager: AnimationRequestManager,
    private val context: Context,
    private val transcodeClass: Class<T>
) {

    private var model: Any? = null
    private var isModelSet = false
    private var options: AnimationOptions = AnimationOptions.create()
    private var playListener: AnimationPlayListener? = null
    private var requestListener: AnimationRequestListener<T>? = null

    private fun isSkipMemoryCacheWithCompletePreviousRequest(previous: AnimationRequest): Boolean {
        return previous.isComplete()
    }

    /**
     * Set the model object to load
     */
    private fun loadWithModel(model: Any?): AnimationRequestBuilder<T> {
        this.model = model
        this.isModelSet = true
        return this
    }

    /**
     * Load from URL string
     */
    fun load(url: String?): AnimationRequestBuilder<T> {
        return loadWithModel(url)
    }

    /**
     * Load from Uri
     */
    fun load(uri: android.net.Uri?): AnimationRequestBuilder<T> {
        return loadWithModel(uri)
    }

    /**
     * Load from file
     */
    fun load(file: java.io.File?): AnimationRequestBuilder<T> {
        return loadWithModel(file)
    }

    /**
     * Load from resource ID
     */
    fun load(@androidx.annotation.DrawableRes @androidx.annotation.RawRes resourceId: Int?): AnimationRequestBuilder<T> {
        return loadWithModel(resourceId)
    }

    /**
     * Load from byte array
     */
    fun load(byteArray: ByteArray?): AnimationRequestBuilder<T> {
        return loadWithModel(byteArray)
    }

    // ========== Configuration methods ==========

    /**
     * Set animation size
     */
    fun size(width: Int, height: Int): AnimationRequestBuilder<T> {
        options.size(width, height)
        return this
    }

    /**
     * Set cache strategy
     */
    fun cacheStrategy(strategy: AnimationCacheStrategy): AnimationRequestBuilder<T> {
        options.cacheStrategy(strategy)
        return this
    }

    /**
     * Set animation repeat count
     * @param count -1 means infinite loop, 0 means no loop, >0 means loop count
     */
    fun repeatCount(count: Int): AnimationRequestBuilder<T> {
        options.repeatCount(count)
        return this
    }

    /**
     * Set whether to auto play
     */
    fun autoPlay(auto: Boolean): AnimationRequestBuilder<T> {
        options.autoPlay(auto)
        return this
    }

    /**
     * Set whether to retain the frame when animation stops (at end)
     * @param retain true means retain current stopped frame (stay at current frame), false means clear display (default true)
     */
    fun retainLastFrame(retain: Boolean): AnimationRequestBuilder<T> {
        options.retainLastFrame(retain)
        return this
    }
    
    /**
     * Set placeholder replacement configuration (using DSL)
     * 
     * Supported formats: SVGA, PAG, Lottie
     * 
     * @param builder Placeholder replacement configuration builder
     * @return this, supports chain calls
     */
    fun placeholderReplacements(builder: com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap.() -> Unit): AnimationRequestBuilder<T> {
        options.placeholderReplacements(builder)
        return this
    }
    
    /**
     * Set placeholder replacement configuration (direct pass)
     * 
     * Supported formats: SVGA, PAG, Lottie
     * 
     * @param map Placeholder replacement map
     * @return this, supports chain calls
     */
    fun placeholderReplacements(map: com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap): AnimationRequestBuilder<T> {
        options.placeholderReplacements(map)
        return this
    }

    /**
     * Apply custom configuration options
     */
    fun apply(customOptions: AnimationOptions): AnimationRequestBuilder<T> {
        // Merge configuration options
        options = customOptions
        return this
    }

    fun playListener(listener: AnimationPlayListener?): AnimationRequestBuilder<T> {
        playListener = listener
        return this
    }

    fun requestListener(listener: AnimationRequestListener<T>?): AnimationRequestBuilder<T> {
        requestListener = listener
        return this
    }

    fun <Y : AnimationTarget<T>> into(target: Y): Y {
        return into(target, requestListener, playListener)
    }


    fun <Y : AnimationTarget<T>> into(
        target: Y,
        requestListener: AnimationRequestListener<T>? = null,
        playListener: AnimationPlayListener? = null
    ): Y {
        // Check if model is set
        if (!isModelSet) {
            throw IllegalArgumentException("You must call #load() before calling #into()")
        }

        // Build AnimationRequest
        val request = buildRequest(target, requestListener)

        // Check if there's a previous request
        val previousRequest = target.getRequest()
        if (request.isEquivalentTo(previousRequest) &&
            previousRequest != null &&
            !isSkipMemoryCacheWithCompletePreviousRequest(previousRequest)
        ) {
            // If request is the same and previous request is not complete, reuse previous request
            if (!previousRequest.isRunning()) {
                previousRequest.begin()
            }
            return target
        }

        // Clear previous request and set new request
        // Key: For new request on the same target, should clear previous listeners to avoid duplicate callbacks
        requestManager.clear(target)

        // Clear previous play listeners on target (avoid multiple callbacks)
        // Both CustomAnimationTarget and CustomViewAnimationTarget support play listener management
        when (target) {
            is CustomAnimationTarget<*> -> {
                target.clearPlayListener()
                // Add new listener (if any)
                playListener?.let { listener ->
                    target.setPlayListener(listener)
                }
            }

            is CustomViewAnimationTarget<*, *> -> {
                target.clearPlayListener()
                // Add new listener (if any)
                playListener?.let { listener ->
                    target.addPlayListener(listener)
                }
            }
        }

        target.setRequest(request)
        requestManager.track(target, request)

        return target
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildRequest(
        target: AnimationTarget<*>,
        requestListener: AnimationRequestListener<T>? = null
    ): AnimationRequest {
        return SingleAnimationRequest(
            context = context,
            requestLock = Any(),
            model = model,
            target = target as AnimationTarget<T>,
            requestListener = requestListener,
            transcodeClass = getTranscodeClass(),
            overrideWidth = options.width,
            overrideHeight = options.height,
            engine = aniFlux.getEngine(),
            options = options
        )
    }

    /**
     * Get transcode class
     */
    private fun getTranscodeClass(): Class<T> {
        return transcodeClass
    }

    /**
     * Get transcodeClass (for type inference)
     * Exposed for use by extension functions
     */
    fun getResourceClass(): Class<*> {
        return transcodeClass
    }
}

