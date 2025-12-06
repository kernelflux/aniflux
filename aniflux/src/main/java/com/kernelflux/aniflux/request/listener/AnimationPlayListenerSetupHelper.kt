package com.kernelflux.aniflux.request.listener

import android.annotation.SuppressLint
import android.view.View
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.gif.AnimationListener
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView
import com.kernelflux.pag.PAGView
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.vap.AnimView
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Animation play listener setup helper class
 * Responsible for setting unified listeners to specific animation objects
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
object AnimationPlayListenerSetupHelper {
    private const val TAG = "AnimationPlayListenerSetupHelper"
    private val adapterCache = ConcurrentHashMap<String, Pair<View?, List<Any>>>()

    /**
     * Set play listener for animation resource
     *
     * @param target Target object
     * @param resource Animation resource
     * @param view View displaying animation (optional)
     */
    @SuppressLint("LongLogTag")
    @Suppress("UNCHECKED_CAST")
    fun setupListeners(
        target: Any,
        resource: Any,
        view: View? = null
    ) {
        // Get listener directly (supports CustomAnimationTarget and CustomViewAnimationTarget)
        val listener = when (target) {
            is CustomAnimationTarget<*> -> target.playListener
            is CustomViewAnimationTarget<*, *> -> target.playListener
            else -> {
                AniFluxLog.w(
                    AniFluxLogCategory.GENERAL,
                    "setupListeners: target type not supported: ${target.javaClass.simpleName}"
                )
                return
            }
        }
        if (listener == null) {
            // No listener, no need to setup
            AniFluxLog.d(AniFluxLogCategory.GENERAL, "setupListeners: no listener to setup")
            return
        }

AniFluxLog.d(
                    AniFluxLogCategory.GENERAL,
            "setupListeners: resource=${resource.javaClass.simpleName}, view=${view?.javaClass?.simpleName}"
        )

        // Key: unified use View as basis (if view exists)
        // Because the same View may be used multiple times with different targets (each into() creates a new target)
        // Only when there's truly no View scenario (like custom CustomAnimationTarget implementation), use target as key
        // No View parameter, try to get View from target
        val targetKey = view?.hashCode()?.toString() ?: when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                // ViewTarget has internal View, use it
                target.getViewForVisibilityCheck().hashCode().toString()
            }

            is CustomAnimationTarget<*> -> {
                // True CustomAnimationTarget implementation (no View), use target as key
                target.hashCode().toString()
            }

            else -> target.hashCode().toString()
        }

