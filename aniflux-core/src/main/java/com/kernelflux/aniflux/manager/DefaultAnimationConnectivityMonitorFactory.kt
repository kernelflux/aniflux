package com.kernelflux.aniflux.manager

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
class DefaultAnimationConnectivityMonitorFactory : AnimationConnectivityMonitorFactory {
    companion object {
        private const val NETWORK_PERMISSION = "android.permission.ACCESS_NETWORK_STATE"
    }

    override fun build(
        context: Context,
        listener: AnimationConnectivityMonitor.AnimationConnectivityListener
    ): AnimationConnectivityMonitor {
        val permissionResult = ContextCompat.checkSelfPermission(context, NETWORK_PERMISSION)
        val hasPermission = permissionResult == PackageManager.PERMISSION_GRANTED
        return if (hasPermission) DefaultAnimationConnectivityMonitor(
            context,
            listener
        ) else NullAnimationConnectivityMonitor()
    }
}