package com.kernelflux.aniflux.request.listener

import android.animation.Animator
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * Lottie动画播放监听器适配器
 * 将Lottie的AnimatorListener适配到统一的AnimationPlayListener
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class LottiePlayListenerAdapter(
    private val listener: AnimationPlayListener
) {
    
    /**
     * 创建AnimatorListener适配器
     */
    fun createAnimatorListener(): Animator.AnimatorListener {
        return object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                listener.onAnimationStart()
            }
            
            override fun onAnimationEnd(animation: Animator) {
                listener.onAnimationEnd()
            }
            
            override fun onAnimationCancel(animation: Animator) {
                listener.onAnimationCancel()
            }
            
            override fun onAnimationRepeat(animation: Animator) {
                listener.onAnimationRepeat()
            }
        }
    }
    
    /**
     * 为LottieAnimationView添加监听器
     */
    fun attachToView(view: LottieAnimationView) {
        view.addAnimatorListener(createAnimatorListener())
    }
    
    /**
     * 为LottieDrawable添加监听器
     */
    fun attachToDrawable(drawable: LottieDrawable) {
        drawable.addAnimatorListener(createAnimatorListener())
    }
}

