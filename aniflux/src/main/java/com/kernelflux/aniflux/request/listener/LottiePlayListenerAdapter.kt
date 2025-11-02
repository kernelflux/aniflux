package com.kernelflux.aniflux.request.listener

import android.animation.Animator
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * Lottie动画播放监听器适配器
 * 将Lottie的AnimatorListener适配到统一的AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class LottiePlayListenerAdapter(
    listener: AnimationPlayListener
) : InternalBasePlayListenerAdapter<Animator.AnimatorListener>(listener) {
    override fun createAnimatorListener(loopCount: Int?): Animator.AnimatorListener {
        return object : Animator.AnimatorListener {
            override fun onAnimationCancel(animation: Animator) {
                notifyAnimationCancel()
            }

            override fun onAnimationEnd(animation: Animator) {
                notifyAnimationEnd()
            }

            override fun onAnimationRepeat(animation: Animator) {
                notifyAnimationRepeat()
            }

            override fun onAnimationStart(animation: Animator) {
                notifyAnimationStart()
            }
        }
    }

}

