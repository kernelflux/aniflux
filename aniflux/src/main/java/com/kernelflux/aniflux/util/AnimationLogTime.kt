package com.kernelflux.aniflux.util

import android.os.SystemClock

/**
 * 动画请求日志时间工具
 * 用于记录请求的执行时间
 */
object AnimationLogTime {
    
    private var startTime = 0L
    
    /**
     * 开始计时
     */
    fun getLogTime(): Long {
        startTime = SystemClock.elapsedRealtime()
        return startTime
    }
    
    /**
     * 获取经过的时间（毫秒）
     */
    fun getElapsedMillis(startTime: Long): Long {
        return SystemClock.elapsedRealtime() - startTime
    }
    
    /**
     * 获取当前时间戳
     */
    fun getCurrentTime(): Long {
        return SystemClock.elapsedRealtime()
    }
}
