package com.kernelflux.aniflux.pag

import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.InternalBasePlayListenerAdapter
import com.kernelflux.pag.PAGImageView

/**
 * PAG animation play listener adapter
 * Adapts PAG's listener interface to unified AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class PAGImageViewPlayListenerAdapter(
    listener: AnimationPlayListener,
    private val pagImageView: PAGImageView? = null,
    private val retainLastFrame: Boolean = true
) : InternalBasePlayListenerAdapter<PAGImageView.PAGImageViewListener>(listener) {

    override fun createAnimatorListener(loopCount: Int?): PAGImageView.PAGImageViewListener {
        return object : PAGImageView.PAGImageViewListener {
            override fun onAnimationStart(p0: PAGImageView?) {
                notifyAnimationStart()
            }

            override fun onAnimationEnd(p0: PAGImageView?) {
                val view = p0 ?: pagImageView
                if (view != null) {
                    if (retainLastFrame) {
                        // ✅ Retain frame at current stop position: do nothing, PAG already automatically stays at current frame
                        // When animation ends, PAG automatically stays at current playback position (last frame or pause position)
                    } else {
                        // ✅ Clear display:
                        // 1. Pause animation first (if still playing)
                        if (view.isPlaying) {
                            view.pause()
                        }
                        // 2. Set composition to null to clear display (this calls refreshResource, internally releases bitmap)
                        execute({
                            view.composition = null
                            // Ensure refresh
                            view.postInvalidate()
                        })
                    }
                }
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

