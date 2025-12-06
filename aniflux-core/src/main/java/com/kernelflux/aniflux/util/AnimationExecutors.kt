package com.kernelflux.aniflux.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Thread executors for animation requests
 */
object AnimationExecutors {
    
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    
    /**
     * Main thread executor - ensures callbacks execute on main thread
     */
    val MAIN_THREAD_EXECUTOR = Executor { command ->
        mainThreadHandler.post(command)
    }
    
    /**
     * Direct executor - executes in current thread
     */
    val DIRECT_EXECUTOR = Executor { command ->
        command.run()
    }
    
    /**
     * Get main thread executor
     */
    fun mainThreadExecutor(): Executor = MAIN_THREAD_EXECUTOR
    
    /**
     * Get direct executor
     */
    fun directExecutor(): Executor = DIRECT_EXECUTOR
}
