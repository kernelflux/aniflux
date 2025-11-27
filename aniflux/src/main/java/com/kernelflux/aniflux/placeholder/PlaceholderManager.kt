package com.kernelflux.aniflux.placeholder

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.svga.SVGADrawable
import org.libpag.PAGFile

/**
 * 占位图管理器基类
 * 负责管理占位图的加载和应用
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
abstract class PlaceholderManager protected constructor(
    protected val view: View,
    protected val resource: Any,
    protected val replacements: PlaceholderReplacementMap,
    protected val imageLoader: PlaceholderImageLoader,
    protected val lifecycle: Lifecycle?
) {
    protected val activeRequests = mutableListOf<PlaceholderImageLoadRequest>()
    protected var isCleared = false
    private var lifecycleObserver: LifecycleEventObserver? = null
    
    companion object {
        // 共享的主线程 Handler，避免每个 Manager 都创建新的 Handler
        @JvmStatic
        private val MAIN_HANDLER = android.os.Handler(android.os.Looper.getMainLooper())
        
        /**
         * 获取共享的主线程 Handler
         */
        @JvmStatic
        fun getMainHandler(): android.os.Handler = MAIN_HANDLER


        /**
         * 创建占位图管理器
         * 根据资源类型自动选择对应的管理器
         */
        @JvmStatic
        fun create(
            view: View,
            resource: Any,
            replacements: PlaceholderReplacementMap,
            imageLoader: PlaceholderImageLoader?,
            lifecycle: Lifecycle?
        ): PlaceholderManager? {
            if (imageLoader == null || replacements.isEmpty()) {
                return null
            }

            return when (resource) {
                is SVGADrawable -> {
                    SVGAPlaceholderManager(view, resource, replacements, imageLoader, lifecycle)
                }

                is PAGFile -> {
                    PAGPlaceholderManager(view, resource, replacements, imageLoader, lifecycle)
                }

                is LottieDrawable -> {
                    LottiePlaceholderManager(view, resource, replacements, imageLoader, lifecycle)
                }

                else -> null
            }
        }

    }

    init {
        // 监听生命周期，自动清理
        try {
            lifecycle?.addObserver(LifecycleEventObserver { source, event ->
                try {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        clear()
                    }
                } catch (e: Exception) {
                    // 忽略生命周期回调中的异常
                }
            }.also { lifecycleObserver = it })
        } catch (e: Exception) {
            // 忽略添加生命周期监听器的异常
        }
    }

    /**
     * 应用占位图替换
     * 子类实现具体的替换逻辑
     */
    abstract fun applyReplacements()

    /**
     * 清理资源
     */
    fun clear() {
        if (isCleared) return

        isCleared = true

        // 取消所有加载请求
        activeRequests.forEach { request ->
            try {
                imageLoader.cancel(request)
            } catch (e: Exception) {
                // 忽略取消时的异常
            }
        }
        activeRequests.clear()

        // 移除生命周期监听
        try {
            lifecycleObserver?.let { observer ->
                lifecycle?.removeObserver(observer)
            }
        } catch (e: Exception) {
            // 忽略移除监听器时的异常
        }
        lifecycleObserver = null

        // 子类可以重写此方法进行额外清理
        onCleared()
    }

    /**
     * 子类可以重写此方法进行额外清理
     */
    protected open fun onCleared() {
        // 默认空实现
    }
}

