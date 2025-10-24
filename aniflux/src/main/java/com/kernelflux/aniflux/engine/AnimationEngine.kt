package com.kernelflux.aniflux.engine

import android.content.Context
import com.bumptech.glide.request.ResourceCallback
import com.kernelflux.aniflux.cache.AnimationCache
import com.kernelflux.aniflux.cache.MemoryAnimationCache
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.AnimationKey
import com.kernelflux.aniflux.util.AnimationOptions
import java.util.concurrent.ConcurrentHashMap


/**
 * 动画引擎 - 参考Glide的Engine设计
 * 负责管理动画请求的生命周期、缓存策略、线程池调度等
 */
class AnimationEngine(
    private val memoryCache: AnimationCache = MemoryAnimationCache()
) {
    // 活跃任务管理 - 正在执行的请求
    private val activeJobs = ConcurrentHashMap<AnimationKey, AnimationJob<*>>()

    // 活跃资源管理 - 正在使用的资源
    private val activeResources = ConcurrentHashMap<AnimationKey, AnimationResource<*>>()

    /**
     * 启动动画加载请求
     */
    fun <T> load(
        context: Context,
        model: Any?,
        target: AnimationTarget<T>,
        options: AnimationOptions,
        listener: AnimationRequestListener<T>?,
        callback: AnimationResourceCallback? = null
    ): LoadStatus {
        // 1. 构建缓存键
        val key = buildAnimationKey(model, options)

        // 2. 检查活跃资源（正在使用的资源）
        val activeResource = loadFromActiveResources(key)
        if (activeResource != null) {
            if (callback != null) {
                // 如果有callback，通过callback通知
                callback.onResourceReady(activeResource, AnimationDataSource.MEMORY_CACHE, false)
            } else {
                // 否则直接通知target
                @Suppress("UNCHECKED_CAST")
                val activeRes = activeResource as T
                target.onResourceReady(activeRes)
                listener?.onResourceReady(
                    activeRes,
                    key.model,
                    target,
                    AnimationDataSource.MEMORY_CACHE,
                    false
                )
            }
            // 返回一个已完成的LoadStatus，表示从活跃资源加载
            return LoadStatus(target, null, LoadStatus.Status.COMPLETED_FROM_ACTIVE)
        }

        // 3. 检查内存缓存
        val cachedResource = loadFromMemoryCache(key)
        if (cachedResource != null) {
            // 从缓存移到活跃资源
            activeResources[key] = cachedResource
            if (callback != null) {
                // 如果有callback，通过callback通知
                callback.onResourceReady(cachedResource, AnimationDataSource.MEMORY_CACHE, false)
            } else {
                // 否则直接通知target
                @Suppress("UNCHECKED_CAST")
                val cachedRes = activeResource as T
                target.onResourceReady(cachedRes)
                listener?.onResourceReady(
                    cachedRes,
                    key.model,
                    target,
                    AnimationDataSource.MEMORY_CACHE,
                    false
                )
            }
            // 返回一个已完成的LoadStatus，表示从内存缓存加载
            return LoadStatus(target, null, LoadStatus.Status.COMPLETED_FROM_CACHE)
        }

        // 4. 检查是否有正在执行的任务
        val existingJob = activeJobs[key]
        if (existingJob != null) {
            existingJob?.addCallback(target, listener)
            return LoadStatus(target, existingJob, LoadStatus.Status.RUNNING)
        }

        // 5. 启动新任务
        return startNewJob(context, model, target, options, listener, key)
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
     * 启动新的加载任务
     */
    private fun <T> startNewJob(
        context: Context,
        model: Any?,
        target: AnimationTarget<T>,
        options: AnimationOptions,
        listener: AnimationRequestListener<T>?,
        key: AnimationKey
    ): LoadStatus {
        val job = AnimationJob(
            engine = this,
            context = context,
            model = model,
            options = options,
            listener = listener,
            key = key,
            callback = callback
        )

        activeJobs[key] = job
        job.start()

        return LoadStatus(target, job, LoadStatus.Status.RUNNING)
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
     */
    internal fun <T> onJobComplete(
        job: AnimationJob<T>,
        key: AnimationKey,
        resource: AnimationResource<T>?
    ) {
        activeJobs.remove(key)

        if (resource != null) {
            // 成功：将资源加入活跃资源
            activeResources[key] = resource
        }
    }

    /**
     * 资源释放回调
     */
    internal fun onResourceReleased(key: AnimationKey, resource: AnimationResource<*>) {
        activeResources.remove(key)

        // 如果资源可缓存，加入内存缓存
        if (resource.isCacheable()) {
            memoryCache.put(key.toString(), resource)
        }
    }

    /**
     * 清理资源
     */
    fun clear() {
        activeJobs.clear()
        activeResources.clear()
        memoryCache.clear()
    }


    inner class LoadStatus internal constructor(private val cb: ResourceCallback?,
                                                private val engineJob: AnimationJob<*>) {

        fun cancel() {
            synchronized(this@AnimationEngine) {
                engineJob.removeCallback(cb)
            }
        }
    }

}