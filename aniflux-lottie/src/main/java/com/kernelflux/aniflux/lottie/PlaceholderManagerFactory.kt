package com.kernelflux.aniflux.lottie

import android.view.View
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap
import com.kernelflux.lottie.LottieDrawable

/**
 * Lottie 占位图管理器工厂
 * 提供创建 LottiePlaceholderManager 的工厂方法
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
object PlaceholderManagerFactory {
    /**
     * 创建 Lottie 占位图管理器
     * 
     * @param view 显示动画的 View
     * @param resource LottieDrawable 资源
     * @param replacements 占位图替换映射
     * @param imageLoader 图片加载器
     * @param lifecycle 生命周期（可选）
     * @return PlaceholderManager 实例，如果参数无效返回 null
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
            is LottieDrawable -> {
                LottiePlaceholderManager(view, resource, replacements, imageLoader, lifecycle)
            }
            else -> null
        }
    }
}

