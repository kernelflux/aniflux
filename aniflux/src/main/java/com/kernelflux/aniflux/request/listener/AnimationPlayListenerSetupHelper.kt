package com.kernelflux.aniflux.request.listener

import android.annotation.SuppressLint
import android.view.View
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.gif.AnimationListener
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView
import com.kernelflux.pag.PAGView
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.vap.AnimView
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 动画播放监听器设置辅助类
 * 负责将统一的监听器设置到具体的动画对象上
 *
 * @author: kerneflux
 * @date: 2025/11/02
 */
object AnimationPlayListenerSetupHelper {
    private const val TAG = "AnimationPlayListenerSetupHelper"
    private val adapterCache = ConcurrentHashMap<String, Pair<View?, List<Any>>>()

    /**
     * 为动画资源设置播放监听器
     *
     * @param target 目标对象
     * @param resource 动画资源
     * @param view 显示动画的View（可选）
     */
    @SuppressLint("LongLogTag")
    @Suppress("UNCHECKED_CAST")
    fun setupListeners(
        target: Any,
        resource: Any,
        view: View? = null
    ) {
        // 直接获取监听器（支持 CustomAnimationTarget 和 CustomViewAnimationTarget）
        val listener = when (target) {
            is CustomAnimationTarget<*> -> target.playListener
            is CustomViewAnimationTarget<*, *> -> target.playListener
            else -> {
                android.util.Log.w(
                    TAG,
                    "setupListeners: target type not supported: ${target.javaClass.simpleName}"
                )
                return
            }
        }
        if (listener == null) {
            // 没有监听器，不需要设置
            android.util.Log.d(TAG, "setupListeners: no listener to setup")
            return
        }

        android.util.Log.d(
            TAG,
            "setupListeners: resource=${resource.javaClass.simpleName}, view=${view?.javaClass?.simpleName}"
        )

        // 关键：统一以View为准（如果view存在）
        // 因为同一个View可能被多次使用不同的target（每次into()都会创建新的target）
        // 只有真正没有View的场景（如自定义CustomAnimationTarget实现），才使用target作为key
        // 没有View参数，尝试从target中获取View
        val targetKey = view?.hashCode()?.toString() ?: when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                // ViewTarget有内部View，使用它
                target.getViewForVisibilityCheck().hashCode().toString()
            }

            is CustomAnimationTarget<*> -> {
                // 真正的CustomAnimationTarget实现（没有View），使用target作为key
                target.hashCode().toString()
            }

