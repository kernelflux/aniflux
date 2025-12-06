package com.kernelflux.aniflux.cache

import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * LRU strategy-based disk cache implementation
 * 
 * Uses index file to record cache metadata (filename, size, access time),
 * implements LRU strategy to manage cache files
 */
class LruAnimationDiskCache(
    private val cacheDir: File,
    private val maxSize: Long = 100 * 1024 * 1024 // 100MB default
) : AnimationDiskCache {

    companion object {
        private const val TAG = "LruDiskCache"
        private const val INDEX_FILE_NAME = "index.json"
        private const val CLEANUP_THRESHOLD = 0.9 // Trigger cleanup when cache reaches 90%
    }

    private val indexFile = File(cacheDir, INDEX_FILE_NAME)
    
    // Index data: key -> (filename, size, lastAccessTime)
    private val index: MutableMap<String, CacheEntry> = mutableMapOf()
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        loadIndex()
    }

    override fun get(key: String): File? {
        val entry = index[key] ?: return null
        
        val file = File(cacheDir, entry.filename)
        if (!file.exists() || !file.isFile) {
            // File doesn't exist, remove from index
            index.remove(key)
            saveIndex()
            return null
        }
        
        // Update access time
        entry.lastAccessTime = System.currentTimeMillis()
        saveIndex()
        
        return file
    }

    override fun put(key: String, file: File) {
        if (!file.exists() || !file.isFile) {
            AniFluxLog.w(AniFluxLogCategory.CACHE, "Cannot cache non-existent file: ${file.absolutePath}")
            return
        }
        
        val fileSize = file.length()
        
        // Check cache size, cleanup if exceeds limit
        if (getSize() + fileSize > maxSize * CLEANUP_THRESHOLD) {
            evictUntilEnoughSpace(fileSize)
        }
        
        // Determine cache filename
        val filename = generateFilename(key, file)
        val cachedFile = File(cacheDir, filename)
        
        try {
            // Copy file to cache directory
            file.copyTo(cachedFile, overwrite = true)
            
            // Update index
            index[key] = CacheEntry(
                filename = filename,
                size = fileSize,
                lastAccessTime = System.currentTimeMillis()
            )
            saveIndex()
            
            AniFluxLog.d(AniFluxLogCategory.CACHE, "Cached file: key=$key, size=$fileSize")
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.CACHE, "Failed to cache file: key=$key", e)
        }
    }

    override fun remove(key: String) {
        val entry = index.remove(key)
        entry?.let {
            val file = File(cacheDir, it.filename)
            file.delete()
            saveIndex()
        }
    }

    override fun clear() {
        // Delete all cache files
        index.values.forEach { entry ->
            File(cacheDir, entry.filename).delete()
        }
        index.clear()
        saveIndex()
    }

    override fun getSize(): Long {
        return index.values.sumOf { it.size }
    }

    override fun getMaxSize(): Long {
        return maxSize
    }

    /**
     * Generate cache filename
     * Format: {key}.{original file extension}
     */
    private fun generateFilename(key: String, file: File): String {
        val ext = file.extension
        return if (ext.isNotEmpty()) {
            "$key.$ext"
        } else {
            key
        }
    }

    /**
     * Evict cache until enough space
     */
    private fun evictUntilEnoughSpace(requiredSpace: Long) {
        val currentSize = getSize()
        val targetSize = maxSize * CLEANUP_THRESHOLD - requiredSpace
        
        if (currentSize <= targetSize) {
            return
        }
        
        // Sort by access time, delete least recently accessed files
        val sortedEntries = index.entries.sortedBy { it.value.lastAccessTime }
        
        var freedSpace = currentSize
        for (entry in sortedEntries) {
            if (freedSpace <= targetSize) {
                break
            }
            
            val file = File(cacheDir, entry.value.filename)
            if (file.delete()) {
                freedSpace -= entry.value.size
                index.remove(entry.key)
            }
        }
        
        saveIndex()
        AniFluxLog.d(AniFluxLogCategory.CACHE, "Evicted cache: freed ${currentSize - freedSpace} bytes")
    }

    /**
     * Load index file
     */
    private fun loadIndex() {
        if (!indexFile.exists()) {
            return
        }
        
        try {
            val json = JSONObject(FileReader(indexFile).readText())
            json.keys().forEach { key ->
                val entryObj = json.getJSONObject(key)
                index[key] = CacheEntry(
                    filename = entryObj.getString("filename"),
                    size = entryObj.getLong("size"),
                    lastAccessTime = entryObj.getLong("lastAccessTime")
                )
            }
            AniFluxLog.d(AniFluxLogCategory.CACHE, "Loaded index: ${index.size} entries")
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.CACHE, "Failed to load index", e)
            index.clear()
        }
    }

    /**
     * Save index file
     */
    private fun saveIndex() {
        try {
            val json = JSONObject()
            index.forEach { (key, entry) ->
                val entryObj = JSONObject().apply {
                    put("filename", entry.filename)
                    put("size", entry.size)
                    put("lastAccessTime", entry.lastAccessTime)
                }
                json.put(key, entryObj)
            }
            
            FileWriter(indexFile).use { writer ->
                writer.write(json.toString())
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.CACHE, "Failed to save index", e)
        }
    }

    /**
     * Cache entry data class
     */
    private data class CacheEntry(
        val filename: String,
        val size: Long,
        var lastAccessTime: Long
    )
}

