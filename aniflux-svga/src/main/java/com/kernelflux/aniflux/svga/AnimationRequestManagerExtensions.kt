package com.kernelflux.aniflux.svga

import androidx.annotation.CheckResult
import com.kernelflux.aniflux.AnimationRequestBuilder
import com.kernelflux.aniflux.AnimationRequestManager
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView

/**
 * AnimationRequestManager extension functions for SVGA format
 * 
 * @author: kernelflux
 * @date: 2025/01/XX
 */

/**
 * Specify loading SVGA animation
 */
@CheckResult
fun AnimationRequestManager.asSVGA(): AnimationRequestBuilder<SVGADrawable> {
    return AnimationRequestBuilder(aniFlux, this, context, SVGADrawable::class.java)
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
 * Load to SVGAImageView (type inference version)
 * If Builder type is unknown, infer as SVGADrawable based on View type
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
        throw IllegalStateException("Unable to get AnimationRequestManager", e)
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
        val sourceOptions = optionsField.get(source) as com.kernelflux.aniflux.util.AnimationOptions
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

