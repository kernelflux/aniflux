package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerSetupHelper

/**
 * Lottie动画的专用ViewTarget
 * 自动处理LottieDrawable资源到LottieAnimationView的设置
 *
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class LottieViewTarget(view: LottieAnimationView) :
    CustomViewAnimationTarget<LottieAnimationView, LottieDrawable>(view) {

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
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        // Lottie 加载失败的处理
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        view.cancelAnimation()
    }
}

