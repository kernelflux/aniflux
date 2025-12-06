package com.kernelflux.aniflux.engine

import java.util.concurrent.atomic.AtomicInteger

/**
 * Animation resource wrapper
 * Manages resource lifecycle and reference counting
 */
class AnimationResource<T>(
    private val resource: T,
    private val isCacheable: Boolean,
    private val key: String,
    private val resourceListener: ResourceListener
) {
    
    private val acquired = AtomicInteger(0)
    private var isRecycled = false
    
    /**
     * Get resource
     */
    fun get(): T {
        if (isRecycled) {
            throw IllegalStateException("Resource has been recycled")
        }
        return resource
    }
    
    /**
     * Get resource (without checking recycle status)
     */
    fun getUnchecked(): T = resource
    
    /**
     * Get reference count
     */
    fun getAcquiredCount(): Int = acquired.get()
    
    /**
     * Whether cacheable
     */
    fun isCacheable(): Boolean = isCacheable
    
    /**
     * Whether recycled
     */
    fun isRecycled(): Boolean = isRecycled
    
    /**
     * Increment reference count
     */
    fun acquire() {
        if (isRecycled) {
            throw IllegalStateException("Cannot acquire a recycled resource")
        }
        acquired.incrementAndGet()
    }
    
    /**
     * Decrement reference count
     */
    fun release() {
        val count = acquired.decrementAndGet()
        if (count < 0) {
            throw IllegalStateException("Cannot release a resource with negative count")
        }
        
        if (count == 0) {
            // Reference count is 0, notify listener
            resourceListener.onResourceReleased(key, this)
        }
    }
    
    /**
     * Recycle resource
     */
    fun recycle() {
        if (isRecycled) {
            throw IllegalStateException("Cannot recycle a resource that has already been recycled")
        }
        isRecycled = true
    }
    
    /**
     * Resource listener
     */
    interface ResourceListener {
        fun onResourceReleased(key: String, resource: AnimationResource<*>)
    }
}
