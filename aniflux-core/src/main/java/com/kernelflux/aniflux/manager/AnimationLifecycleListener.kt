package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 * 动画生命周期监听器接口
 * 提供简洁的生命周期事件回调
 */
interface AnimationLifecycleListener {

    /**
     * 生命周期开始（对应Activity/Fragment的onStart）
     * 此时应该恢复动画请求
     */
    fun onStart()

    /**
     * 生命周期停止（对应Activity/Fragment的onStop）
     * 此时应该暂停动画请求
     */
    fun onStop()

    /**
     * 生命周期销毁（对应Activity/Fragment的onDestroy）
     * 此时应该清理所有资源
     */
    fun onDestroy()
}