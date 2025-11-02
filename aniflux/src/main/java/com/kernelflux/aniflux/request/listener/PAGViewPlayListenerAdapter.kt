package com.kernelflux.aniflux.request.listener

import org.libpag.PAGView
import kotlin.math.roundToInt

/**
 * PAG动画播放监听器适配器
 * 将PAG的监听接口适配到统一的AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class PAGViewPlayListenerAdapter(
    listener: AnimationPlayListener
) : InternalBasePlayListenerAdapter<PAGView.PAGViewListener>(listener) {

    override fun createAnimatorListener(loopCount: Int?): PAGView.PAGViewListener {
        return object : PAGView.PAGViewListener {
            override fun onAnimationStart(p0: PAGView?) {
                notifyAnimationStart()
            }

            override fun onAnimationEnd(p0: PAGView?) {
                notifyAnimationEnd()
            }

            override fun onAnimationCancel(p0: PAGView?) {
                notifyAnimationCancel()
            }

            override fun onAnimationRepeat(p0: PAGView?) {
                notifyAnimationRepeat()
            }

            override fun onAnimationUpdate(p0: PAGView?) {
                val pagView = p0 ?: return
                try {
                    val currentFrame = pagView.currentFrame().toInt()
                    val composition = pagView.composition
                    var totalFrames = 0
                    if (composition != null) {
                        val duration = composition.duration() // 微秒
                        val frameRate = composition.frameRate() // FPS
                        totalFrames = ((duration / 1000000.0) * frameRate).roundToInt()
                    }
                    notifyAnimationUpdate(currentFrame, totalFrames)
                } catch (t: Throwable) {
                    //
                }
            }
        }
    }
}

