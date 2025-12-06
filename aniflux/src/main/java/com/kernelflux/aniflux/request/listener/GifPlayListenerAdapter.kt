package com.kernelflux.aniflux.request.listener

import com.kernelflux.gif.AnimationListener
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView


/**
 * GIF animation play listener adapter
 * Adapts GifDrawable's AnimationListener to unified AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class GifPlayListenerAdapter(
    listener: AnimationPlayListener,
    private val gifImageView: GifImageView? = null,
    private val retainLastFrame: Boolean = true
) : InternalBasePlayListenerAdapter<AnimationListener>(listener) {

    override fun createAnimatorListener(loopCount: Int?): AnimationListener {
        var isFirstLoadAim = false
        val totalPlays = loopCount ?: 0
        return AnimationListener { loopNumber ->
            if (!isFirstLoadAim) {
                isFirstLoadAim = true
                notifyAnimationStart()
            } else {
                notifyAnimationRepeat()
                val isLastPlay = totalPlays > 0 && loopNumber >= totalPlays - 1
                if (isLastPlay) {
                    // ✅ Retain frame at current stop position: do nothing, GIF already automatically stays at current frame
                    if (!retainLastFrame) {
                        gifImageView?.setImageDrawable(null)
                    }
                    notifyAnimationEnd()
                }
            }
        }
    }
}

