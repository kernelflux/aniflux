package com.kernelflux.aniflux.cache

import java.io.File

/**
 * Disk cache interface
 * Used to manage animation file disk cache
 */
interface AnimationDiskCache {
    /**
     * Get cache file
     * @param key Cache key
     * @return Cache file, returns null if not exists
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
     * Get max cache size (bytes)
     */
    fun getMaxSize(): Long
}

