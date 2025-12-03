package com.kernelflux.aniflux.lottie

import android.animation.Animator
import android.graphics.drawable.Drawable
import android.view.View
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.lottie.LottieDrawable

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
    private var currentAdapter: LottiePlayListenerAdapter? = null
    private var currentListener: Animator.AnimatorListener? = null

    override fun setupPlayListeners(resource: Any, view: View?) {
        val lottieView = view as? LottieAnimationView ?: return
        val listener = playListener ?: return

        // 移除旧的监听器
        currentListener?.let { oldListener ->
            try {
                lottieView.removeAnimatorListener(oldListener)
            } catch (e: Exception) {
                // 忽略移除时的异常
            }
        }

        // 获取 retainLastFrame 配置
        val retainLastFrame = animationOptions?.retainLastFrame ?: true

        // 创建新的适配器
        val adapter = LottiePlayListenerAdapter(listener, lottieView, retainLastFrame)
        val userRepeatCount = animationOptions?.repeatCount ?: -1
        val animatorListener = adapter.createAnimatorListener(userRepeatCount)
        lottieView.addAnimatorListener(animatorListener)

        // 保存引用以便清理
        currentAdapter = adapter
        currentListener = animatorListener
    }

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
        // Lottie 加载失败的处理
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
        placeholderManager = null
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        // 清理监听器
        currentListener?.let { listener ->
            try {
                view.removeAnimatorListener(listener)
            } catch (e: Exception) {
                // 忽略清理时的异常
            }
        }
        currentAdapter?.onClear()
        currentAdapter = null
        currentListener = null

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

