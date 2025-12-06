package com.kernelflux.aniflux.cache

import java.io.File

/**
 * Disk cache interface
 * Used to manage disk cache for animation files
 */
interface AnimationDiskCache {
    /**
     * Get cache file
     * @param key Cache key
     * @return Cache file, returns null if doesn't exist
     */
    fun get(key: String): File?
    
    /**
     * Save to disk cache
     * @param key Cache key
     * @param file File to cache
     */
    fun put(key: String, file: File)
    
    /**
     * Remove cache
     * @param key Cache key
     */
    fun remove(key: String)
    
    /**
     * Clear cache
     */
    fun clear()
    
    /**
     * Get cache size (bytes)
     */
    fun getSize(): Long
    
    /**
     * Get maximum cache size (bytes)
     */
    fun getMaxSize(): Long
}

