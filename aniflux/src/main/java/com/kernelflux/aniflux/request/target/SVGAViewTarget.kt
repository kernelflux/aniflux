package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerSetupHelper
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGAImageView

/**
 * SVGA动画的专用ViewTarget
 * 自动处理SVGADrawable资源到SVGAImageView的设置
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class SVGAViewTarget(view: SVGAImageView) : CustomViewAnimationTarget<SVGAImageView, SVGADrawable>(view) {
    
    override fun onResourceReady(resource: SVGADrawable) {
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)
        
        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true
        
        view.apply {
            setVideoItem(resource.videoItem)
            
            // SVGA 的循环设置需要通过反射设置内部的 animator
            if (repeatCount >= 0) {
                try {
                    val animatorField = SVGAImageView::class.java.getDeclaredField("mAnimator")
                    animatorField.isAccessible = true
                    val animator = animatorField.get(this) as? android.animation.ValueAnimator
                    if (animator != null) {
                        // SVGA 内部使用 ValueAnimator，设置 repeatCount
                        animator.repeatCount = when {
                            repeatCount == 0 -> 0 // 0 表示不循环
                            else -> repeatCount // >0 表示循环次数
                        }
                    } else {
                        // 如果 animator 还未创建，在 startAnimation 后延迟设置
                        if (autoPlay) {
                            post {
                                try {
                                    val delayedAnimator = animatorField.get(this) as? android.animation.ValueAnimator
                                    delayedAnimator?.repeatCount = when {
                                        repeatCount == 0 -> 0
                                        else -> repeatCount
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("SVGAViewTarget", "Failed to set repeatCount", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SVGAViewTarget", "Failed to set SVGA repeatCount via reflection", e)
                }
            }
            
            // 如果设置了自动播放，则调用 startAnimation()
            if (autoPlay) {
                startAnimation()
            }
        }
    }
    
    override fun onLoadFailed(errorDrawable: Drawable?) {
        // SVGA 加载失败的处理
    }
    
    override fun onResourceCleared(placeholder: Drawable?) {
        view.stopAnimation()
        view.setVideoItem(null)
    }
}

