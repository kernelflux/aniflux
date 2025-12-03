package com.kernelflux.aniflux.gif

import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.InternalBasePlayListenerAdapter
import com.kernelflux.gif.AnimationListener
import com.kernelflux.gif.GifImageView


/**
 * GIF动画播放监听器适配器
 * 将GifDrawable的AnimationListener适配到统一的AnimationPlayListener
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
                    // ✅ 保留当前停止位置的帧：不做任何操作，GIF 已经自动停留在当前帧
                    if (!retainLastFrame) {
                        gifImageView?.setImageDrawable(null)
                    }
                    notifyAnimationEnd()
                }
            }
        }
    }
}

