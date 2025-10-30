package com.kernelflux.aniflux.engine

import android.content.Context
import com.kernelflux.aniflux.cache.AnimationCache
import com.kernelflux.aniflux.cache.MemoryAnimationCache
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.AnimationKey
import com.kernelflux.aniflux.util.AnimationOptions
import java.util.concurrent.ConcurrentHashMap


/**
 * 动画引擎
 * 负责管理动画请求的生命周期、缓存策略、线程池调度等
 */
class AnimationEngine(
    private val memoryCache: AnimationCache = MemoryAnimationCache()
) {
    private val activeJobs = ConcurrentHashMap<AnimationKey, AnimationJob<*>>()
    private val activeResources = ConcurrentHashMap<AnimationKey, AnimationResource<*>>()

    /**
     * 启动动画加载请求
     * 这是从SingleAnimationRequest调用的核心方法
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
                // 找到内存资源，直接返回
                cb?.onResourceReady(memoryResource, AnimationDataSource.MEMORY_CACHE, false)
                return null
            }
        }

        // 2. 内存中没有，检查是否有正在执行的任务
        val existingJob = activeJobs[key]
        if (existingJob != null) {
            // 有正在执行的任务，等待它完成
            return LoadStatus(cb, existingJob)
        }

        // 3. 启动新的加载任务
        return startNewJob(context, model, target, options, listener, cb, key)
    }

    /**
     * 启动新的加载任务
     */
    private fun <T> startNewJob(
        context: Context,
        model: Any?,
        target: AnimationTarget<T>,
        options: AnimationOptions,
        listener: AnimationRequestListener<T>?,
        cb: AnimationResourceCallback?,
        key: AnimationKey
    ): LoadStatus {
        // 创建AnimationJob，参考Glide的EngineJob设计
        val job = AnimationJob<T>(
            engine = this,
            context = context,
            model = model,
            target = target,
            options = options,
            key = key,
            listener = listener,
            callback = cb
        )

        // 将任务添加到活跃任务列表
        activeJobs[key] = job

        // 启动任务，参考Glide: engineJob.start(decodeJob)
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
     */
    private fun loadFromActiveResources(key: AnimationKey): AnimationResource<*>? {
        return activeResources[key]
    }

    /**
     * 从内存缓存中加载
     */
    private fun loadFromMemoryCache(key: AnimationKey): AnimationResource<*>? {
        return memoryCache.get(key.toString())
    }

    /**
     * 构建动画缓存键
     */
    private fun buildAnimationKey(model: Any?, options: AnimationOptions): AnimationKey {
        return AnimationKey(
            model = model,
            width = options.width,
            height = options.height,
            scaleType = options.scaleType,
            cacheStrategy = options.cacheStrategy
        )
    }

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

        // 如果资源可缓存，加入内存缓存
        if (resource.isCacheable()) {
            memoryCache.put(key.toString(), resource)
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