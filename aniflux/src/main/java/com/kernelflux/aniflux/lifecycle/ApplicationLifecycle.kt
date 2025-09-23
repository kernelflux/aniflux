package com.kernelflux.aniflux.lifecycle

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 应用级生命周期 - 始终处于活跃状态
 */
class ApplicationLifecycle : MediaLifecycle {
    private val listeners = mutableSetOf<MediaLifecycleListener>()

    override fun addListener(listener: MediaLifecycleListener) {
        listeners.add(listener)
        // 应用级生命周期始终是活跃的
        listener.onStart()
    }

    override fun removeListener(listener: MediaLifecycleListener) {
        listeners.remove(listener)
    }
}