package com.kernelflux.aniflux.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 将AndroidX Lifecycle适配到MediaLifecycle
 */
class MediaLifecycleAdapter(
    private val androidXLifecycle: Lifecycle
) : MediaLifecycle, DefaultLifecycleObserver {

    private val listeners = CopyOnWriteArraySet<MediaLifecycleListener>()

    init {
        androidXLifecycle.addObserver(this)
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
        owner.lifecycle.removeObserver(this)
    }

    override fun addListener(listener: MediaLifecycleListener) {
        listeners.add(listener)

        // 根据AndroidX Lifecycle的当前状态决定调用哪个方法
        when (androidXLifecycle.currentState) {
            Lifecycle.State.DESTROYED -> listener.onDestroy()
            Lifecycle.State.STARTED, Lifecycle.State.RESUMED -> listener.onStart()
            else -> listener.onStop()
        }
    }

    override fun removeListener(listener: MediaLifecycleListener) {
        listeners.remove(listener)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaLifecycleAdapter
        return androidXLifecycle == other.androidXLifecycle
    }

    override fun hashCode(): Int {
        return androidXLifecycle.hashCode()
    }

}