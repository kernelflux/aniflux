package com.kernelflux.aniflux.cache

/**
 * @author: kerneflux
 * @date: 2025/11/3
 *
 */
enum class AnimationCacheStrategy {
    /**
     * No cache (neither memory nor disk cache)
     * - Network resources: download every time, don't save
     * - Local resources: load every time, don't cache
     */
    NONE,

    /**
     * Memory cache only
     * - Network resources: download and parse, only cache memory objects
     * - Local resources: load and parse, only cache memory objects
     * - Suitable for: low storage space scenarios, or frequently accessed hot resources
     */
    MEMORY_ONLY,

    /**
     * Disk cache only (no memory cache)
     * - Network resources: save to disk after download, don't cache memory objects after parsing
     * - Local resources: save to disk, don't cache memory objects
     * - Suitable for: large file resources, or memory-constrained scenarios
     */
    DISK_ONLY,

    /**
     * Memory + disk cache (default)
     * - Network resources: save to disk after download, cache memory objects after parsing
     * - Local resources: cache memory objects after loading (local resources usually don't need disk cache)
     * - Suitable for: most scenarios
     */
    BOTH
}