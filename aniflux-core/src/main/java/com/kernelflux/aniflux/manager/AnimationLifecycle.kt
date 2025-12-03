package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 * 自定义动画生命周期接口
 */
interface AnimationLifecycle {

    /**
     * 添加生命周期监听器到当前AnimationLifecycle实现管理的监听器集合中
     */
    fun addListener( listener: AnimationLifecycleListener)

    /**
     * 从当前AnimationLifecycle实现管理的监听器集合中移除指定的监听器
     *
     * @param listener 要移除的监听器
     * @return 如果监听器被成功移除返回true，否则返回false
     *
     * 这是一个优化方法，不保证每个添加的监听器最终都会被移除。
     * 可以安全地多次调用此方法，移除后的监听器仍可继续使用。
     */
    fun removeListener(listener: AnimationLifecycleListener)
}
