package com.kernelflux.aniflux.request.listener

import android.animation.Animator
import com.opensource.svgaplayer.SVGAImageView
import java.lang.ref.WeakReference

/**
 * SVGA动画播放监听器适配器
 * SVGA内部使用Animator，需要通过反射或包装方式添加监听
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class SVGAPlayListenerAdapter(
    private val listener: AnimationPlayListener
) {
    
    /**
     * 创建AnimatorListener适配器
     * 注意：SVGA内部使用ValueAnimator，我们需要通过反射或者SVGA的回调机制来实现
     * 这里先创建一个基础的适配器，实际使用时需要配合SVGAImageView的callback机制
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
}

