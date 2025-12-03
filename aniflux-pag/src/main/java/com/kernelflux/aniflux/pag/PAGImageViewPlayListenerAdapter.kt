package com.kernelflux.aniflux.pag

import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.InternalBasePlayListenerAdapter
import com.kernelflux.pag.PAGImageView

/**
 * PAG动画播放监听器适配器
 * 将PAG的监听接口适配到统一的AnimationPlayListener
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
                        // ✅ 保留当前停止位置的帧：不做任何操作，PAG 已经自动停留在当前帧
                        // 动画结束时，PAG 会自动停留在当前播放位置（最后一帧或暂停位置）
                    } else {
                        // ✅ 清空显示：
                        // 1. 先暂停动画（如果还在播放）
                        if (view.isPlaying) {
                            view.pause()
                        }
                        // 2. 设置 composition 为 null 以清空显示（这会调用 refreshResource，内部会 releaseBitmap）
                        execute({
                            view.composition = null
                            // 确保刷新
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

