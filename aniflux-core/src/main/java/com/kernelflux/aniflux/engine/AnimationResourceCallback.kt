package com.kernelflux.aniflux.engine

import com.kernelflux.aniflux.load.AnimationDataSource

/**
 * Animation resource callback interface
 * Used by Engine to callback to Request, notify load completion or failure
 */
interface AnimationResourceCallback {

    /**
     * Called when resource is successfully loaded
     * @param resource Loaded resource
     * @param dataSource Data source
     * @param isLoadedFromAlternateCacheKey Whether loaded from alternate cache key
     */
    fun onResourceReady(
        resource: AnimationResource<*>?,
        dataSource: AnimationDataSource,
        isLoadedFromAlternateCacheKey: Boolean
    )

    /**
     * Called when resource load fails
     * @param exception Exception information
     */
    fun onLoadFailed(exception: Throwable)

    /**
     * Returns lock object for synchronization
     */
    fun getLock(): Any
}
