package com.kernelflux.aniflux.request.listener

import com.kernelflux.vap.AnimConfig
import com.kernelflux.vap.AnimView
import com.kernelflux.vap.inter.IAnimListener


/**
 * Vap动画播放监听器适配器
 * 将VAP的IAnimListener适配到统一的AnimationPlayListener
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
        // ✅ VAP 的 setLoop 语义：0=不循环（播放1次），N=循环N次（总播放N+1次）
        // 统一 API 语义：repeatCount = N 表示总播放 N 次
        // 转换关系：repeatCount(3) → setLoop(2) → 总播放 3 次 → 应该有 2 次 onAnimationRepeat
        var hasStarted = false
        var lastFrameIndex = -1
        var isFirstFrame = true
        var sawLastFrame = false  // 标记是否已经看到最后一帧
        
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
                
                // ✅ 检测循环的改进逻辑：
                // 1. 追踪是否已经看到最后一帧（frameIndex >= totalFrames - 1）
                // 2. 当 frameIndex 从非 0（特别是从最后一帧）回到 0 时，说明开始了新循环
                // 3. 使用 sawLastFrame 标志确保我们确实已经播放过最后一帧
                
                // 检查是否到达最后一帧
                if (frameIndex >= totalFrames - 1 && totalFrames > 0) {
                    sawLastFrame = true
                }
                
                // ✅ 检测循环：frameIndex == 0 且 sawLastFrame == true 且不是第一帧
                // 如果 sawLastFrame，说明确实完成了一次完整播放，此时 frameIndex == 0 肯定是循环
                // 或者，如果 lastFrameIndex > 0 且 frameIndex == 0，也认为是循环（从非第一帧回到第一帧）
                val isLoopDetected = hasStarted && 
                    !isFirstFrame && 
                    frameIndex == 0 && 
                    (sawLastFrame || lastFrameIndex > 0)
                
                if (isLoopDetected) {
                    // 开始新的循环（从非第一帧回到第一帧）
                    notifyAnimationRepeat()
                    // 重置标志，为新循环准备
                    sawLastFrame = false
                }
                
                // 更新状态
                if (isFirstFrame && frameIndex == 0) {
                    isFirstFrame = false
                }
                lastFrameIndex = frameIndex
                
                notifyAnimationUpdate(frameIndex, totalFrames)
            }

            override fun onVideoComplete() {
                // ✅ VAP 源码已支持 retainLastFrame 配置
                // 在 VAPViewTarget 中已经设置了 animView.retainLastFrame
                // 如果 retainLastFrame = true，AnimView 不会调用 hide()，最后一帧会被保留
                // 如果 retainLastFrame = false，AnimView 会调用 hide()，清空视图
                notifyAnimationEnd()
            }

            override fun onVideoDestroy() {
                // ✅ onVideoDestroy 是资源销毁回调，不是 cancel
                // VAP 库在播放结束后会自动调用 onVideoDestroy 进行资源清理（参考 HardDecoder.kt:393）
                // 这是正常的资源清理，不应该触发 onAnimationCancel()
                // 注意：如果需要在资源清理时做某些操作，可以在这里处理，但不应该调用 notifyAnimationCancel()
            }

            override fun onFailed(errorType: Int, errorMsg: String?) {
                notifyAnimationFailed(Throwable(errorMsg))
            }
        }
    }

}

