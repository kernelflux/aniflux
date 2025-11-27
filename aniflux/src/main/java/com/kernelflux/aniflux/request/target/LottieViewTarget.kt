package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import androidx.lifecycle.Lifecycle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerSetupHelper

/**
 * Lottie动画的专用ViewTarget
 * 自动处理LottieDrawable资源到LottieAnimationView的设置
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
class LottieViewTarget(view: LottieAnimationView) :
    CustomViewAnimationTarget<LottieAnimationView, LottieDrawable>(view) {

    private var placeholderManager: PlaceholderManager? = null

    override fun onResourceReady(resource: LottieDrawable) {
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)

        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true

        view.apply {
            resource.composition?.let { setComposition(it) }
            this.repeatCount = when {
                repeatCount < 0 -> LottieDrawable.INFINITE  // -1
                repeatCount <= 1 -> 0  // 播放1次（不重复）
                else -> repeatCount - 1  // 总次数N → 重复次数N-1
            }

            // 如果设置了自动播放，则调用 playAnimation()
            if (autoPlay) {
                playAnimation()
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
        // Lottie 加载失败的处理
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
            view.cancelAnimation()
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
    }
}

