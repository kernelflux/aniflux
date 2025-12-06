package com.kernelflux.aniflux.svga

import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.InternalBasePlayListenerAdapter
import com.kernelflux.svga.SVGACallback
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * SVGA animation play listener adapter
 * SVGA internally uses Animator, needs to add listener via reflection or wrapper
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class SVGAPlayListenerAdapter(
    listener: AnimationPlayListener
) : InternalBasePlayListenerAdapter<SVGACallback>(listener) {

    override fun createAnimatorListener(loopCount: Int?): SVGACallback {
        return object : SVGACallback {
            override fun onStart() {
                notifyAnimationStart()
            }

            override fun onPause() {
                notifyAnimationCancel()
            }

            override fun onFinished() {
                notifyAnimationEnd()
            }

            override fun onRepeat() {
                notifyAnimationRepeat()
            }

            override fun onStep(frame: Int, percentage: Double) {
                val validatedTotalFrames = calculateTotalFrames(frame, percentage) ?: return
                notifyAnimationUpdate(frame, validatedTotalFrames)
            }

            /**
             * Calculate total frames from percentage and frame
             */
            fun calculateTotalFrames(currentFrame: Int, percentage: Double): Int? {
                // 1. Boundary check
                if (percentage <= 0.0 || percentage > 1.0) {
                    return null
                }
                if (currentFrame < 0) {
                    return null
                }

                // 2. Calculate
                val totalFrames = (currentFrame + 1) / percentage
                val totalFramesInt = totalFrames.roundToInt()

                // 3. Validate: total frames should be greater than current frame
                if (totalFramesInt <= currentFrame) {
                    return null
                }

                // 4. Validate: reverse calculate percentage to check consistency (allow small error)
                val recalculatedPercentage = (currentFrame + 1).toDouble() / totalFramesInt
                val diff = abs(recalculatedPercentage - percentage)
                if (diff > 0.01) {  // Allow 1% error
                    return null
                }

                return totalFramesInt
            }
        }
    }
}

