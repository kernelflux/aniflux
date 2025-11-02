package com.kernelflux.aniflux.util

import android.widget.ImageView
import com.kernelflux.aniflux.cache.AnimationCacheStrategy

/**
 * 动画选项
 * 配置动画加载的各种参数
 */
class AnimationOptions {
    
    // 尺寸配置（保留用于特殊情况，如 GIF 的尺寸缩放，但不影响缓存键）
    var width: Int = 0
    var height: Int = 0
    var scaleType: ImageView.ScaleType? = null
    var cacheStrategy: AnimationCacheStrategy = AnimationCacheStrategy.BOTH
    var useDiskCache: Boolean = true
    var isAnimation: Boolean = false
    var priority: Priority = Priority.NORMAL
    var timeout: Long = 30000L // 30秒超时
    
    // 动画播放配置
    var repeatCount: Int = -1 // -1表示无限循环，0表示不循环，>0表示循环次数
    var autoPlay: Boolean = true // 是否自动播放
    var retainLastFrame: Boolean = true // 是否保留最后一帧（动画结束时），默认 true
    
    companion object {
        fun create(): AnimationOptions = AnimationOptions()
        
        /**
         * 创建默认配置
         */
        fun defaultOptions(): AnimationOptions {
            return create()
                .cacheStrategy(AnimationCacheStrategy.BOTH)
                .useDiskCache(true)
                .isAnimation(true)
                .priority(Priority.NORMAL)
                .timeout(30000L)
        }
        
        /**
         * 创建高性能配置（无缓存）
         */
        fun highPerformanceOptions(): AnimationOptions {
            return create()
                .cacheStrategy(AnimationCacheStrategy.NONE)
                .useDiskCache(false)
                .isAnimation(true)
                .priority(Priority.HIGH)
                .timeout(15000L)
        }
        
        /**
         * 创建低内存配置（仅磁盘缓存）
         */
        fun lowMemoryOptions(): AnimationOptions {
            return create()
                .cacheStrategy(AnimationCacheStrategy.DISK_ONLY)
                .useDiskCache(true)
                .isAnimation(true)
                .priority(Priority.LOW)
                .timeout(60000L)
        }
        
        /**
         * 创建仅内存缓存配置
         */
        fun memoryOnlyOptions(): AnimationOptions {
            return create()
                .cacheStrategy(AnimationCacheStrategy.MEMORY_ONLY)
                .useDiskCache(false)
                .isAnimation(true)
                .priority(Priority.NORMAL)
                .timeout(30000L)
        }
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
    
    fun cacheStrategy(strategy: AnimationCacheStrategy): AnimationOptions {
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
    
    /**
     * 设置动画循环次数
     * @param count -1表示无限循环，0表示不循环，>0表示循环次数
     */
    fun repeatCount(count: Int): AnimationOptions {
        this.repeatCount = count
        return this
    }
    
    /**
     * 设置是否自动播放
     */
    fun autoPlay(auto: Boolean): AnimationOptions {
        this.autoPlay = auto
        return this
    }
    
    /**
     * 设置是否保留最后一帧（动画结束时）
     * @param retain true 表示保留最后一帧，false 表示清空（默认 true）
     */
    fun retainLastFrame(retain: Boolean): AnimationOptions {
        this.retainLastFrame = retain
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
