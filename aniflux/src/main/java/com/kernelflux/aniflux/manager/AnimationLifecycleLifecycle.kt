package com.kernelflux.aniflux.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 包装AndroidX Lifecycle的AnimationLifecycleLifecycle
 * 实现自定义AnimationLifecycle和LifecycleObserver接口
 */
class AnimationLifecycleLifecycle(
    lifecycle: androidx.lifecycle.Lifecycle
) : AnimationLifecycle, DefaultLifecycleObserver {

    private val listeners = CopyOnWriteArrayList<AnimationLifecycleListener>()

    init {
        lifecycle.addObserver(this)
    }


    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        listeners.forEach { it.onStart() }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        listeners.forEach { it.onStop() }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        listeners.forEach { it.onDestroy() }
        listeners.clear()
    }

    // 自定义AnimationLifecycle接口实现
    override fun addListener(listener: AnimationLifecycleListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: AnimationLifecycleListener) {
        listeners.remove(listener)
    }
}
