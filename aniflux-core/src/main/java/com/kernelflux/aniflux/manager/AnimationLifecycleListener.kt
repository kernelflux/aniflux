package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 * Animation lifecycle listener interface
 * Provides concise lifecycle event callbacks
 */
interface AnimationLifecycleListener {

    /**
     * Lifecycle start (corresponds to Activity/Fragment's onStart)
     * Should resume animation requests at this time
     */
    fun onStart()

    /**
     * Lifecycle stop (corresponds to Activity/Fragment's onStop)
     * Should pause animation requests at this time
     */
    fun onStop()

    /**
     * Lifecycle destroy (corresponds to Activity/Fragment's onDestroy)
     * Should clean up all resources at this time
     */
    fun onDestroy()
}