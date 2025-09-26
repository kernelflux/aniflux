package com.kernelflux.aniflux.config

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.lifecycle.MediaLifecycleListener
import com.kernelflux.aniflux.request.MediaRequest
import com.kernelflux.aniflux.request.target.MediaSizeReadyCallback

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体资源Target接口
 * 用于处理不同类型的媒体资源（动画、静态图片等）
 */
interface MediaTarget<R> : MediaLifecycleListener {

    companion object {
        const val SIZE_ORIGINAL: Int = Int.Companion.MIN_VALUE
    }

    fun onLoadStarted(placeholder: Drawable?)
    fun onLoadFailed(errorDrawable: Drawable?)
    fun onResourceReady(resource: R)
    fun onLoadCleared(placeholder: Drawable?)
    fun getSize(cb: MediaSizeReadyCallback)
    fun removeCallback(cb: MediaSizeReadyCallback)
    fun setRequest(request: MediaRequest?)
    fun getRequest(): MediaRequest?

}
