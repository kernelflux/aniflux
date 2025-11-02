package com.kernelflux.aniflux.load

/**
 * 动画数据来源
 * 
 * @author: kerneflux
 * @date: 2025/10/12
 */
enum class AnimationDataSource {
    /**
     * 本地资源（文件、Asset、Resource）
     */
    LOCAL,
    
    /**
     * 远程资源（网络下载，首次加载）
     */
    REMOTE,
    
    /**
     * 磁盘缓存（网络资源的缓存命中）
     */
    DISK_CACHE,
    
    /**
     * 内存缓存（activeResource 或 memoryCache）
     */
    MEMORY_CACHE
}