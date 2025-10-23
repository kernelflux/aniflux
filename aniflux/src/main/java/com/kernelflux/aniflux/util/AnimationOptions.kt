package com.kernelflux.aniflux.util

import android.widget.ImageView

/**
 * 动画选项 - 参考Glide的RequestOptions设计
 * 配置动画加载的各种参数
 */
class AnimationOptions {
    
    var width: Int = 0
    var height: Int = 0
    var scaleType: ImageView.ScaleType? = null
    var cacheStrategy: CacheStrategy = CacheStrategy.ALL
    var useDiskCache: Boolean = true
    var isAnimation: Boolean = false
    var priority: Priority = Priority.NORMAL
    var timeout: Long = 30000L // 30秒超时
    
    companion object {
        fun create(): AnimationOptions = AnimationOptions()
    }
    
    fun size(width: Int, height: Int): AnimationOptions {
        this.width = width
        this.height = height
        return this
    }
    
    fun scaleType(scaleType: ImageView.ScaleType): AnimationOptions {
        this.scaleType = scaleType
        return this
    }
    
    fun cacheStrategy(strategy: CacheStrategy): AnimationOptions {
        this.cacheStrategy = strategy
        return this
    }
    
    fun useDiskCache(use: Boolean): AnimationOptions {
        this.useDiskCache = use
        return this
    }
    
    fun isAnimation(isAnimation: Boolean): AnimationOptions {
        this.isAnimation = isAnimation
        return this
    }
    
    fun priority(priority: Priority): AnimationOptions {
        this.priority = priority
        return this
    }
    
    fun timeout(timeout: Long): AnimationOptions {
        this.timeout = timeout
        return this
    }
}

/**
 * 请求优先级
 */
enum class Priority {
    LOW,
    NORMAL,
    HIGH,
    IMMEDIATE
}
