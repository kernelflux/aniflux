package com.kernelflux.aniflux.config

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 缓存策略枚举
 */
enum class CacheStrategy {
    /**
     * 不缓存
     */
    NONE,

    /**
     * 只缓存原始数据
     */
    SOURCE,

    /**
     * 只缓存处理后的数据
     */
    RESULT,

    /**
     * 缓存所有数据（原始数据 + 处理后的数据）
     */
    ALL
}
