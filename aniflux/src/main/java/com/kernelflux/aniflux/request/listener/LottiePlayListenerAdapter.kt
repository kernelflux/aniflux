package com.kernelflux.aniflux.request.listener

import android.animation.Animator
import com.kernelflux.lottie.LottieAnimationView

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
                android.util.Log.d("LottieAdapter", "Animation ended, notifying onAnimationEnd")
                if (!retainLastFrame) {
                    lottieView?.let {
                        // ✅ 清空显示：
                        // LottieAnimationView 继承自 ImageView
                        // 要清空画面，必须调用 ImageView 的方法：
                        // 1. cancelAnimation() - 停止动画
                        // 2. setImageDrawable(null) - 清空 Drawable（关键！这是唯一能清空画面的方法）
                        // 注意：setComposition(null) 不支持，必须用 setImageDrawable(null)
                        it.post {
                            try {
                                // 先移除监听器，避免 cancelAnimation 触发回调
                                it.removeAnimatorListener(this)
                                // 停止动画
                                it.cancelAnimation()
                                // ✅ 清空画面：使用 ImageView 的方法
                                // LottieAnimationView 继承自 ImageView，setImageDrawable(null) 可以清空显示
                                it.setImageDrawable(null)
                                // 确保刷新
                                it.invalidate()
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "LottieAdapter",
                                    "Failed to clear Lottie view",
                                    e
                                )
                            }
                        }
                    }
                } else {
                    // ✅ 保留当前停止位置的帧：不做任何操作，Lottie 已经自动停留在当前帧
                    // 动画结束时，Lottie 会自动停留在当前播放位置（最后一帧或暂停位置）
                }
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