AniFluxLog.d(
                    AniFluxLogCategory.GENERAL,
            "setupListeners: targetKey=$targetKey (based on ${if (view != null) "view param" else "target/view from target"})"
        )

        // Set listener based on resource type
        when (resource) {
            is PAGFile -> {
                setupPAGListeners(targetKey, view, listener, target)
            }

            is LottieDrawable -> {
                setupLottieListeners(targetKey, resource, view, listener, target)
            }

            is SVGADrawable -> {
                setupSVGAListeners(targetKey, view, listener)
            }

            is GifDrawable -> {
                setupGifListeners(targetKey, resource, view, listener, target)
            }

            is File -> {
                if (view is AnimView) {
                    setupVAPListeners(targetKey, view, listener, target)
                }
            }
        }
    }

    /**
     * Set PAG animation listener
     */
    @SuppressLint("LongLogTag")
    private fun setupPAGListeners(
        targetKey: String,
        view: View?,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // Remove old adapter (if exists) - must actually remove from View
        removeOldPAGAdapter(targetKey, view)

        if (view == null) {
            AniFluxLog.w(AniFluxLogCategory.GENERAL, "PAG listener setup failed: view is null")
            return
        }

        // Get retainLastFrame configuration from target
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // Default retain
        }

        when (view) {
            is PAGView -> {
                val adapter = PAGViewPlayListenerAdapter(listener, view, retainLastFrame)
                val pagListener = adapter.createAnimatorListener()
                view.addListener(pagListener)
                adapterCache[targetKey] = Pair(view, listOf(pagListener))
                AniFluxLog.d(AniFluxLogCategory.GENERAL, "PAGView listener set")
            }

            is PAGImageView -> {
                val adapter = PAGImageViewPlayListenerAdapter(listener, view, retainLastFrame)
                val pagListener = adapter.createAnimatorListener()
                view.addListener(pagListener)
                adapterCache[targetKey] = Pair(view, listOf(pagListener))
                AniFluxLog.d(AniFluxLogCategory.GENERAL, "PAGImageView listener set")
            }

            else -> {
                AniFluxLog.w(
                    AniFluxLogCategory.GENERAL,
                    "PAG listener setup failed: view is not PAGView or PAGImageView, type=${view.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * Remove old PAG listener (actually remove from View)
     * Key: if currentView is not null, remove directly from currentView, because targetKey is based on View
     */
    @SuppressLint("LongLogTag")
    private fun removeOldPAGAdapter(targetKey: String, currentView: View?) {
        val cached = adapterCache.remove(targetKey)
        if (cached != null && currentView != null) {
            val (_, listeners) = cached
            listeners.forEach { listener ->
                when {
                    listener is PAGView.PAGViewListener && currentView is PAGView -> {
                        // Remove old listener from current View (prevent duplicate addition)
                        currentView.removeListener(listener)
                        AniFluxLog.d(
                            AniFluxLogCategory.GENERAL,
                            "Removed PAGView listener from current view (key=$targetKey)"
                        )
                    }

                    listener is PAGImageView.PAGImageViewListener && currentView is PAGImageView -> {
                        // Remove old listener from current View (prevent duplicate addition)
                        currentView.removeListener(listener)
                        AniFluxLog.d(
                            AniFluxLogCategory.GENERAL,
                            "Removed PAGImageView listener from current view (key=$targetKey)"
                        )
                    }
                }
            }
        } else if (cached != null) {
            // If no currentView, remove from oldView (cleanup scenario)
            val (oldView, listeners) = cached
            listeners.forEach { listener ->
                when {
                    listener is PAGView.PAGViewListener && oldView is PAGView -> {
                        oldView.removeListener(listener)
                        AniFluxLog.d(AniFluxLogCategory.GENERAL, "Removed PAGView listener from old view")
                    }

                    listener is PAGImageView.PAGImageViewListener && oldView is PAGImageView -> {
                        oldView.removeListener(listener)
                        AniFluxLog.d(AniFluxLogCategory.GENERAL, "Removed PAGImageView listener from old view")
                    }
                }
            }
        }
    }

    /**
     * Set Lottie animation listener
     */
    private fun setupLottieListeners(
        targetKey: String,
        lottieDrawable: LottieDrawable,
        view: View?,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // Remove old adapter (if exists)
        removeOldLottieAdapter(targetKey, view, lottieDrawable)

        // Get retainLastFrame configuration from target
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // Default retain
        }

        val lottieView = view as? LottieAnimationView
        val adapter = LottiePlayListenerAdapter(listener, lottieView, retainLastFrame)
        
        // Get user's original repeatCount (total play count)
        val userRepeatCount = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.repeatCount ?: -1
            }
            else -> -1  // Default infinite loop
        }
        
        // Pass to adapter: user's expected total play count
        // Note: LottieViewTarget will convert user's repeatCount to Lottie's semantics
        // But adapter needs to know user's expected total play count, not Lottie's repeatCount
        val animatorListener = adapter.createAnimatorListener(userRepeatCount)

        if (lottieView != null) {
            lottieView.addAnimatorListener(animatorListener)
        } else {
            lottieDrawable.addAnimatorListener(animatorListener)
        }

        adapterCache[targetKey] = Pair(view, listOf(animatorListener))
    }

    /**
     * Remove old Lottie listener
     */
    private fun removeOldLottieAdapter(
        targetKey: String,
        currentView: View?,
        currentDrawable: LottieDrawable
    ) {
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            val (oldView, listeners) = cached
            listeners.forEach { listener ->
                if (listener is android.animation.Animator.AnimatorListener) {
                    // Remove from old View or Drawable
                    when {
                        oldView is LottieAnimationView -> {
                            oldView.removeAnimatorListener(listener)
                        }
                        // If old and new View are the same, also need to remove (prevent duplicate addition)
                        currentView is LottieAnimationView && oldView == currentView -> {
                            currentView.removeAnimatorListener(listener)
                        }

                        else -> {
                            // Try to remove from Drawable
                            try {
                                currentDrawable.removeAnimatorListener(listener)
                            } catch (e: Exception) {
                                // Drawable may not support removal, ignore
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Set SVGA animation listener
     * Note: SVGA internally uses Animator, need to get internal Animator via reflection to add listener
     */
    @SuppressLint("LongLogTag")
    private fun setupSVGAListeners(
        targetKey: String,
        view: View?,
        listener: AnimationPlayListener
    ) {
        if (view !is SVGAImageView) {
            return
        }
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            view.callback = null
        }
        val adapter = SVGAPlayListenerAdapter(listener)
        val animationListener = adapter.createAnimatorListener()
        view.callback = animationListener
        adapterCache[targetKey] = Pair(null, listOf(animationListener))
    }


    /**
     * Set VAP animation listener
     */
    private fun setupVAPListeners(
        targetKey: String,
        view: AnimView,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // Remove old adapter (if exists)
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            view.setAnimListener(null)
        }

        // Get retainLastFrame configuration from target
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // Default retain
        }

        val adapter = VapPlayListenerAdapter(listener, view, retainLastFrame)
        val animationListener = adapter.createAnimatorListener()
        view.setAnimListener(animationListener)
        adapterCache[targetKey] = Pair(null, listOf(animationListener))
    }


    /**
     * Set GIF animation listener
     */
    private fun setupGifListeners(
        targetKey: String,
        gifDrawable: GifDrawable,
        view: View?,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // Remove old adapter (if exists)
        removeOldGifAdapter(targetKey, gifDrawable)

        // Get retainLastFrame configuration from target
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // Default retain
        }

        val adapter =
            GifPlayListenerAdapter(listener, view as? GifImageView, retainLastFrame)
        val animationListener = adapter.createAnimatorListener(gifDrawable.loopCount)
        gifDrawable.addAnimationListener(animationListener)

        adapterCache[targetKey] = Pair(null, listOf(animationListener))
    }

    /**
     * Remove old GIF listener
     */
    private fun removeOldGifAdapter(targetKey: String, currentDrawable: GifDrawable) {
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            val (_, listeners) = cached
            listeners.forEach { listener ->
                if (listener is AnimationListener) {
                    try {
                        currentDrawable.removeAnimationListener(listener)
                    } catch (e: Exception) {
                        // Ignore removal failure
                    }
                }
            }
        }
    }


    /**
     * Cleanup adapter for specified target
     * Need to actually remove listener from View/Drawable, not just remove from cache
     */
    fun cleanup(target: Any) {
        val targetKey = getTargetKey(target)
        val cached = adapterCache.remove(targetKey)

        // Remove listener from actual View/Drawable
        if (cached != null) {
            val (view, listeners) = cached
            listeners.forEach { listener ->
                when {
                    // PAG listener
                    listener is PAGView.PAGViewListener && view is PAGView -> {
                        view.removeListener(listener)
                    }

                    listener is PAGImageView.PAGImageViewListener && view is PAGImageView -> {
                        view.removeListener(listener)
                    }
                    // Lottie listener
                    listener is android.animation.Animator.AnimatorListener && view is LottieAnimationView -> {
                        view.removeAnimatorListener(listener)
                    }
                    // SVGA listener (needs reflection)
                    listener is android.animation.Animator.AnimatorListener && view is SVGAImageView -> {
                        try {
                            val animatorField =
                                SVGAImageView::class.java.getDeclaredField("mAnimator")
                            animatorField.isAccessible = true
                            val animator =
                                animatorField.get(view) as? android.animation.ValueAnimator
                            animator?.removeListener(listener)
                        } catch (e: Exception) {
                            // Ignore reflection failure
                        }
                    }
                }
            }
        }
    }

    /**
     * Get unique identifier for target (for cleanup and other scenarios)
     * Prefer View, only use target if no View
     */
    private fun getTargetKey(target: Any): String {
        return when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                // ViewTarget always has View, use View as key
                target.getViewForVisibilityCheck().hashCode().toString()
            }

            is CustomAnimationTarget<*> -> {
                // CustomAnimationTarget: if AutoAnimationFrameLayoutTarget, try to get container
                // Other cases use target as key (true custom implementation)
                try {
                    val field = target.javaClass.getDeclaredField("container")
                    field.isAccessible = true
                    val container = field.get(target)
                    if (container is View) {
                        container.hashCode().toString()
                    } else {
                        target.hashCode().toString()
                    }
                } catch (e: Exception) {
                    // No container field, use target as key
                    target.hashCode().toString()
                }
            }

            else -> target.hashCode().toString()
        }
    }

}

