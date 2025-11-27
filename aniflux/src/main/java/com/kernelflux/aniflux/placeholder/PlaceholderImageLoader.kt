package com.kernelflux.aniflux.placeholder

import android.content.Context
import android.graphics.Bitmap

/**
 * 占位图加载器接口
 * 业务方需要实现此接口，可以使用任何图片加载框架（Glide、Coil、Picasso等）
 * 
 * 框架只负责调用此接口，不关心具体实现
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
interface PlaceholderImageLoader {
    
    /**
     * 加载图片
     * 
     * @param context 上下文
     * @param source 图片源（可以是URL、File、Uri、ResourceId等，由业务方定义）
     * @param width 目标宽度（0表示不限制，使用原始尺寸）
     * @param height 目标高度（0表示不限制，使用原始尺寸）
     * @param callback 加载回调（在主线程调用）
     * @return 加载请求（用于取消）
     */
    fun load(
        context: Context,
        source: Any,
        width: Int = 0,
        height: Int = 0,
        callback: PlaceholderImageLoadCallback
    ): PlaceholderImageLoadRequest
    
    /**
     * 取消加载请求
     * 
     * @param request 要取消的请求
     */
    fun cancel(request: PlaceholderImageLoadRequest)
}

/**
 * 图片加载回调
 * 所有回调都在主线程执行
 */
interface PlaceholderImageLoadCallback {
    /**
     * 加载成功
     * 
     * @param bitmap 加载的Bitmap
     * 注意：框架会负责管理Bitmap的生命周期，业务方不需要调用recycle()
     * 但如果业务方需要长期持有Bitmap，应该创建副本
     */
    fun onSuccess(bitmap: Bitmap)
    
    /**
     * 加载失败
     * 
     * @param error 错误信息
     */
    fun onError(error: Throwable)
}

/**
 * 图片加载请求（用于取消）
 */
interface PlaceholderImageLoadRequest {
    /**
     * 是否已取消
     */
    fun isCancelled(): Boolean
    
    /**
     * 取消请求
     * 取消后，回调不会被调用
     */
    fun cancel()
}

