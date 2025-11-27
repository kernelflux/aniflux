package com.kernelflux.aniflux.request.listener

import com.kernelflux.svga.SVGACallback
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * SVGA动画播放监听器适配器
 * SVGA内部使用Animator，需要通过反射或包装方式添加监听
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
             * 通过 percentage 和 frame 计算总帧数
             */
            fun calculateTotalFrames(currentFrame: Int, percentage: Double): Int? {
                // 1. 边界检查
                if (percentage <= 0.0 || percentage > 1.0) {
                    return null
                }
                if (currentFrame < 0) {
                    return null
                }

                // 2. 计算
                val totalFrames = (currentFrame + 1) / percentage
                val totalFramesInt = totalFrames.roundToInt()

                // 3. 验证：总帧数应该大于当前帧
                if (totalFramesInt <= currentFrame) {
                    return null
                }

                // 4. 验证：反向计算 percentage 是否一致（允许小误差）
                val recalculatedPercentage = (currentFrame + 1).toDouble() / totalFramesInt
                val diff = abs(recalculatedPercentage - percentage)
                if (diff > 0.01) {  // 允许 1% 的误差
                    return null
                }

                return totalFramesInt
            }
        }
    }
}

