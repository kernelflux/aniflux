package com.kernelflux.aniflux.request.listener

import pl.droidsonroids.gif.AnimationListener
import pl.droidsonroids.gif.GifDrawable

/**
 * GIF动画播放监听器适配器
 * 将GifDrawable的AnimationListener适配到统一的AnimationPlayListener
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class GifPlayListenerAdapter(
    private val listener: AnimationPlayListener
) {
    
    private var currentListener: AnimationListener? = null
    
    /**
     * 创建AnimationListener适配器
     * 注意：每次调用都会创建新的实例，需要保存引用以便后续移除
     */
    fun createAnimationListener(): AnimationListener {
        val animationListener = object : AnimationListener {
            override fun onAnimationCompleted(loopNumber: Int) {
                // GIF播放完成
                // loopNumber表示完成的循环次数（从1开始）
                // 如果loopNumber == 1，说明完成了一次播放（可能是单次播放或循环播放的第一轮）
                // 如果loopNumber > 1，说明完成了多次循环
                
                // 对于GIF，onAnimationCompleted在每次循环完成时都会调用
                // 如果是无限循环，loopNumber会一直递增
                // 这里我们假设：第一次完成时调用onAnimationEnd，后续完成时调用onAnimationRepeat
                // 但这个判断可能不够准确，因为无法区分是单次播放还是循环播放
                // 更准确的做法需要结合GifDrawable的loopCount来判断
                if (loopNumber == 1) {
                    listener.onAnimationEnd()
                } else {
                    listener.onAnimationRepeat()
                }
            }
        }
        currentListener = animationListener
        return animationListener
    }
    
    /**
     * 为GifDrawable添加监听器
     */
    fun attachToDrawable(drawable: GifDrawable) {
        drawable.addAnimationListener(createAnimationListener())
    }
    
    /**
     * 从GifDrawable移除监听器
     */
    fun detachFromDrawable(drawable: GifDrawable) {
        val animationListener = createAnimationListener()
        drawable.removeAnimationListener(animationListener)
    }
}

