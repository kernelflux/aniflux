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
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.vap.AnimView
import org.libpag.PAGFile
import org.libpag.PAGImageView

/**
 * 通用的动画容器 FrameLayout Target
 * 自动根据动画类型创建并显示对应的动画 View
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
     * 根据动画类型创建对应的 View
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
     * 显示指定的动画 View，隐藏其他的
     */
    private fun showAnimationView(view: View, type: AnimationTypeDetector.AnimationType) {
        // 如果类型相同且 View 已存在，直接使用
        if (currentAnimationType == type && currentAnimationView != null && 
            container.indexOfChild(currentAnimationView) >= 0) {
            // View 已存在，只需显示
            currentAnimationView?.visibility = View.VISIBLE
            return
        }
        
        // 隐藏所有子 View
        for (i in 0 until container.childCount) {
            container.getChildAt(i).visibility = View.GONE
        }
        
        // 添加新 View 或显示已存在的 View
        if (container.indexOfChild(view) < 0) {
            // View 不存在，添加它
            container.addView(view)
        }
        view.visibility = View.VISIBLE
        
        currentAnimationView = view
        currentAnimationType = type
    }
    
    override fun onResourceReady(resource: Any) {
        val animationType = detectAnimationType(resource)
        val view = currentAnimationView ?: createAnimationView(animationType)
        
        // 显示对应的 View
        showAnimationView(view, animationType)
        
        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true
        
        // 先设置监听器（避免错过 onAnimationStart）
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
        
        // 根据类型设置资源和配置
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
                    // SVGA 的循环设置
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
                                        // 忽略
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略
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
        // 加载失败时显示错误占位图
        currentAnimationView?.let { view ->
            when (view) {
                is GifImageView -> view.setImageDrawable(errorDrawable)
                // 其他类型暂不支持错误占位图
            }
        }
    }
    
    override fun onLoadCleared(placeholder: Drawable?) {
        // 清理所有动画 View
        currentAnimationView?.let { view ->
            when (view) {
                is PAGImageView -> view.composition = null
                is LottieAnimationView -> {
                    // LottieAnimationView 的 composition 是 val，不能直接设置为 null
                    // 调用 cancelAnimation() 来清理
                    view.cancelAnimation()
                }
                is SVGAImageView -> {
                    view.stopAnimation()
                    view.setVideoItem(null)
                }
                is GifImageView -> view.setImageDrawable(placeholder)
            }
        }
        
        // 隐藏所有子 View
        for (i in 0 until container.childCount) {
            container.getChildAt(i).visibility = View.GONE
        }
        
        currentAnimationView = null
        currentAnimationType = null
    }
    
    /**
     * 检测资源类型
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

