package com.kernelflux.aniflux.engine

/**
 * 动画资源回调接口 - 参考Glide的ResourceCallback设计
 * 用于Engine回调给Request，通知加载完成或失败
 */
interface AnimationResourceCallback {
    
    /**
     * 当资源成功加载时调用
     * @param resource 加载的资源
     * @param dataSource 数据源
     * @param isLoadedFromAlternateCacheKey 是否从备用缓存键加载
     */
    fun onResourceReady(
        resource: AnimationResource<*>,
        dataSource: Any?,
        isLoadedFromAlternateCacheKey: Boolean
    )
    
    /**
     * 当资源加载失败时调用
     * @param exception 异常信息
     */
    fun onLoadFailed(exception: Throwable)
    
    /**
     * 返回用于同步的锁对象
     */
    fun getLock(): Any
}
