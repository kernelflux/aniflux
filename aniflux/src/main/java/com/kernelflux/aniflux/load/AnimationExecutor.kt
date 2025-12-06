package com.kernelflux.aniflux.load

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Animation loading thread pool manager, provides different types of thread pools
 */
object AnimationExecutor {
    
    private const val TAG = "AnimationExecutor"
    
    // Default thread pool parameters
    private const val KEEP_ALIVE_TIME_MS = 10L
    private const val MAXIMUM_AUTOMATIC_THREAD_COUNT = 4
    
    // Thread pool instances
    @Volatile
    private var sourceExecutor: ExecutorService? = null
    
    @Volatile
    private var diskCacheExecutor: ExecutorService? = null
    
    @Volatile
    private var animationExecutor: ExecutorService? = null

    /**
     * Get source data loading thread pool (for IO operations like network download)
     */
    fun getSourceExecutor(): ExecutorService {
        return sourceExecutor ?: synchronized(this) {
            sourceExecutor ?: createSourceExecutor().also { sourceExecutor = it }
        }
    }

    /**
     * Get disk cache thread pool (for file read/write operations)
     */
    fun getDiskCacheExecutor(): ExecutorService {
        return diskCacheExecutor ?: synchronized(this) {
            diskCacheExecutor ?: createDiskCacheExecutor().also { diskCacheExecutor = it }
        }
    }

    /**
     * Get animation processing thread pool (for animation parsing and rendering)
     */
    fun getAnimationExecutor(): ExecutorService {
        return animationExecutor ?: synchronized(this) {
            animationExecutor ?: createAnimationExecutor().also { animationExecutor = it }
        }
    }

    /**
     * Create source data loading thread pool
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
     * Create disk cache thread pool
     */
    private fun createDiskCacheExecutor(): ExecutorService {
        return ThreadPoolExecutor(
            1, // Single thread, avoid concurrent file read/write
            1,
            KEEP_ALIVE_TIME_MS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            createThreadFactory("aniflux-disk-cache-")
        )
    }

    /**
     * Create animation processing thread pool
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
     * Calculate optimal thread count
     */
    private fun calculateBestThreadCount(): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        return minOf(availableProcessors, MAXIMUM_AUTOMATIC_THREAD_COUNT)
    }

    /**
     * Create thread factory
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
     * Shutdown all thread pools
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
