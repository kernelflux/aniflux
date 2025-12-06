package com.kernelflux.anifluxsample.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoadCallback
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoadRequest
import java.io.File

/**
 * 基于 Glide 的占位图加载器实现
 * 
 * 支持的图片源类型：
 * - String: URL 或 "asset://xxx.jpg" (assets 资源)
 * - File: 本地文件
 * - Int: Resource ID (如 R.drawable.xxx)
 * - android.net.Uri: Uri 对象
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
class GlidePlaceholderImageLoader : PlaceholderImageLoader {
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    @SuppressLint("CheckResult")
    override fun load(
        context: Context,
        source: Any,
        width: Int,
        height: Int,
        callback: PlaceholderImageLoadCallback
    ): PlaceholderImageLoadRequest {
        val request = GlidePlaceholderRequest()
        
        // 特殊处理 assets 资源（Glide 不支持直接加载 assets）
        if (source is String && source.startsWith("asset://")) {
            val assetPath = source.removePrefix("asset://")
            // 在后台线程加载 assets
            Thread {
                try {
                    val inputStream = context.assets.open(assetPath)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap != null) {
                        mainHandler.post {
                            if (!request.isCancelled()) {
                                callback.onSuccess(bitmap)
                            }
                        }
                    } else {
                        mainHandler.post {
                            if (!request.isCancelled()) {
                                callback.onError(RuntimeException("Failed to decode asset: $assetPath"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        if (!request.isCancelled()) {
                            callback.onError(e)
                        }
                    }
                }
            }.start()
            return request
        }
        
        // 构建 Glide 请求
        val requestBuilder = when (source) {
            is String -> {
                // URL 或其他字符串
                Glide.with(context)
                    .asBitmap()
                    .load(source)
            }
            is File -> {
                Glide.with(context)
                    .asBitmap()
                    .load(source)
            }
            is Int -> {
                Glide.with(context)
                    .asBitmap()
                    .load(source)
            }
            is android.net.Uri -> {
                Glide.with(context)
                    .asBitmap()
                    .load(source)
            }
            else -> {
                // 不支持的源类型，直接返回错误
                mainHandler.post {
                    callback.onError(IllegalArgumentException("Unsupported image source type: ${source.javaClass.name}"))
                }
                return request
            }
        }
        
        // 设置尺寸（如果指定了）
        if (width > 0 && height > 0) {
            requestBuilder.override(width, height)
        }
        
        // 使用 submit() 在后台线程加载，然后获取结果
        Thread {
            try {
                val futureTarget = requestBuilder.submit()
                val bitmap = futureTarget.get()
                if (!request.isCancelled()) {
                    if (bitmap != null) {
                        mainHandler.post {
                            if (!request.isCancelled()) {
                                callback.onSuccess(bitmap)
                            }
                        }
                    } else {
                        mainHandler.post {
                            if (!request.isCancelled()) {
                                callback.onError(RuntimeException("Bitmap is null"))
                            }
                        }
                    }
                }
                // 清理 FutureTarget
                Glide.with(context).clear(futureTarget)
            } catch (e: Exception) {
                if (!request.isCancelled()) {
                    mainHandler.post {
                        callback.onError(e)
                    }
                }
            }
        }.start()
        
        return request
    }
    
    override fun cancel(request: PlaceholderImageLoadRequest) {
        if (request is GlidePlaceholderRequest) {
            request.cancel()
        }
    }
    
    /**
     * Glide 占位图加载请求实现
     */
    private class GlidePlaceholderRequest : PlaceholderImageLoadRequest {
        @Volatile
        private var cancelled = false
        
        override fun isCancelled(): Boolean = cancelled
        
        override fun cancel() {
            cancelled = true
        }
    }
}

