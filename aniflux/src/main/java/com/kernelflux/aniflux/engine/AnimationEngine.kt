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
 * Animation engine
 * Responsible for managing animation request lifecycle, cache strategy, thread pool scheduling, etc.
 */
class AnimationEngine(
    private val memoryCache: AnimationMemoryCache = MemoryAnimationMemoryCache(),
    private val animationDiskCache: AnimationDiskCache? = null
) {
    private val activeJobs = ConcurrentHashMap<AnimationKey, AnimationJob<*>>()
    private val activeResources = ConcurrentHashMap<AnimationKey, AnimationResource<*>>()

    /**
     * Start animation load request
     * This is the core method called from SingleAnimationRequest
     *
     * Complete cache flow:
     * 1. Memory cache query (activeResource + memoryCache)
     * 2. Disk cache query (if enabled)
     * 3. Start new load task (network download or local load)
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

        // 1. First try to get resource from memory
        var memoryResource: AnimationResource<*>?
        synchronized(this) {
            memoryResource = loadFromMemory(key)
            if (memoryResource != null) {
                // ✅ Found memory resource, return directly (note: loadFromMemory has already called acquire)
                cb?.onResourceReady(memoryResource, AnimationDataSource.MEMORY_CACHE, false)
                return null
            }
        }

        // 2. Not in memory, check disk cache (if enabled)
        if (animationDiskCache != null &&
            (options.cacheStrategy == AnimationCacheStrategy.DISK_ONLY || options.cacheStrategy == AnimationCacheStrategy.BOTH)
        ) {
            val diskFile = animationDiskCache.get(key.toCacheKey())
            if (diskFile != null && diskFile.exists()) {
                // Disk cache hit, start task to load from disk (no network download needed)
                // Note: Need to inform AnimationJob to use disk file instead of network download
                return startNewJob(context, model, target, options, listener, cb, key, diskFile)
            }
        }

        // 3. Check if there's an executing task
        val existingJob = activeJobs[key]
        if (existingJob != null) {
            // There's an executing task, add new callback to existing Job
            if (cb != null) {
                existingJob.addCallback(cb)
            }
            return LoadStatus(cb, existingJob)
        }

        // 4. Start new load task (network download or local load)
        return startNewJob(context, model, target, options, listener, cb, key, null)
    }

    /**
     * Start new load task
     * @param diskCachedFile Disk cache file (if loading from disk cache)
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
        // Check active resources (resources in use)
        val activeResource = loadFromActiveResources(key)
        if (activeResource != null) {
            return activeResource
        }
        // Check memory cache
        val cachedResource = loadFromMemoryCache(key)
        if (cachedResource != null) {
            return cachedResource
        }
        return null
    }


    /**
     * Load from active resources
     * When getting from active resources, need to acquire (increment reference count)
     */
    private fun loadFromActiveResources(key: AnimationKey): AnimationResource<*>? {
        val active = activeResources[key] ?: return null
        // ✅ Acquire when getting from active resources (Request holds resource)
        active.acquire()
        return active
    }

    /**
     * Load from memory cache
     * When getting resource from memory cache, need to acquire and transfer to activeResources
     */
    private fun loadFromMemoryCache(key: AnimationKey): AnimationResource<*>? {
        val cached = memoryCache.get(key.toCacheKey()) ?: return null
        // ✅ Acquire when getting from memory cache (Engine holds resource)
        cached.acquire()
        // ✅ Remove from memory cache, add to active resources (resource flow)
        memoryCache.remove(key.toCacheKey())
        activeResources[key] = cached
        return cached
    }

    /**
     * Build animation cache key
     */
    private fun buildAnimationKey(model: Any?, options: AnimationOptions): AnimationKey {
        return AnimationKey(
            model = model,
            cacheStrategy = options.cacheStrategy
        )
    }

    /**
     * Get disk cache instance (for AnimationJob use)
     */
    internal fun getDiskCache(): AnimationDiskCache? = animationDiskCache

    /**
     * Task completion callback
     * Called when AnimationJob completes
     */
    internal fun <T> onJobComplete(
        key: AnimationKey,
        resource: AnimationResource<T>?
    ) {
        // Remove from active jobs
        activeJobs.remove(key)

        if (resource != null) {
            // ✅ Acquire when task completes (Engine holds resource)
            resource.acquire()
            // Success: add resource to active resources
            activeResources[key] = resource
        }

        // Handle other tasks waiting for this resource
        handleWaitingJobs(key, resource)
    }

    /**
     * Resource release callback
     * Called when AnimationResource reference count reaches 0
     */
    internal fun onResourceReleased(key: AnimationKey, resource: AnimationResource<*>) {
        // Remove from active resources
        activeResources.remove(key)

        // ✅ If resource is cacheable, add to memory cache (resource flow: activeResources → memoryCache)
        if (resource.isCacheable() &&
            (key.cacheStrategy == AnimationCacheStrategy.MEMORY_ONLY || key.cacheStrategy == AnimationCacheStrategy.BOTH)
        ) {
            memoryCache.put(key.toCacheKey(), resource)
        } else {
            // ✅ If not cacheable, recycle resource
            resource.recycle()
        }
    }

    /**
     * Handle other tasks waiting for this resource
     * Actually don't need to handle here, as addCallback has already handled waiting requests
     * But can keep as extension point
     */
    private fun handleWaitingJobs(key: AnimationKey, resource: AnimationResource<*>?) {
        // addCallback has already handled waiting requests, can leave empty or do additional processing
    }

    /**
     * Clear resources
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