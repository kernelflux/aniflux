package com.kernelflux.aniflux.request.listener

import com.kernelflux.pag.PAGView
import kotlin.math.roundToInt

/**
 * PAG动画播放监听器适配器
 * 将PAG的监听接口适配到统一的AnimationPlayListener
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
                            // ✅ 清空显示：
                            // 1. 先停止动画
                            view.pause()
                            // 2. 清空 PAGView（使用 TextureView + OpenGL）
                            execute({
                                // PAGView 使用 TextureView + OpenGL 渲染
                                // 设置 composition = null 并重置进度到开始位置，然后刷新
                                view.composition = null
                                view.progress = 0.0
                                view.flush()
                                // 强制刷新视图
                                view.postInvalidate()
                            })
                        } catch (t: Throwable) {
                            // 忽略错误
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

