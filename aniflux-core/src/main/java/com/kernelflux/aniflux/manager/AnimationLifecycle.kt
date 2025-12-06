package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 * Custom animation lifecycle interface
 */
interface AnimationLifecycle {

    /**
     * Add lifecycle listener to the listener collection managed by current AnimationLifecycle implementation
     */
    fun addListener( listener: AnimationLifecycleListener)

    /**
     * Remove specified listener from the listener collection managed by current AnimationLifecycle implementation
     *
     * @param listener Listener to remove
     * @return Returns true if listener was successfully removed, otherwise false
     *
     * This is an optimization method, doesn't guarantee every added listener will eventually be removed.
     * Can safely call this method multiple times, removed listeners can still be used.
     */
    fun removeListener(listener: AnimationLifecycleListener)
}
