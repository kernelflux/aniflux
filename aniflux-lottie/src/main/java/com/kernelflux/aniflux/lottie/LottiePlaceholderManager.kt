package com.kernelflux.aniflux.lottie

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoadCallback
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.lottie.LottieDrawable

/**
 * Lottie 占位图管理器
 * 使用 Lottie 的 ImageAssetDelegate 机制来替换图片
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
class LottiePlaceholderManager(
    view: View,
    private val drawable: LottieDrawable,
    replacements: PlaceholderReplacementMap,
    imageLoader: PlaceholderImageLoader,
    lifecycle: Lifecycle?
) : PlaceholderManager(view, drawable, replacements, imageLoader, lifecycle) {
    
    private val mainHandler = getMainHandler()
    private val lottieView: LottieAnimationView? = view as? LottieAnimationView
    private val loadedBitmaps = mutableMapOf<String, Bitmap>()
    
    override fun applyReplacements() {
        // 安全检查：如果View类型不匹配，直接返回，不抛出异常
        val lottieView = this.lottieView ?: return
        val composition = drawable.composition ?: return
        
        try {
            // 设置 ImageAssetDelegate，用于动态提供图片
            lottieView.setImageAssetDelegate { asset ->
                try {
                    val key = asset.id
                    // 如果已加载，直接返回
                    loadedBitmaps[key]
                } catch (e: Exception) {
                    // 忽略异常，返回null
                    null
                }
            }
        } catch (e: Exception) {
            // 如果设置失败，直接返回，不处理占位图
            return
        }
        
        // 异步加载所有占位图
        replacements.getAll().forEach { (key, replacement) ->
            try {
                val imageAsset = composition.images[key] ?: return@forEach
                
                val request = imageLoader.load(
                    context = view.context,
                    source = replacement.imageSource,
                    width = imageAsset.width,
                    height = imageAsset.height,
                    callback = object : PlaceholderImageLoadCallback {
                        override fun onSuccess(bitmap: Bitmap) {
                            if (isCleared) return
                            
                            // 在主线程更新
                            mainHandler.post {
                                if (isCleared) return@post
                                
                                try {
                                    // 再次检查view是否有效
                                    val currentView = this@LottiePlaceholderManager.lottieView
                                    // 保存到缓存
                                    loadedBitmaps[key] = bitmap
                                    
                                    // 更新 ImageAssetDelegate（重新设置以触发刷新）
                                    currentView.setImageAssetDelegate { asset ->
                                        try {
                                            loadedBitmaps[asset.id]
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    
                                    // 刷新视图
                                    currentView.invalidate()
                                } catch (e: Exception) {
                                    // 忽略设置失败的错误，不影响主流程
                                }
                            }
                        }
                        
                        override fun onError(error: Throwable) {
                            // 错误处理：静默失败，不影响动画播放
                        }
                    }
                )
                activeRequests.add(request)
            } catch (e: Exception) {
                // 单个占位图加载失败不影响其他占位图
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        loadedBitmaps.clear()
        // 清除 ImageAssetDelegate
        try {
            lottieView?.setImageAssetDelegate(null)
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
    }
}

