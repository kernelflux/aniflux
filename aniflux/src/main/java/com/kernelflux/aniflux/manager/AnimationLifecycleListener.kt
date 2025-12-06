package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 * Animation lifecycle listener interface
 * Provides concise lifecycle event callbacks
 */
interface AnimationLifecycleListener {

    /**
     * Lifecycle started (corresponds to Activity/Fragment's onStart)
     * Animation requests should be resumed at this point
     */
    fun onStart()

    /**
     * Lifecycle stopped (corresponds to Activity/Fragment's onStop)
     * Animation requests should be paused at this point
     */
    fun onStop()

    /**
     * Lifecycle destroyed (corresponds to Activity/Fragment's onDestroy)
     * All resources should be cleaned up at this point
     */
    fun onDestroy()
}