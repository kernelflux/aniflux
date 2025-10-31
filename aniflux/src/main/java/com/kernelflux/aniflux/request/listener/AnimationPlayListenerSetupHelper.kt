package com.kernelflux.aniflux.request.listener

import android.annotation.SuppressLint
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGAImageView
import org.libpag.PAGFile
import org.libpag.PAGImageView
import org.libpag.PAGView
import pl.droidsonroids.gif.GifDrawable
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * 动画播放监听器设置辅助类
 * 负责将统一的监听器设置到具体的动画对象上
 *
 * @author: kerneflux
 * @date: 2025/01/XX
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
                setupPAGListeners(targetKey, view, listener)
            }

            is LottieDrawable -> {
                setupLottieListeners(targetKey, resource, view, listener)
            }

            is SVGADrawable -> {
                setupSVGAListeners(targetKey, view, listener)
            }

            is GifDrawable -> {
                setupGifListeners(targetKey, resource, listener)
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
        listener: AnimationPlayListener
    ) {
        // 移除旧的适配器（如果存在）- 必须从View中真正移除
        removeOldPAGAdapter(targetKey, view)

        if (view == null) {
            android.util.Log.w(TAG, "PAG listener setup failed: view is null")
            return
        }

        when (view) {
            is PAGView -> {
                val adapter = PAGPlayListenerAdapter(listener)
                val pagListener = adapter.createPAGViewListener()
                view.addListener(pagListener)
                adapterCache[targetKey] = Pair(view, listOf(pagListener))
                android.util.Log.d(TAG, "PAGView listener set")
            }

            is PAGImageView -> {
                val adapter = PAGPlayListenerAdapter(listener)
                val pagListener = adapter.createPAGImageViewListener()
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
                    listener is org.libpag.PAGView.PAGViewListener && currentView is PAGView -> {
                        // 从当前View中移除旧的监听器（防止重复添加）
                        currentView.removeListener(listener)
                        android.util.Log.d(
                            TAG,
                            "Removed PAGView listener from current view (key=$targetKey)"
                        )
                    }

                    listener is org.libpag.PAGImageView.PAGImageViewListener && currentView is PAGImageView -> {
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
                    listener is org.libpag.PAGView.PAGViewListener && oldView is PAGView -> {
                        oldView.removeListener(listener)
                        android.util.Log.d(TAG, "Removed PAGView listener from old view")
                    }

                    listener is org.libpag.PAGImageView.PAGImageViewListener && oldView is PAGImageView -> {
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
        listener: AnimationPlayListener
    ) {
        // 移除旧的适配器（如果存在）
        removeOldLottieAdapter(targetKey, view, lottieDrawable)

        val adapter = LottiePlayListenerAdapter(listener)
        val animatorListener = adapter.createAnimatorListener()

        if (view is LottieAnimationView) {
            view.addAnimatorListener(animatorListener)
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
        // 移除旧的适配器（如果存在）
        removeOldSVGAAdapter(targetKey, view)

        if (view !is SVGAImageView) {
            return
        }

        try {
            // 通过反射获取SVGAImageView内部的mAnimator字段
            val animatorField = SVGAImageView::class.java.getDeclaredField("mAnimator")
            animatorField.isAccessible = true

            // 创建监听器并添加到Animator
            val adapter = SVGAPlayListenerAdapter(listener)
            val animatorListener = adapter.createAnimatorListener()

            // 注意：SVGA的Animator在startAnimation()时创建
            // 如果此时Animator为null，我们保存适配器，稍后重试设置
            val animator = animatorField.get(view) as? android.animation.ValueAnimator
            if (animator != null) {
                animator.addListener(animatorListener)
            } else {
                // Animator还未创建，保存适配器以便后续设置
                android.util.Log.d(TAG, "SVGA Animator not created yet, adapter saved for later")
            }

            // 保存适配器引用和view以便后续清理和重试设置
            adapterCache[targetKey] = Pair(view, listOf(animatorListener))

            // 尝试延迟设置：如果Animator还未创建，提供一个方法在动画开始后重试
            if (animator == null) {
                // 提供延迟设置：在view的post中尝试设置
                view.post {
                    try {
                        val delayedAnimator =
                            animatorField.get(view) as? android.animation.ValueAnimator
                        if (delayedAnimator != null) {
                            delayedAnimator.addListener(animatorListener)
                            android.util.Log.d(
                                TAG,
                                "SVGA listeners set successfully after animation started"
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Failed to set SVGA listeners in post", e)
                    }
                }
            }

        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to setup SVGA listeners via reflection", e)
        }
    }

    /**
     * 移除旧的SVGA监听器
     */
    @SuppressLint("LongLogTag")
    private fun removeOldSVGAAdapter(targetKey: String, currentView: View?) {
        val cached = adapterCache.remove(targetKey)
        if (cached != null) {
            val (oldView, listeners) = cached
            if (oldView is SVGAImageView) {
                try {
                    val animatorField = SVGAImageView::class.java.getDeclaredField("mAnimator")
                    animatorField.isAccessible = true
                    val animator = animatorField.get(oldView) as? android.animation.ValueAnimator
                    listeners.forEach { listener ->
                        if (listener is android.animation.Animator.AnimatorListener && animator != null) {
                            animator.removeListener(listener)
                            android.util.Log.d(TAG, "Removed SVGA listener from old view")
                        }
                        // 如果新旧View相同，也需要从当前View中移除
                        if (currentView is SVGAImageView && oldView == currentView && animator != null) {
                            animator.removeListener(
                                listener as? android.animation.Animator.AnimatorListener
                                    ?: return@forEach
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Failed to remove SVGA listeners", e)
                }
            }
        }
    }

    /**
     * 设置GIF动画监听器
     */
    private fun setupGifListeners(
        targetKey: String,
        gifDrawable: GifDrawable,
        listener: AnimationPlayListener
    ) {
        // 移除旧的适配器（如果存在）
        removeOldGifAdapter(targetKey, gifDrawable)

        val adapter = GifPlayListenerAdapter(listener)
        val animationListener = adapter.createAnimationListener()
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
                if (listener is pl.droidsonroids.gif.AnimationListener) {
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

