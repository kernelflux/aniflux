package com.kernelflux.aniflux.cache

import com.kernelflux.aniflux.engine.AnimationResource

/**
 * 动画缓存接口
 */
interface AnimationCache {
    fun get(key: String): AnimationResource<*>?
    fun put(key: String, resource: AnimationResource<*>)
    fun remove(key: String)
    fun clear()
    fun size(): Int
    fun maxSize(): Int
}
