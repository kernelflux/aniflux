package com.kernelflux.aniflux.cache

import android.util.LruCache
import com.kernelflux.aniflux.engine.AnimationResource

/**
 * Memory animation cache - implemented based on LruCache
 */
class MemoryAnimationMemoryCache(
    maxSize: Int = calculateDefaultMaxSize()
) : AnimationMemoryCache {
    
    private val cache = object : LruCache<String, AnimationResource<*>>(maxSize) {
        override fun sizeOf(key: String, value: AnimationResource<*>): Int {
            // Simple memory calculation, should actually calculate based on resource type
            return 1
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: AnimationResource<*>,
            newValue: AnimationResource<*>?
        ) {
            // Recycle resource when removed
            oldValue.recycle()
        }
    }
    
    override fun get(key: String): AnimationResource<*>? {
        return cache.get(key)
    }
    
    override fun put(key: String, resource: AnimationResource<*>) {
        cache.put(key, resource)
    }
    
    override fun remove(key: String) {
        cache.remove(key)
    }
    
    override fun clear() {
        cache.evictAll()
    }
    
    override fun size(): Int {
        return cache.size()
    }
    
    override fun maxSize(): Int {
        return cache.maxSize()
    }
    
    companion object {
        private fun calculateDefaultMaxSize(): Int {
            val maxMemory = Runtime.getRuntime().maxMemory()
            val cacheSize = (maxMemory / 8).toInt() // Use 1/8 of memory as cache
            return maxOf(cacheSize, 10 * 1024 * 1024) // Minimum 10MB
        }
    }
}
