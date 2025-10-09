package com.kernelflux.aniflux.manager

import android.content.Context

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
interface AnimationConnectivityMonitorFactory {
    fun build(
        context: Context,
        listener: AnimationConnectivityMonitor.AnimationConnectivityListener
    ): AnimationConnectivityMonitor
}