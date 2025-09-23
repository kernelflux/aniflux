package com.kernelflux.aniflux.lifecycle

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *  生命周期接口
 */
interface MediaLifecycle {
    fun addListener(listener: MediaLifecycleListener)
    fun removeListener(listener: MediaLifecycleListener)
}

/**
 * 生命周期监听器
 */
interface MediaLifecycleListener {
    fun onStart()
    fun onStop()
    fun onDestroy()
}