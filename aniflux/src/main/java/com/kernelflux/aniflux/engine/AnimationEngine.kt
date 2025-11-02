package com.kernelflux.aniflux.engine

import android.content.Context
import com.kernelflux.aniflux.cache.AnimationMemoryCache
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.aniflux.cache.AnimationDiskCache
import com.kernelflux.aniflux.cache.MemoryAnimationMemoryCache
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.AnimationKey
import com.kernelflux.aniflux.util.AnimationOptions
import java.io.File
import java.util.concurrent.ConcurrentHashMap


/**
 * 动画引擎
 * 负责管理动画请求的生命周期、缓存策略、线程池调度等
 */
class AnimationEngine(
    private val memoryCache: AnimationMemoryCache = MemoryAnimationMemoryCache(),
    private val animationDiskCache: AnimationDiskCache? = null
) {
    private val activeJobs = ConcurrentHashMap<AnimationKey, AnimationJob<*>>()
    private val activeResources = ConcurrentHashMap<AnimationKey, AnimationResource<*>>()

    /**
     * 启动动画加载请求
     * 这是从SingleAnimationRequest调用的核心方法
     *
     * 完整的缓存流程：
     * 1. 内存缓存查询（activeResource + memoryCache）
     * 2. 磁盘缓存查询（如果启用）
     * 3. 启动新的加载任务（网络下载或本地加载）
     */
    fun <T> load(
        context: Context,
        model: Any?,
        target: AnimationTarget<T>,
        options: AnimationOptions,
        listener: AnimationRequestListener<T>?,
        cb: AnimationResourceCallback? = null
    ): LoadStatus? {
        val key = buildAnimationKey(model, options)

        // 1. 首先尝试从内存中获取资源
        var memoryResource: AnimationResource<*>?
        synchronized(this) {
            memoryResource = loadFromMemory(key)
            if (memoryResource != null) {
                // ✅ 找到内存资源，直接返回（注意：loadFromMemory 已经调用了 acquire）
                cb?.onResourceReady(memoryResource, AnimationDataSource.MEMORY_CACHE, false)
                return null
            }
        }

        // 2. 内存中没有，检查磁盘缓存（如果启用）
        if (animationDiskCache != null &&
            options.useDiskCache &&
            (options.cacheStrategy == AnimationCacheStrategy.DISK_ONLY || options.cacheStrategy == AnimationCacheStrategy.BOTH)
        ) {
            val diskFile = animationDiskCache.get(key.toCacheKey())
            if (diskFile != null && diskFile.exists()) {
                // 磁盘缓存命中，启动任务从磁盘加载（不需要网络下载）
                // 注意：这里需要告知 AnimationJob 使用磁盘文件而不是网络下载
                return startNewJob(context, model, target, options, listener, cb, key, diskFile)
            }
        }

        // 3. 检查是否有正在执行的任务
        val existingJob = activeJobs[key]
        if (existingJob != null) {
            // 有正在执行的任务，等待它完成
            return LoadStatus(cb, existingJob)
        }

        // 4. 启动新的加载任务（网络下载或本地加载）
        return startNewJob(context, model, target, options, listener, cb, key, null)
    }

    /**
     * 启动新的加载任务
     * @param diskCachedFile 磁盘缓存文件（如果从磁盘缓存加载）
     */
    private fun <T> startNewJob(
        context: Context,
        model: Any?,
        target: AnimationTarget<T>,
        options: AnimationOptions,
        listener: AnimationRequestListener<T>?,
        cb: AnimationResourceCallback?,
        key: AnimationKey,
        diskCachedFile: File? = null
    ): LoadStatus {
        val job = AnimationJob<T>(
            engine = this,
            context = context,
            model = model,
            target = target,
            options = options,
            key = key,
            listener = listener,
            callback = cb,
            animationDiskCache = animationDiskCache,
            diskCachedFile = diskCachedFile
        )
        activeJobs[key] = job
        job.start()

        return LoadStatus(cb, job)
    }

    private fun loadFromMemory(key: AnimationKey): AnimationResource<*>? {
        // 检查活跃资源（正在使用的资源）
        val activeResource = loadFromActiveResources(key)
        if (activeResource != null) {
            return activeResource
        }
        // 检查内存缓存
        val cachedResource = loadFromMemoryCache(key)
        if (cachedResource != null) {
            return cachedResource
        }
        return null
    }


    /**
     * 从活跃资源中加载
     * 从活跃资源获取时，需要 acquire（增加引用计数）
     */
    private fun loadFromActiveResources(key: AnimationKey): AnimationResource<*>? {
        val active = activeResources[key] ?: return null
        // ✅ 从活跃资源获取时 acquire（Request 持有资源）
        active.acquire()
        return active
    }

    /**
     * 从内存缓存中加载
     * 从内存缓存获取资源时，需要 acquire 并转移到 activeResources
     */
    private fun loadFromMemoryCache(key: AnimationKey): AnimationResource<*>? {
        val cached = memoryCache.get(key.toCacheKey()) ?: return null
        // ✅ 从内存缓存获取时 acquire（Engine 持有资源）
        cached.acquire()
        // ✅ 从内存缓存移除，加入活跃资源（资源流转）
        memoryCache.remove(key.toCacheKey())
        activeResources[key] = cached
        return cached
    }

    /**
     * 构建动画缓存键
     */
    private fun buildAnimationKey(model: Any?, options: AnimationOptions): AnimationKey {
        return AnimationKey(
            model = model,
            cacheStrategy = options.cacheStrategy
        )
    }

    /**
     * 获取磁盘缓存实例（供 AnimationJob 使用）
     */
    internal fun getDiskCache(): AnimationDiskCache? = animationDiskCache

    /**
     * 任务完成回调
     * 当AnimationJob完成时调用
     */
    internal fun <T> onJobComplete(
        key: AnimationKey,
        resource: AnimationResource<T>?
    ) {
        // 从活跃任务中移除
        activeJobs.remove(key)

        if (resource != null) {
            // ✅ 任务完成时 acquire（Engine 持有资源）
            resource.acquire()
            // 成功：将资源加入活跃资源
            activeResources[key] = resource
        }

        // 处理等待该资源的其他任务
        handleWaitingJobs(key, resource)
    }

    /**
     * 资源释放回调
     * 当AnimationResource引用计数为0时调用
     */
    internal fun onResourceReleased(key: AnimationKey, resource: AnimationResource<*>) {
        // 从活跃资源中移除
        activeResources.remove(key)

        // ✅ 如果资源可缓存，加入内存缓存（资源流转：activeResources → memoryCache）
        if (resource.isCacheable() &&
            (key.cacheStrategy == AnimationCacheStrategy.MEMORY_ONLY || key.cacheStrategy == AnimationCacheStrategy.BOTH)
        ) {
            memoryCache.put(key.toCacheKey(), resource)
        } else {
            // ✅ 不可缓存则回收资源
            resource.recycle()
        }
    }

    /**
     * 处理等待该资源的其他任务
     * 暂时简化实现，后续可以扩展
     */
    private fun handleWaitingJobs(key: AnimationKey, resource: AnimationResource<*>?) {
        // 简化实现：暂时不需要复杂的等待队列管理
        // 后续可以根据需要实现等待相同资源的任务队列
    }

    /**
     * 清理资源
     */
    fun clear() {
        activeJobs.clear()
        activeResources.clear()
        memoryCache.clear()
    }


    inner class LoadStatus internal constructor(
        private val cb: AnimationResourceCallback?,
        private val engineJob: AnimationJob<*>
    ) {

        fun cancel() {
            synchronized(this@AnimationEngine) {
                engineJob.removeCallback(cb)
            }
        }
    }


}