            else -> target.hashCode().toString()
        }

        android.util.Log.d(
            TAG,
            "setupListeners: targetKey=$targetKey (based on ${if (view != null) "view param" else "target/view from target"})"
        )

        // 根据资源类型设置监听器
        when (resource) {
            is PAGFile -> {
                setupPAGListeners(targetKey, view, listener, target)
            }

            is LottieDrawable -> {
                setupLottieListeners(targetKey, resource, view, listener, target)
            }

            is SVGADrawable -> {
                setupSVGAListeners(targetKey, view, listener)
            }

            is GifDrawable -> {
                setupGifListeners(targetKey, resource, view, listener, target)
            }

            is File -> {
                if (view is AnimView) {
                    setupVAPListeners(targetKey, view, listener, target)
                }
            }
        }
    }

    /**
     * 设置PAG动画监听器
     */
    @SuppressLint("LongLogTag")
    private fun setupPAGListeners(
        targetKey: String,
        view: View?,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // 移除旧的适配器（如果存在）- 必须从View中真正移除
        removeOldPAGAdapter(targetKey, view)

        if (view == null) {
            android.util.Log.w(TAG, "PAG listener setup failed: view is null")
            return
        }

        // 从 target 获取 retainLastFrame 配置
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // 默认保留
        }

        when (view) {
            is PAGView -> {
                val adapter = PAGViewPlayListenerAdapter(listener, view, retainLastFrame)
                val pagListener = adapter.createAnimatorListener()
                view.addListener(pagListener)
                adapterCache[targetKey] = Pair(view, listOf(pagListener))
                android.util.Log.d(TAG, "PAGView listener set")
            }

            is PAGImageView -> {
                val adapter = PAGImageViewPlayListenerAdapter(listener, view, retainLastFrame)
                val pagListener = adapter.createAnimatorListener()
                view.addListener(pagListener)
                adapterCache[targetKey] = Pair(view, listOf(pagListener))
                android.util.Log.d(TAG, "PAGImageView listener set")
            }

            else -> {
                android.util.Log.w(
                    TAG,
                    "PAG listener setup failed: view is not PAGView or PAGImageView, type=${view.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * 移除旧的PAG监听器（从View中真正移除）
     * 关键：如果currentView不为null，直接从currentView中移除，因为targetKey基于View
     */
    @SuppressLint("LongLogTag")
    private fun removeOldPAGAdapter(targetKey: String, currentView: View?) {
        val cached = adapterCache.remove(targetKey)
        if (cached != null && currentView != null) {
            val (_, listeners) = cached
            listeners.forEach { listener ->
                when {
                    listener is PAGView.PAGViewListener && currentView is PAGView -> {
                        // 从当前View中移除旧的监听器（防止重复添加）
                        currentView.removeListener(listener)
                        android.util.Log.d(
                            TAG,
                            "Removed PAGView listener from current view (key=$targetKey)"
                        )
                    }

                    listener is PAGImageView.PAGImageViewListener && currentView is PAGImageView -> {
                        // 从当前View中移除旧的监听器（防止重复添加）
                        currentView.removeListener(listener)
                        android.util.Log.d(
                            TAG,
                            "Removed PAGImageView listener from current view (key=$targetKey)"
                        )
                    }
                }
            }
        } else if (cached != null) {
            // 如果没有currentView，从oldView中移除（清理场景）
            val (oldView, listeners) = cached
            listeners.forEach { listener ->
                when {
                    listener is PAGView.PAGViewListener && oldView is PAGView -> {
                        oldView.removeListener(listener)
                        android.util.Log.d(TAG, "Removed PAGView listener from old view")
                    }

                    listener is PAGImageView.PAGImageViewListener && oldView is PAGImageView -> {
                        oldView.removeListener(listener)
                        android.util.Log.d(TAG, "Removed PAGImageView listener from old view")
                    }
                }
            }
        }
    }

    /**
     * 设置Lottie动画监听器
     */
    private fun setupLottieListeners(
        targetKey: String,
        lottieDrawable: LottieDrawable,
        view: View?,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // 移除旧的适配器（如果存在）
        removeOldLottieAdapter(targetKey, view, lottieDrawable)

        // 从 target 获取 retainLastFrame 配置
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // 默认保留
        }

        val lottieView = view as? LottieAnimationView
        val adapter = LottiePlayListenerAdapter(listener, lottieView, retainLastFrame)
        
        // 获取用户的原始 repeatCount（总播放次数）
        val userRepeatCount = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.repeatCount ?: -1
            }
            else -> -1  // 默认无限循环
        }
        
        // 传递给 adapter：用户期望的总播放次数
        // 注意：LottieViewTarget 会将用户的 repeatCount 转换为 Lottie 的语义
        // 但 adapter 需要知道用户期望的总播放次数，而不是 Lottie 的 repeatCount
        val animatorListener = adapter.createAnimatorListener(userRepeatCount)

        if (lottieView != null) {
            lottieView.addAnimatorListener(animatorListener)
        } else {
            lottieDrawable.addAnimatorListener(animatorListener)
        }

        adapterCache[targetKey] = Pair(view, listOf(animatorListener))
    }

    /**
     * 移除旧的Lottie监听器
     */
    private fun removeOldLottieAdapter(
        targetKey: String,
        currentView: View?,
        currentDrawable: LottieDrawable
    ) {
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            val (oldView, listeners) = cached
            listeners.forEach { listener ->
                if (listener is android.animation.Animator.AnimatorListener) {
                    // 从旧的View或Drawable中移除
                    when {
                        oldView is LottieAnimationView -> {
                            oldView.removeAnimatorListener(listener)
                        }
                        // 如果新旧View相同，也需要移除（防止重复添加）
                        currentView is LottieAnimationView && oldView == currentView -> {
                            currentView.removeAnimatorListener(listener)
                        }

                        else -> {
                            // 尝试从Drawable中移除
                            try {
                                currentDrawable.removeAnimatorListener(listener)
                            } catch (e: Exception) {
                                // Drawable可能不支持移除，忽略
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置SVGA动画监听器
     * 注意：SVGA内部使用Animator，需要通过反射获取内部的Animator来添加监听器
     */
    @SuppressLint("LongLogTag")
    private fun setupSVGAListeners(
        targetKey: String,
        view: View?,
        listener: AnimationPlayListener
    ) {
        if (view !is SVGAImageView) {
            return
        }
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            view.callback = null
        }
        val adapter = SVGAPlayListenerAdapter(listener)
        val animationListener = adapter.createAnimatorListener()
        view.callback = animationListener
        adapterCache[targetKey] = Pair(null, listOf(animationListener))
    }


    /**
     * 设置VAP动画监听器
     */
    private fun setupVAPListeners(
        targetKey: String,
        view: AnimView,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // 移除旧的适配器（如果存在）
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            view.setAnimListener(null)
        }

        // 从 target 获取 retainLastFrame 配置
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // 默认保留
        }

        val adapter = VapPlayListenerAdapter(listener, view, retainLastFrame)
        val animationListener = adapter.createAnimatorListener()
        view.setAnimListener(animationListener)
        adapterCache[targetKey] = Pair(null, listOf(animationListener))
    }


    /**
     * 设置GIF动画监听器
     */
    private fun setupGifListeners(
        targetKey: String,
        gifDrawable: GifDrawable,
        view: View?,
        listener: AnimationPlayListener,
        target: Any? = null
    ) {
        // 移除旧的适配器（如果存在）
        removeOldGifAdapter(targetKey, gifDrawable)

        // 从 target 获取 retainLastFrame 配置
        val retainLastFrame = when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                target.animationOptions?.retainLastFrame ?: true
            }

            else -> true // 默认保留
        }

        val adapter =
            GifPlayListenerAdapter(listener, view as? GifImageView, retainLastFrame)
        val animationListener = adapter.createAnimatorListener(gifDrawable.loopCount)
        gifDrawable.addAnimationListener(animationListener)

        adapterCache[targetKey] = Pair(null, listOf(animationListener))
    }

    /**
     * 移除旧的GIF监听器
     */
    private fun removeOldGifAdapter(targetKey: String, currentDrawable: GifDrawable) {
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            val (_, listeners) = cached
            listeners.forEach { listener ->
                if (listener is AnimationListener) {
                    try {
                        currentDrawable.removeAnimationListener(listener)
                    } catch (e: Exception) {
                        // 忽略移除失败的情况
                    }
                }
            }
        }
    }


    /**
     * 清理指定target的适配器
     * 需要真正从View/Drawable中移除监听器，而不是只从缓存中移除
     */
    fun cleanup(target: Any) {
        val targetKey = getTargetKey(target)
        val cached = adapterCache.remove(targetKey)

        // 从实际的View/Drawable中移除监听器
        if (cached != null) {
            val (view, listeners) = cached
            listeners.forEach { listener ->
                when {
                    // PAG监听器
                    listener is PAGView.PAGViewListener && view is PAGView -> {
                        view.removeListener(listener)
                    }

                    listener is PAGImageView.PAGImageViewListener && view is PAGImageView -> {
                        view.removeListener(listener)
                    }
                    // Lottie监听器
                    listener is android.animation.Animator.AnimatorListener && view is LottieAnimationView -> {
                        view.removeAnimatorListener(listener)
                    }
                    // SVGA监听器（需要反射）
                    listener is android.animation.Animator.AnimatorListener && view is SVGAImageView -> {
                        try {
                            val animatorField =
                                SVGAImageView::class.java.getDeclaredField("mAnimator")
                            animatorField.isAccessible = true
                            val animator =
                                animatorField.get(view) as? android.animation.ValueAnimator
                            animator?.removeListener(listener)
                        } catch (e: Exception) {
                            // 忽略反射失败
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取target的唯一标识（用于cleanup等场景）
     * 优先使用View，如果没有View才使用target
     */
    private fun getTargetKey(target: Any): String {
        return when (target) {
            is CustomViewAnimationTarget<*, *> -> {
                // ViewTarget始终有View，使用View作为key
                target.getViewForVisibilityCheck().hashCode().toString()
            }

            is CustomAnimationTarget<*> -> {
                // CustomAnimationTarget：如果是AutoAnimationFrameLayoutTarget，尝试获取container
                // 其他情况使用target作为key（真正的自定义实现）
                try {
                    val field = target.javaClass.getDeclaredField("container")
                    field.isAccessible = true
                    val container = field.get(target)
                    if (container is View) {
                        container.hashCode().toString()
                    } else {
                        target.hashCode().toString()
                    }
                } catch (e: Exception) {
                    // 没有container字段，使用target作为key
                    target.hashCode().toString()
                }
            }

            else -> target.hashCode().toString()
        }
    }

}

