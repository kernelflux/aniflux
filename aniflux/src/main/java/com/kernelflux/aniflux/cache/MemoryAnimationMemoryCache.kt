package com.kernelflux.aniflux.cache

import android.util.LruCache
import com.kernelflux.aniflux.engine.AnimationResource

/**
 * 内存动画缓存 - 基于LruCache实现
 */
class MemoryAnimationMemoryCache(
    maxSize: Int = calculateDefaultMaxSize()
) : AnimationMemoryCache {
    
    private val cache = object : LruCache<String, AnimationResource<*>>(maxSize) {
        override fun sizeOf(key: String, value: AnimationResource<*>): Int {
            // 简单的内存计算，实际应该根据资源类型计算
            return 1
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: AnimationResource<*>,
            newValue: AnimationResource<*>?
        ) {
            // 资源被移除时，回收资源
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
            val cacheSize = (maxMemory / 8).toInt() // 使用1/8的内存作为缓存
            return maxOf(cacheSize, 10 * 1024 * 1024) // 最少10MB
        }
    }
}
