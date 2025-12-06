package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerSetupHelper
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.vap.AnimView

/**
 * Universal animation container FrameLayout Target
 * Automatically creates and displays corresponding animation View based on animation type
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class AutoAnimationFrameLayoutTarget(
    private val container: FrameLayout
) : CustomAnimationTarget<Any>() {
    
    private var currentAnimationView: View? = null
    private var currentAnimationType: AnimationTypeDetector.AnimationType? = null
    
    /**
     * Create corresponding View based on animation type
     */
    private fun createAnimationView(type: AnimationTypeDetector.AnimationType): View {
        val context = container.context
        
        return when (type) {
            AnimationTypeDetector.AnimationType.PAG -> {
                PAGImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
            AnimationTypeDetector.AnimationType.LOTTIE -> {
                LottieAnimationView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
            AnimationTypeDetector.AnimationType.SVGA -> {
                SVGAImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
            AnimationTypeDetector.AnimationType.GIF -> {
                GifImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
            AnimationTypeDetector.AnimationType.VAP -> {
                AnimView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }

            AnimationTypeDetector.AnimationType.UNKNOWN -> {
                throw IllegalArgumentException("Unknown animation type")
            }
        }
    }
    
    /**
     * Show specified animation View, hide others
     */
    private fun showAnimationView(view: View, type: AnimationTypeDetector.AnimationType) {
        // If type is same and View already exists, use directly
        if (currentAnimationType == type && currentAnimationView != null && 
            container.indexOfChild(currentAnimationView) >= 0) {
            // View already exists, just show
            currentAnimationView?.visibility = View.VISIBLE
            return
        }
        
        // Hide all child Views
        for (i in 0 until container.childCount) {
            container.getChildAt(i).visibility = View.GONE
        }
        
        // Add new View or show existing View
        if (container.indexOfChild(view) < 0) {
            // View doesn't exist, add it
            container.addView(view)
        }
        view.visibility = View.VISIBLE
        
        currentAnimationView = view
        currentAnimationType = type
    }
    
    override fun onResourceReady(resource: Any) {
        val animationType = detectAnimationType(resource)
        val view = currentAnimationView ?: createAnimationView(animationType)
        
        // Show corresponding View
        showAnimationView(view, animationType)
        
        // Get configuration options
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true
        
        // Set listener first (avoid missing onAnimationStart)
        when {
            resource is PAGFile && view is PAGImageView -> {
                AnimationPlayListenerSetupHelper.setupListeners(this, resource, view)
            }
            resource is LottieDrawable && view is LottieAnimationView -> {
                AnimationPlayListenerSetupHelper.setupListeners(this, resource, view)
            }
            resource is SVGADrawable && view is SVGAImageView -> {
                AnimationPlayListenerSetupHelper.setupListeners(this, resource, view)
            }
            resource is GifDrawable && view is GifImageView -> {
                AnimationPlayListenerSetupHelper.setupListeners(this, resource, view)
            }
        }
        
        // Set resource and configuration based on type
        when {
            resource is PAGFile && view is PAGImageView -> {
                view.apply {
                    composition = resource
                    setRepeatCount(
                        when {
                            repeatCount < 0 -> -1
                            else -> repeatCount
                        }
                    )
                    if (autoPlay) {
                        play()
                    }
                }
            }
            resource is LottieDrawable && view is LottieAnimationView -> {
                view.apply {
                    resource.composition?.let { setComposition(it) }
                    this.repeatCount = when {
                        repeatCount < 0 -> LottieDrawable.INFINITE
                        repeatCount == 0 -> 0
                        else -> repeatCount
                    }
                    if (autoPlay) {
                        playAnimation()
                    }
                }
            }
            resource is SVGADrawable && view is SVGAImageView -> {
                view.apply {
                    setVideoItem(resource.videoItem)
                    // SVGA loop setting
                    if (repeatCount >= 0) {
                        try {
                            val animatorField = SVGAImageView::class.java.getDeclaredField("mAnimator")
                            animatorField.isAccessible = true
                            val animator = animatorField.get(this) as? android.animation.ValueAnimator
                            if (animator != null) {
                                animator.repeatCount = if (repeatCount == 0) 0 else repeatCount
                            } else if (autoPlay) {
                                post {
                                    try {
                                        val delayedAnimator = animatorField.get(this) as? android.animation.ValueAnimator
                                        delayedAnimator?.repeatCount = if (repeatCount == 0) 0 else repeatCount
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    if (autoPlay) {
                        startAnimation()
                    }
                }
            }
            resource is GifDrawable && view is GifImageView -> {
                val loopCount = when {
                    repeatCount < 0 -> 0
                    repeatCount == 0 -> 1
                    else -> repeatCount
                }
                resource.loopCount = loopCount
                view.setImageDrawable(resource)
            }
            else -> {
                throw IllegalStateException("Resource type ${resource.javaClass.simpleName} does not match animation type $animationType")
            }
        }
    }
    
    override fun onLoadFailed(errorDrawable: Drawable?) {
        // Show error placeholder when load fails
        currentAnimationView?.let { view ->
            when (view) {
                is GifImageView -> view.setImageDrawable(errorDrawable)
                // Other types don't support error placeholder yet
            }
        }
    }
    
    override fun onLoadCleared(placeholder: Drawable?) {
        // Clear all animation Views
        currentAnimationView?.let { view ->
            when (view) {
                is PAGImageView -> view.composition = null
                is LottieAnimationView -> {
                    // LottieAnimationView's composition is val, cannot set to null directly
                    // Call cancelAnimation() to clean up
                    view.cancelAnimation()
                }
                is SVGAImageView -> {
                    view.stopAnimation()
                    view.setVideoItem(null)
                }
                is GifImageView -> view.setImageDrawable(placeholder)
            }
        }
        
        // Hide all child Views
        for (i in 0 until container.childCount) {
            container.getChildAt(i).visibility = View.GONE
        }
        
        currentAnimationView = null
        currentAnimationType = null
    }
    
    /**
     * Detect resource type
     */
    private fun detectAnimationType(resource: Any): AnimationTypeDetector.AnimationType {
        return when (resource) {
            is PAGFile -> AnimationTypeDetector.AnimationType.PAG
            is LottieDrawable -> AnimationTypeDetector.AnimationType.LOTTIE
            is SVGADrawable -> AnimationTypeDetector.AnimationType.SVGA
            is GifDrawable -> AnimationTypeDetector.AnimationType.GIF
            else -> AnimationTypeDetector.AnimationType.UNKNOWN
        }
    }
}

