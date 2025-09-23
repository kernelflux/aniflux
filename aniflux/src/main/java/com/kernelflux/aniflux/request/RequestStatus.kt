package com.kernelflux.aniflux.request

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 请求状态枚举 - 用于跟踪MediaRequest的生命周期
 */
enum class RequestStatus {
    PENDING,
    RUNNING,
    PAUSED,
    CANCELLED,
    COMPLETE,
    CLEARED
}
