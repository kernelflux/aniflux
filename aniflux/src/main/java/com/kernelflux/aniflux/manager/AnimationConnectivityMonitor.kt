package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
interface AnimationConnectivityMonitor : AnimationLifecycleListener {
    interface AnimationConnectivityListener {
        fun onConnectivityChanged(isConnected: Boolean)
    }
}
