package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
class ApplicationAnimationLifecycle : AnimationLifecycle {
    override fun addListener(listener: AnimationLifecycleListener) {
        listener.onStart()
    }

    override fun removeListener(listener: AnimationLifecycleListener) {
    }

}