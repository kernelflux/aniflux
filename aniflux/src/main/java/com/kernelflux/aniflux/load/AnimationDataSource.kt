package com.kernelflux.aniflux.load

/**
 * Animation data source
 * 
 * @author: kerneflux
 * @date: 2025/10/12
 */
enum class AnimationDataSource {
    /**
     * Local resource (file, Asset, Resource)
     */
    LOCAL,
    
    /**
     * Remote resource (network download, first load)
     */
    REMOTE,
    
    /**
     * Disk cache (network resource cache hit)
     */
    DISK_CACHE,
    
    /**
     * Memory cache (activeResource or memoryCache)
     */
    MEMORY_CACHE
}