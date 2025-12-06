package com.kernelflux.aniflux.request.listener

import android.animation.Animator
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.lottie.LottieAnimationView

/**
 * Lottie animation play listener adapter
 * Adapts Lottie's AnimatorListener to unified AnimationPlayListener
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
                AniFluxLog.d(AniFluxLogCategory.TARGET, "Animation ended, notifying onAnimationEnd")
                if (!retainLastFrame) {
                    lottieView?.let {
                        // ✅ Clear display:
                        // LottieAnimationView inherits from ImageView
                        // To clear display, must call ImageView methods:
                        // 1. cancelAnimation() - stop animation
                        // 2. setImageDrawable(null) - clear Drawable (key! This is the only way to clear display)
                        // Note: setComposition(null) is not supported, must use setImageDrawable(null)
                        it.post {
                            try {
                                // Remove listener first, avoid cancelAnimation triggering callback
                                it.removeAnimatorListener(this)
                                // Stop animation
                                it.cancelAnimation()
                                // ✅ Clear display: use ImageView method
                                // LottieAnimationView inherits from ImageView, setImageDrawable(null) can clear display
                                it.setImageDrawable(null)
                                // Ensure refresh
                                it.invalidate()
                            } catch (e: Exception) {
                                AniFluxLog.e(
                                    AniFluxLogCategory.TARGET,
                                    "Failed to clear Lottie view",
                                    e
                                )
                            }
                        }
                    }
                } else {
                    // ✅ Retain frame at current stop position: do nothing, Lottie already automatically stays at current frame
                    // When animation ends, Lottie automatically stays at current playback position (last frame or pause position)
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

