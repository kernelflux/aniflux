package com.kernelflux.aniflux

import android.content.Context
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.SingleAnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.target.*
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView
import com.kernelflux.pag.PAGView
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.vap.AnimView
import java.io.File

/**
 * Animation request builder
 * Provides a fluent API to build animation loading requests
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
     * Sets the model object to load
     */
    private fun loadWithModel(model: Any?): AnimationRequestBuilder<T> {
        this.model = model
        this.isModelSet = true
        return this
    }

    /**
     * Loads from URL string
     */
    fun load(url: String?): AnimationRequestBuilder<T> {
        return loadWithModel(url)
    }

    /**
     * Loads from Uri
     */
    fun load(uri: android.net.Uri?): AnimationRequestBuilder<T> {
        return loadWithModel(uri)
    }

    /**
     * Loads from file
     */
    fun load(file: java.io.File?): AnimationRequestBuilder<T> {
        return loadWithModel(file)
    }

    /**
     * Loads from resource ID
     */
    fun load(@androidx.annotation.DrawableRes @androidx.annotation.RawRes resourceId: Int?): AnimationRequestBuilder<T> {
        return loadWithModel(resourceId)
    }

    /**
     * Loads from byte array
     */
    fun load(byteArray: ByteArray?): AnimationRequestBuilder<T> {
        return loadWithModel(byteArray)
    }

    // ========== Configuration methods ==========

    /**
     * Sets animation size
     */
    fun size(width: Int, height: Int): AnimationRequestBuilder<T> {
        options.size(width, height)
        return this
    }

    /**
     * Sets cache strategy
     */
    fun cacheStrategy(strategy: AnimationCacheStrategy): AnimationRequestBuilder<T> {
        options.cacheStrategy(strategy)
        return this
    }

    /**
     * Sets animation repeat count
     * @param count -1 for infinite loop, 0 for no loop, >0 for number of loops
     */
    fun repeatCount(count: Int): AnimationRequestBuilder<T> {
        options.repeatCount(count)
        return this
    }

    /**
     * Sets whether to autoplay
     */
    fun autoPlay(auto: Boolean): AnimationRequestBuilder<T> {
        options.autoPlay(auto)
        return this
    }

    /**
     * Sets whether to retain the last frame when animation stops (at the end of animation)
     * @param retain true to retain the frame at the current stop position (stays on current frame), false to clear display (default true)
     *
     * Supported formats:
     * - GIF: Retains frame at current stop position (automatically stays at current frame when animation ends)
     * - Lottie: Retains frame at current stop position (automatically stays at current frame when animation ends)
     * - SVGA: Controlled via fillMode (Forward retains frame at current stop position, Clear clears)
     * - PAG: Retains frame at current stop position (automatically stays at current frame when animation ends)
     * - VAP: Controlled via retainLastFrame configuration (true retains last frame, false clears)
     */
    fun retainLastFrame(retain: Boolean): AnimationRequestBuilder<T> {
        options.retainLastFrame(retain)
        return this
    }
    
    /**
     * Sets placeholder replacement configuration (using DSL)
     * 
     * Supported formats: SVGA, PAG, Lottie
     * 
     * @param builder Builder for placeholder replacement configuration
     * @return this, supports fluent calls
     * 
     * Example:
     * ```
     * .placeholderReplacements {
     *     add("user_1", "https://example.com/user1.jpg")
     *     add("user_2", File("/sdcard/user2.jpg"))
     *     add("logo", R.drawable.logo)
     * }
     * ```
     */
    fun placeholderReplacements(builder: com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap.() -> Unit): AnimationRequestBuilder<T> {
        options.placeholderReplacements(builder)
        return this
    }
    
    /**
     * Sets placeholder replacement configuration (direct pass-in)
     * 
     * Supported formats: SVGA, PAG, Lottie
     * 
     * @param map Placeholder replacement map
     * @return this, supports fluent calls
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
        // Check if model has been set
        if (!isModelSet) {
            throw IllegalArgumentException("You must call #load() before calling #into()")
        }

        // Build AnimationRequest
        val request = buildRequest(target, requestListener)

        // Check for previous request
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
        // Key: For a new request targeting the same target, previous listeners should be cleared to avoid duplicate callbacks
        requestManager.clear(target)

        // Clear previous play listener on target (avoid multiple callbacks)
        // CustomAnimationTarget and CustomViewAnimationTarget both support play listener management
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
     * Get transcoded class type
     */
    private fun getTranscodeClass(): Class<T> {
        return transcodeClass
    }

    /**
     * Get transcodeClass (for type inference)
     * Exposed for extension functions
     */
    internal fun getResourceClass(): Class<*> {
        return transcodeClass
    }


}

