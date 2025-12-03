package com.kernelflux.aniflux.svga

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoadCallback
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap
import com.kernelflux.svga.SVGADynamicEntity
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.svga.SVGAVideoEntity

/**
 * SVGA 占位图管理器
 *
 * @author: kerneflux
 * @date: 2025/11/27
 */
class SVGAPlaceholderManager(
    view: View,
    private val drawable: SVGADrawable,
    replacements: PlaceholderReplacementMap,
    imageLoader: PlaceholderImageLoader,
    lifecycle: Lifecycle?
) : PlaceholderManager(view, drawable, replacements, imageLoader, lifecycle) {

    private val mainHandler = getMainHandler()
    private val dynamicEntity = SVGADynamicEntity()
    private var appliedCount = 0
    private val totalCount = replacements.getAll().size
    private var pendingUpdate = false
    private var videoItem: SVGAVideoEntity? = null
    private var wasAnimating = false

    override fun applyReplacements() {
        videoItem = drawable.videoItem
        // 安全检查：如果View类型不匹配，直接返回，不抛出异常
        val svgaView = view as? SVGAImageView ?: return

        if (totalCount == 0) return

        // 保存当前播放状态
        wasAnimating = try {
            svgaView.isAnimating
        } catch (e: Exception) {
            false
        }

        // 加载所有占位图
        replacements.getAll().forEach { (key, replacement) ->
            try {
                val request = imageLoader.load(
                    context = view.context,
                    source = replacement.imageSource,
                    width = 0,  // SVGA会根据占位符原始尺寸处理
                    height = 0,
                    callback = object : PlaceholderImageLoadCallback {
                        override fun onSuccess(bitmap: Bitmap) {
                            if (isCleared) return

                            // 在主线程更新
                            mainHandler.post {
                                if (isCleared) return@post

                                try {
                                    // 再次检查view是否有效
                                    val currentView = view as? SVGAImageView ?: return@post
                                    // 设置占位图
                                    dynamicEntity.setDynamicImage(bitmap, key)
                                    appliedCount++
                                    pendingUpdate = true

                                    // 如果所有占位图都加载完成，立即更新
                                    if (appliedCount >= totalCount) {
                                        updateViewImmediately(currentView, wasAnimating)
                                    } else {
                                        // 部分加载完成，延迟批量更新（避免频繁调用 setVideoItem）
                                        mainHandler.postDelayed({
                                            if (!isCleared && pendingUpdate) {
                                                updateViewImmediately(currentView, wasAnimating)
                                                pendingUpdate = false
                                            }
                                        }, 50) // 50ms 内的更新会合并
                                    }
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

        // 先设置一次（可能有些占位图已经加载完成或使用缓存）
        try {
            svgaView.setVideoItem(videoItem, dynamicEntity)

            // 如果之前正在播放，重新启动动画
            if (wasAnimating) {
                try {
                    svgaView.startAnimation()
                } catch (e: Exception) {
                    // 忽略启动动画失败
                }
            }
        } catch (e: Exception) {
            // 忽略设置失败的错误
        }
    }

    /**
     * 立即更新视图（批量更新，减少 setVideoItem 调用次数）
     */
    private fun updateViewImmediately(view: SVGAImageView, shouldRestartAnimation: Boolean) {
        val currentVideoItem = videoItem ?: return
        try {
            view.setVideoItem(currentVideoItem, dynamicEntity)

            // 如果之前正在播放，重新启动动画
            if (shouldRestartAnimation) {
                try {
                    view.startAnimation()
                } catch (e: Exception) {
                    // 忽略启动动画失败
                }
            }
        } catch (e: Exception) {
            // 忽略设置失败的错误
        }
    }

    override fun onCleared() {
        super.onCleared()
        appliedCount = 0
        pendingUpdate = false
    }
}

