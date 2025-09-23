package com.kernelflux.aniflux.request

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *  媒体请求接口
 */
interface MediaRequest {
    fun begin()
    fun pause()
    fun resume()
    fun clear()
    fun isRunning(): Boolean
    fun isComplete(): Boolean
    fun isCleared(): Boolean
    fun isFailed(): Boolean
    fun isPaused(): Boolean

}