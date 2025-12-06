package com.kernelflux.aniflux.pag

import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.InternalBasePlayListenerAdapter
import com.kernelflux.pag.PAGView
import kotlin.math.roundToInt

/**
 * PAG animation play listener adapter
 * Adapts PAG's listener interface to unified AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class PAGViewPlayListenerAdapter(
    listener: AnimationPlayListener,
    private val pagView: PAGView? = null,
    private val retainLastFrame: Boolean = true
) : InternalBasePlayListenerAdapter<PAGView.PAGViewListener>(listener) {

    override fun createAnimatorListener(loopCount: Int?): PAGView.PAGViewListener {
        return object : PAGView.PAGViewListener {
            override fun onAnimationStart(p0: PAGView?) {
                notifyAnimationStart()
            }

            override fun onAnimationEnd(p0: PAGView?) {
                if (!retainLastFrame) {
                    val view = p0 ?: pagView
                    view?.apply {
                        try {
                            // ✅ Clear display:
                            // 1. Stop animation first
                            view.pause()
                            // 2. Clear PAGView (uses TextureView + OpenGL)
                            execute({
                                // PAGView uses TextureView + OpenGL rendering
                                // Set composition = null and reset progress to start position, then refresh
                                view.composition = null
                                view.progress = 0.0
                                view.flush()
                                // Force refresh view
                                view.postInvalidate()
                            })
                        } catch (t: Throwable) {
                            // Ignore errors
                        }
                    }
                }
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
                        val duration = composition.duration() // microseconds
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

