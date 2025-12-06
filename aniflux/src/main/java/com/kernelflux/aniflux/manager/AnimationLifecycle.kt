package com.kernelflux.aniflux.manager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 * Custom animation lifecycle interface
 */
interface AnimationLifecycle {

    /**
     * Adds a lifecycle listener to the set of listeners managed by this AnimationLifecycle implementation.
     */
    fun addListener( listener: AnimationLifecycleListener)

    /**
     * Removes the specified listener from the set of listeners managed by this AnimationLifecycle implementation.
     *
     * @param listener The listener to remove.
     * @return True if the listener was successfully removed, false otherwise.
     *
     * This is an optimization method and does not guarantee that every added listener will eventually be removed.
     * This method can be safely called multiple times, and removed listeners can still be used.
     */
    fun removeListener(listener: AnimationLifecycleListener)
}
