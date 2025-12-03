package com.kernelflux.aniflux.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * 动画请求的线程执行器
 */
object AnimationExecutors {
    
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    
    /**
     * 主线程执行器 - 确保回调在主线程执行
     */
    val MAIN_THREAD_EXECUTOR = Executor { command ->
        mainThreadHandler.post(command)
    }
    
    /**
     * 直接执行器 - 在当前线程执行
     */
    val DIRECT_EXECUTOR = Executor { command ->
        command.run()
    }
    
    /**
     * 获取主线程执行器
     */
    fun mainThreadExecutor(): Executor = MAIN_THREAD_EXECUTOR
    
    /**
     * 获取直接执行器
     */
    fun directExecutor(): Executor = DIRECT_EXECUTOR
}