// ========== Extension functions: provide more concise API ==========

/**
 * Load File to VAP AnimView
 */
@JvmName("intoPAGImageView")
fun AnimationRequestBuilder<File>.into(view: AnimView): VAPViewTarget {
    val target = VAPViewTarget(view)
    into(target as AnimationTarget<File>)
    return target
}


/**
 * Load PAGFile to PAGImageView
 */
@JvmName("intoPAGImageView")
fun AnimationRequestBuilder<PAGFile>.into(view: PAGImageView): PAGImageViewTarget {
    val target = PAGImageViewTarget(view)
    into(target as AnimationTarget<PAGFile>)
    return target
}

/**
 * Load PAGFile to PAGView
 */
@JvmName("intoPAGView")
fun AnimationRequestBuilder<PAGFile>.into(view: PAGView): PAGViewTarget {
    val target = PAGViewTarget(view)
    into(target as AnimationTarget<PAGFile>)
    return target
}

/**
 * Load LottieDrawable to LottieAnimationView
 */
@JvmName("intoLottieView")
fun AnimationRequestBuilder<LottieDrawable>.into(view: LottieAnimationView): LottieViewTarget {
    val target = LottieViewTarget(view)
    into(target as AnimationTarget<LottieDrawable>)
    return target
}

/**
 * Load SVGADrawable to SVGAImageView
 */
@JvmName("intoSVGAView")
fun AnimationRequestBuilder<SVGADrawable>.into(view: SVGAImageView): SVGAViewTarget {
    val target = SVGAViewTarget(view)
    into(target as AnimationTarget<SVGADrawable>)
    return target
}

/**
 * Load GifDrawable to GifImageView
 */
@JvmName("intoGifView")
fun AnimationRequestBuilder<GifDrawable>.into(view: GifImageView): GifViewTarget {
    val target = GifViewTarget(view)
    into(target as AnimationTarget<GifDrawable>)
    return target
}

/**
 * Load to generic AutoAnimationFrameLayout
 * Automatically creates and displays corresponding animation View based on animation type
 */
fun AnimationRequestBuilder<*>.into(container: android.widget.FrameLayout): AutoAnimationFrameLayoutTarget {
    val target = AutoAnimationFrameLayoutTarget(container)
    @Suppress("UNCHECKED_CAST")
    (this as AnimationRequestBuilder<Any>).into(target)
    return target
}

// ========== Type inference extensions for AnimationRequestBuilder<*> ==========
// When load() cannot detect type, infer resource type based on View type

/**
 * Load to PAGImageView (type inference version)
 * If Builder type is unknown, infer as PAGFile based on View type
 */
fun AnimationRequestBuilder<*>.into(view: PAGImageView): PAGImageViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        PAGFile::class.java -> {
            // Type matches, use directly
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<PAGFile>).into(view)
        }

        Any::class.java -> {
            // Type unknown, infer as PAG based on View type
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asPAG()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "Type mismatch: Builder type is ${builderClass.simpleName}, but target View is PAGImageView (requires PAGFile)\n" +
                        "Please use asPAG().load(...) to explicitly specify type"
            )
        }
    }
}

/**
 * Load to PAGView (type inference version)
 */
fun AnimationRequestBuilder<*>.into(view: PAGView): PAGViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        PAGFile::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<PAGFile>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asPAG()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "Type mismatch: Builder type is ${builderClass.simpleName}, but target View is PAGView (requires PAGFile)\n" +
                        "Please use asPAG().load(...) to explicitly specify type"
            )
        }
    }
}

