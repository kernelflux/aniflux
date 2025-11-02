package com.kernelflux.aniflux.request.listener

import com.kernelflux.gif.AnimationListener


/**
 * GIF动画播放监听器适配器
 * 将GifDrawable的AnimationListener适配到统一的AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class GifPlayListenerAdapter(
    listener: AnimationPlayListener
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
                    notifyAnimationEnd()
                }
            }
        }
    }
}

