package com.kernelflux.aniflux.vap

import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.InternalBasePlayListenerAdapter
import com.kernelflux.vap.AnimConfig
import com.kernelflux.vap.AnimView
import com.kernelflux.vap.inter.IAnimListener

/**
 * VAP animation play listener adapter
 * Adapts VAP's IAnimListener to unified AnimationPlayListener
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
class VapPlayListenerAdapter(
    listener: AnimationPlayListener,
    private val animView: AnimView? = null,
    private val retainLastFrame: Boolean = true
) : InternalBasePlayListenerAdapter<IAnimListener>(listener) {

    override fun createAnimatorListener(loopCount: Int?): IAnimListener {
        // ✅ VAP's setLoop semantics: 0=no loop (play once), N=loop N times (total play N+1 times)
        // Unified API semantics: repeatCount = N means total play N times
        // Conversion: repeatCount(3) → setLoop(2) → total play 3 times → should have 2 onAnimationRepeat
        var hasStarted = false
        var lastFrameIndex = -1
        var isFirstFrame = true
        var sawLastFrame = false  // Flag whether last frame has been seen
        
        return object : IAnimListener {
            override fun onVideoStart() {
                if (!hasStarted) {
                    hasStarted = true
                    lastFrameIndex = -1
                    isFirstFrame = true
                    sawLastFrame = false
                    notifyAnimationStart()
                }
            }

            override fun onVideoRender(
                frameIndex: Int,
                config: AnimConfig?
            ) {
                val conf = config ?: return
                val totalFrames = conf.totalFrames
                
                // ✅ Improved loop detection logic:
                // 1. Track whether last frame has been seen (frameIndex >= totalFrames - 1)
                // 2. When frameIndex returns to 0 from non-0 (especially from last frame), indicates new loop started
                // 3. Use sawLastFrame flag to ensure we have indeed played the last frame
                
                // Check if reached last frame
                if (frameIndex >= totalFrames - 1 && totalFrames > 0) {
                    sawLastFrame = true
                }
                
                // ✅ Detect loop: frameIndex == 0 && sawLastFrame == true && not first frame
                // If sawLastFrame, means we've completed a full playback, frameIndex == 0 is definitely a loop
                // Or, if lastFrameIndex > 0 && frameIndex == 0, also consider it a loop (returned to first frame from non-first frame)
                val isLoopDetected = hasStarted && 
                    !isFirstFrame && 
                    frameIndex == 0 && 
                    (sawLastFrame || lastFrameIndex > 0)
                
                if (isLoopDetected) {
                    // Start new loop (returned to first frame from non-first frame)
                    notifyAnimationRepeat()
                    // Reset flag for new loop
                    sawLastFrame = false
                }
                
                // Update state
                if (isFirstFrame && frameIndex == 0) {
                    isFirstFrame = false
                }
                lastFrameIndex = frameIndex
                
                notifyAnimationUpdate(frameIndex, totalFrames)
            }

            override fun onVideoComplete() {
                // ✅ VAP source code already supports retainLastFrame configuration
                // animView.retainLastFrame has been set in VAPViewTarget
                // If retainLastFrame = true, AnimView won't call hide(), last frame will be retained
                // If retainLastFrame = false, AnimView will call hide(), clear view
                notifyAnimationEnd()
            }

            override fun onVideoDestroy() {
                // ✅ onVideoDestroy is resource destruction callback, not cancel
                // VAP library automatically calls onVideoDestroy for resource cleanup after playback ends (reference HardDecoder.kt:393)
                // This is normal resource cleanup, should not trigger onAnimationCancel()
                // Note: If operations are needed during resource cleanup, can handle here, but should not call notifyAnimationCancel()
            }

            override fun onFailed(errorType: Int, errorMsg: String?) {
                notifyAnimationFailed(Throwable(errorMsg))
            }
        }
    }
}

