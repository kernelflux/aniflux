package com.kernelflux.aniflux.manager

import android.content.Context

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
class DefaultAnimationConnectivityMonitor(
    private val context: Context,
    val listener: AnimationConnectivityMonitor.AnimationConnectivityListener
) : AnimationConnectivityMonitor {

    private fun register() {
        SingletonConnectivityReceiver.get(context).register(listener)
    }

    private fun unregister(){
        SingletonConnectivityReceiver.get(context).unregister(listener)
    }

    override fun onStart() {
        register()
    }

    override fun onStop() {
        unregister()
    }

    override fun onDestroy() {
        // Do nothing.
    }
}