/**
 * Load to LottieAnimationView (type inference version)
 */
fun AnimationRequestBuilder<*>.into(view: LottieAnimationView): LottieViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        LottieDrawable::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<LottieDrawable>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asLottie()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "Type mismatch: Builder type is ${builderClass.simpleName}, but target View is LottieAnimationView (requires LottieDrawable)\n" +
                        "Please use asLottie().load(...) to explicitly specify type"
            )
        }
    }
}

/**
 * Load to SVGAImageView (type inference version)
 */
fun AnimationRequestBuilder<*>.into(view: SVGAImageView): SVGAViewTarget {
    val builderClass = getBuilderTranscodeClass(this)

    return when (builderClass) {
        SVGADrawable::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<SVGADrawable>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asSVGA()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "Type mismatch: Builder type is ${builderClass.simpleName}, but target View is SVGAImageView (requires SVGADrawable)\n" +
                        "Please use asSVGA().load(...) to explicitly specify type"
            )
        }
    }
}

/**
 * Load to GifImageView (type inference version)
 */
fun AnimationRequestBuilder<*>.into(view: GifImageView): GifViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        GifDrawable::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<GifDrawable>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asGif()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "Type mismatch: Builder type is ${builderClass.simpleName}, but target View is GifImageView (requires GifDrawable)\n" +
                        "Please use asGif().load(...) to explicitly specify type"
            )
        }
    }
}

/**
 * Load to AnimView (type inference version)
 */
fun AnimationRequestBuilder<*>.into(view: AnimView): VAPViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        File::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<File>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asGif()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "Type mismatch: Builder type is ${builderClass.simpleName}, but target View is AnimView (requires File)\n" +
                        "Please use asFile().load(...) to explicitly specify type"
            )
        }
    }
}


/**
 * Get Builder's transcodeClass
 */
private fun getBuilderTranscodeClass(builder: AnimationRequestBuilder<*>): Class<*> {
    return builder.getResourceClass()
}

/**
 * Get AnimationRequestManager from Builder (via reflection)
 */
private fun getRequestManagerFromBuilder(builder: AnimationRequestBuilder<*>): AnimationRequestManager {
    return try {
        val field = AnimationRequestBuilder::class.java.getDeclaredField("requestManager")
        field.isAccessible = true
        field.get(builder) as AnimationRequestManager
    } catch (e: Exception) {
        throw IllegalStateException("Cannot get AnimationRequestManager", e)
    }
}

/**
 * Copy Builder's configuration options (options, listeners, etc.)
 */
private fun copyBuilderOptions(
    target: AnimationRequestBuilder<*>,
    source: AnimationRequestBuilder<*>
) {
    try {
        // Copy options
        val optionsField = AnimationRequestBuilder::class.java.getDeclaredField("options")
        optionsField.isAccessible = true
        val sourceOptions = optionsField.get(source) as AnimationOptions
        optionsField.set(target, sourceOptions)

        // Copy playListener
        val playListenerField = AnimationRequestBuilder::class.java.getDeclaredField("playListener")
        playListenerField.isAccessible = true
        val playListener = playListenerField.get(source)
        playListenerField.set(target, playListener)

        // Copy requestListener
        val requestListenerField =
            AnimationRequestBuilder::class.java.getDeclaredField("requestListener")
        requestListenerField.isAccessible = true
        val requestListener = requestListenerField.get(source)
        requestListenerField.set(target, requestListener)

        // Copy model
        val modelField = AnimationRequestBuilder::class.java.getDeclaredField("model")
        modelField.isAccessible = true
        val model = modelField.get(source)
        modelField.set(target, model)

        // Copy isModelSet
        val isModelSetField = AnimationRequestBuilder::class.java.getDeclaredField("isModelSet")
        isModelSetField.isAccessible = true
        val isModelSet = isModelSetField.get(source)
        isModelSetField.set(target, isModelSet)
    } catch (e: Exception) {
        com.kernelflux.aniflux.log.AniFluxLog.w(com.kernelflux.aniflux.log.AniFluxLogCategory.GENERAL, "Failed to copy builder options", e)
        // Continue execution even if copy fails
    }
}