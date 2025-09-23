package com.kernelflux.aniflux.config

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 请求优先级枚举
 */
enum class Priority {
    /**
     * 低优先级
     */
    LOW,

    /**
     * 普通优先级
     */
    NORMAL,

    /**
     * 高优先级
     */
    HIGH,

    /**
     * 立即执行
     */
    IMMEDIATE
}