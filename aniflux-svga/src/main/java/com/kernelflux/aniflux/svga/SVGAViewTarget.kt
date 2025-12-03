package com.kernelflux.aniflux.svga

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.view.View
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.svga.SVGACallback
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView

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
    private var currentAdapter: SVGAPlayListenerAdapter? = null
    private var currentCallback: SVGACallback? = null

    override fun setupPlayListeners(resource: Any, view: View?) {
        val svgaView = view as? SVGAImageView ?: return
        val listener = playListener ?: return

        // 移除旧的监听器
        currentCallback?.let { oldCallback ->
            try {
                svgaView.callback = null
            } catch (e: Exception) {
                // 忽略移除时的异常
            }
        }

        // 创建新的适配器
        val adapter = SVGAPlayListenerAdapter(listener)
        val svgaCallback = adapter.createAnimatorListener()
        svgaView.callback = svgaCallback

        // 保存引用以便清理
        currentAdapter = adapter
        currentCallback = svgaCallback
    }

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

                placeholderManager = PlaceholderManagerFactory.create(
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
        // 清理监听器
        currentCallback?.let { callback ->
            try {
                view.callback = null
            } catch (e: Exception) {
                // 忽略清理时的异常
            }
        }
        currentAdapter?.onClear()
        currentAdapter = null
        currentCallback = null

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

