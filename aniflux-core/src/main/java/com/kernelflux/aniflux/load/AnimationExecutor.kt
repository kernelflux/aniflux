package com.kernelflux.aniflux.load

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 动画加载线程池管理器，提供不同类型的线程池
 */
object AnimationExecutor {
    
    private const val TAG = "AnimationExecutor"
    
    // 默认线程池参数
    private const val KEEP_ALIVE_TIME_MS = 10L
    private const val MAXIMUM_AUTOMATIC_THREAD_COUNT = 4
    
    // 线程池实例
    @Volatile
    private var sourceExecutor: ExecutorService? = null
    
    @Volatile
    private var diskCacheExecutor: ExecutorService? = null
    
    @Volatile
    private var animationExecutor: ExecutorService? = null

    /**
     * 获取源数据加载线程池（用于网络下载等IO操作）
     */
    fun getSourceExecutor(): ExecutorService {
        return sourceExecutor ?: synchronized(this) {
            sourceExecutor ?: createSourceExecutor().also { sourceExecutor = it }
        }
    }

    /**
     * 获取磁盘缓存线程池（用于文件读写操作）
     */
    fun getDiskCacheExecutor(): ExecutorService {
        return diskCacheExecutor ?: synchronized(this) {
            diskCacheExecutor ?: createDiskCacheExecutor().also { diskCacheExecutor = it }
        }
    }

    /**
     * 获取动画处理线程池（用于动画解析和渲染）
     */
    fun getAnimationExecutor(): ExecutorService {
        return animationExecutor ?: synchronized(this) {
            animationExecutor ?: createAnimationExecutor().also { animationExecutor = it }
        }
    }

    /**
     * 创建源数据加载线程池
     */
    private fun createSourceExecutor(): ExecutorService {
        val threadCount = calculateBestThreadCount()
        return ThreadPoolExecutor(
            threadCount,
            threadCount,
            KEEP_ALIVE_TIME_MS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            createThreadFactory("aniflux-source-")
        )
    }

    /**
     * 创建磁盘缓存线程池
     */
    private fun createDiskCacheExecutor(): ExecutorService {
        return ThreadPoolExecutor(
            1, // 单线程，避免并发读写文件
            1,
            KEEP_ALIVE_TIME_MS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            createThreadFactory("aniflux-disk-cache-")
        )
    }

    /**
     * 创建动画处理线程池
     */
    private fun createAnimationExecutor(): ExecutorService {
        val threadCount = calculateBestThreadCount()
        return ThreadPoolExecutor(
            threadCount,
            threadCount,
            KEEP_ALIVE_TIME_MS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            createThreadFactory("aniflux-animation-")
        )
    }

    /**
     * 计算最佳线程数
     */
    private fun calculateBestThreadCount(): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        return minOf(availableProcessors, MAXIMUM_AUTOMATIC_THREAD_COUNT)
    }

    /**
     * 创建线程工厂
     */
    private fun createThreadFactory(namePrefix: String): ThreadFactory {
        return object : ThreadFactory {
            private val threadNumber = AtomicInteger(1)
            
            override fun newThread(r: Runnable): Thread {
                val thread = Thread(r, "$namePrefix${threadNumber.getAndIncrement()}")
                thread.isDaemon = true
                thread.priority = Thread.NORM_PRIORITY
                return thread
            }
        }
    }

    /**
     * 关闭所有线程池
     */
    fun shutdown() {
        sourceExecutor?.shutdown()
        diskCacheExecutor?.shutdown()
        animationExecutor?.shutdown()
        
        sourceExecutor = null
        diskCacheExecutor = null
        animationExecutor = null
    }
}
