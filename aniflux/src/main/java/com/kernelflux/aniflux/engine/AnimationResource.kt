package com.kernelflux.aniflux.engine

import java.util.concurrent.atomic.AtomicInteger

/**
 * 动画资源包装器
 * 管理资源的生命周期和引用计数
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
     * 获取资源
     */
    fun get(): T {
        if (isRecycled) {
            throw IllegalStateException("Resource has been recycled")
        }
        return resource
    }
    
    /**
     * 获取资源（不检查回收状态）
     */
    fun getUnchecked(): T = resource
    
    /**
     * 获取引用计数
     */
    fun getAcquiredCount(): Int = acquired.get()
    
    /**
     * 是否可缓存
     */
    fun isCacheable(): Boolean = isCacheable
    
    /**
     * 是否已回收
     */
    fun isRecycled(): Boolean = isRecycled
    
    /**
     * 增加引用计数
     */
    fun acquire() {
        if (isRecycled) {
            throw IllegalStateException("Cannot acquire a recycled resource")
        }
        acquired.incrementAndGet()
    }
    
    /**
     * 减少引用计数
     */
    fun release() {
        val count = acquired.decrementAndGet()
        if (count < 0) {
            throw IllegalStateException("Cannot release a resource with negative count")
        }
        
        if (count == 0) {
            // 引用计数为0，通知监听器
            resourceListener.onResourceReleased(key, this)
        }
    }
    
    /**
     * 回收资源
     */
    fun recycle() {
        if (isRecycled) {
            throw IllegalStateException("Cannot recycle a resource that has already been recycled")
        }
        isRecycled = true
    }
    
    /**
     * 资源监听器
     */
    interface ResourceListener {
        fun onResourceReleased(key: String, resource: AnimationResource<*>)
    }
}
