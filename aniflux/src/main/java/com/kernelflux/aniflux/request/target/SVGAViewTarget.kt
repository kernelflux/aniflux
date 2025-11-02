package com.kernelflux.aniflux.request.target

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import com.kernelflux.svgaplayer.SVGADrawable
import com.kernelflux.svgaplayer.SVGAImageView

/**
 * SVGA动画的专用ViewTarget
 * 自动处理SVGADrawable资源到SVGAImageView的设置
 *
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class SVGAViewTarget(view: SVGAImageView) :
    CustomViewAnimationTarget<SVGAImageView, SVGADrawable>(view) {

    override fun onResourceReady(resource: SVGADrawable) {
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)

        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true

        view.apply {
            setVideoItem(resource.videoItem)
            setPlayRepeatCount(
                when {
                    repeatCount < 0 -> ValueAnimator.INFINITE  // -1
                    repeatCount == 0 -> 0  // 播放1次
                    else -> repeatCount  // ✅ 直接使用总播放次数，让 SVGAImageView.play() 统一转换为 ValueAnimator 的重复次数
                }
            )
            // 如果设置了自动播放，则调用 startAnimation()
            if (autoPlay) {
                startAnimation()
            }
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        // SVGA 加载失败的处理
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        view.stopAnimation()
        view.setVideoItem(null)
    }
}

