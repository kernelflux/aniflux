package com.kernelflux.aniflux.util

import android.widget.ImageView

/**
 * 动画缓存键 - 参考Glide的EngineKey设计
 * 用于唯一标识一个动画请求
 */
data class AnimationKey(
    val model: Any?,
    val width: Int,
    val height: Int,
    val scaleType: ImageView.ScaleType?,
    val cacheStrategy: CacheStrategy
) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimationKey) return false
        
        return model == other.model &&
                width == other.width &&
                height == other.height &&
                scaleType == other.scaleType &&
                cacheStrategy == other.cacheStrategy
    }
    
    override fun hashCode(): Int {
        var result = model?.hashCode() ?: 0
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + (scaleType?.hashCode() ?: 0)
        result = 31 * result + cacheStrategy.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "AnimationKey(model=$model, width=$width, height=$height, scaleType=$scaleType, cacheStrategy=$cacheStrategy)"
    }
}

/**
 * 缓存策略
 */
enum class CacheStrategy {
    ALL,           // 缓存所有
    NONE,          // 不缓存
    SOURCE,        // 只缓存源数据
    RESULT         // 只缓存结果
}
