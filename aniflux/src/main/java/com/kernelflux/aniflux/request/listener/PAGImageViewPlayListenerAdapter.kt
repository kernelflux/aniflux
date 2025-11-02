package com.kernelflux.aniflux.request.listener

import org.libpag.PAGImageView

/**
 * PAG动画播放监听器适配器
 * 将PAG的监听接口适配到统一的AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class PAGImageViewPlayListenerAdapter(
    listener: AnimationPlayListener
) : InternalBasePlayListenerAdapter<PAGImageView.PAGImageViewListener>(listener) {

    override fun createAnimatorListener(loopCount: Int?): PAGImageView.PAGImageViewListener {
        return object : PAGImageView.PAGImageViewListener {
            override fun onAnimationStart(p0: PAGImageView?) {
                notifyAnimationStart()
            }

            override fun onAnimationEnd(p0: PAGImageView?) {
                notifyAnimationEnd()
            }

            override fun onAnimationCancel(p0: PAGImageView?) {
                notifyAnimationCancel()
            }

            override fun onAnimationRepeat(p0: PAGImageView?) {
                notifyAnimationRepeat()
            }

            override fun onAnimationUpdate(p0: PAGImageView?) {
                val pagImageView = p0 ?: return
                val currentFrame = pagImageView.currentFrame()
                val totalFrame = pagImageView.numFrames()
                notifyAnimationUpdate(currentFrame, totalFrame)
            }
        }
    }
}

