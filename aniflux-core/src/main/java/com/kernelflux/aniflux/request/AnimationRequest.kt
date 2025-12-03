package com.kernelflux.aniflux.request

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
interface AnimationRequest {
    fun begin()
    fun clear()
    fun pause()
    fun isRunning(): Boolean
    fun isComplete(): Boolean
    fun isCleared(): Boolean
    fun isAnyResourceSet(): Boolean
    fun isEquivalentTo(other: AnimationRequest?): Boolean
}