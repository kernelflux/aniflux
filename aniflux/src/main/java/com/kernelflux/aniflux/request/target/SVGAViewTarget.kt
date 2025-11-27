package com.kernelflux.aniflux.request.target

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.svgaplayer.SVGADrawable
import com.kernelflux.svgaplayer.SVGAImageView

/**
 * SVGA动画的专用ViewTarget
 * 自动处理SVGADrawable资源到SVGAImageView的设置
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
class SVGAViewTarget(view: SVGAImageView) :
    CustomViewAnimationTarget<SVGAImageView, SVGADrawable>(view) {

    private var placeholderManager: PlaceholderManager? = null

    override fun onResourceReady(resource: SVGADrawable) {
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)

        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true
        val retainLastFrame = animationOptions?.retainLastFrame ?: true

        view.apply {
            setVideoItem(resource.videoItem)
            setPlayRepeatCount(
                when {
                    repeatCount < 0 -> ValueAnimator.INFINITE  // -1
                    repeatCount == 0 -> 0  // 播放1次
                    else -> repeatCount  // ✅ 直接使用总播放次数，让 SVGAImageView.play() 统一转换为 ValueAnimator 的重复次数
                }
            )
            // ✅ 根据 retainLastFrame 配置设置 fillMode
            fillMode = if (retainLastFrame) {
                SVGAImageView.FillMode.Forward  // 保留最后一帧
            } else {
                SVGAImageView.FillMode.Clear    // 清空
            }
            // 如果设置了自动播放，则调用 startAnimation()
            if (autoPlay) {
                startAnimation()
            }
        }

        // 处理占位图替换
        animationOptions?.placeholderReplacements?.let { replacements ->
            // 先清理旧的占位图管理器（如果存在）
            placeholderManager?.clear()
            placeholderManager = null
            
            val imageLoader = AniFlux.get(view.context).getPlaceholderImageLoader()
            if (imageLoader != null) {
                val lifecycle = getLifecycle()

                placeholderManager = PlaceholderManager.create(
                    view = view,
                    resource = resource,
                    replacements = replacements,
                    imageLoader = imageLoader,
                    lifecycle = lifecycle
                )

                placeholderManager?.applyReplacements()
            }
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        // SVGA 加载失败的处理
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
        placeholderManager = null
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
        placeholderManager = null
        try {
            view.stopAnimation()
            view.setVideoItem(null)
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
    }
}

