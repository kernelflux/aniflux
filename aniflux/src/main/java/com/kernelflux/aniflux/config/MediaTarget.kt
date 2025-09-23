package com.kernelflux.aniflux.config

import android.content.Context
import com.kernelflux.aniflux.request.MediaRequest

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体资源Target接口
 * 用于处理不同类型的媒体资源（动画、静态图片等）
 */
interface MediaTarget {
    fun getContext(): Context
    fun onLoadStarted()
    fun onLoadSuccess(resource: Any)
    fun onLoadFail(error: MediaError)
    fun onLoadCleared()

    fun setRequest(request: MediaRequest?)
    fun getRequest(): MediaRequest?

}
