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
    listener: AnimationPlayListener,
    private val lottieView: LottieAnimationView? = null,
    private val retainLastFrame: Boolean = true
) : InternalBasePlayListenerAdapter<Animator.AnimatorListener>(listener) {
    override fun createAnimatorListener(loopCount: Int?): Animator.AnimatorListener {
        return object : Animator.AnimatorListener {
            override fun onAnimationCancel(animation: Animator) {
                notifyAnimationCancel()
            }

            override fun onAnimationEnd(animation: Animator) {
                // ✅ 保留当前停止位置的帧：不做任何操作，Lottie 已经自动停留在当前帧
                // 动画结束时，Lottie 会自动停留在当前播放位置（最后一帧或暂停位置）
                // 只有在 retainLastFrame = false 时才需要清空，但 Lottie 没有直接的清空 API
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

