package com.kernelflux.aniflux.cache

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * 基于 LRU 策略的磁盘缓存实现
 * 
 * 使用索引文件记录缓存的元数据（文件名、大小、访问时间），
 * 实现 LRU 策略管理缓存文件
 */
class LruAnimationDiskCache(
    private val cacheDir: File,
    private val maxSize: Long = 100 * 1024 * 1024 // 100MB 默认
) : AnimationDiskCache {

    companion object {
        private const val TAG = "LruDiskCache"
        private const val INDEX_FILE_NAME = "index.json"
        private const val CLEANUP_THRESHOLD = 0.9 // 当缓存达到 90% 时触发清理
    }

    private val indexFile = File(cacheDir, INDEX_FILE_NAME)
    
    // 索引数据：key -> (filename, size, lastAccessTime)
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
            // 文件不存在，从索引中移除
            index.remove(key)
            saveIndex()
            return null
        }
        
        // 更新访问时间
        entry.lastAccessTime = System.currentTimeMillis()
        saveIndex()
        
        return file
    }

    override fun put(key: String, file: File) {
        if (!file.exists() || !file.isFile) {
            Log.w(TAG, "Cannot cache non-existent file: ${file.absolutePath}")
            return
        }
        
        val fileSize = file.length()
        
        // 检查缓存大小，如果超过限制则清理
        if (getSize() + fileSize > maxSize * CLEANUP_THRESHOLD) {
            evictUntilEnoughSpace(fileSize)
        }
        
        // 确定缓存文件名
        val filename = generateFilename(key, file)
        val cachedFile = File(cacheDir, filename)
        
        try {
            // 复制文件到缓存目录
            file.copyTo(cachedFile, overwrite = true)
            
            // 更新索引
            index[key] = CacheEntry(
                filename = filename,
                size = fileSize,
                lastAccessTime = System.currentTimeMillis()
            )
            saveIndex()
            
            Log.d(TAG, "Cached file: key=$key, size=$fileSize")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache file: key=$key", e)
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
        // 删除所有缓存文件
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
     * 生成缓存文件名
     * 格式：{key}.{原文件扩展名}
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
     * 清理缓存直到有足够空间
     */
    private fun evictUntilEnoughSpace(requiredSpace: Long) {
        val currentSize = getSize()
        val targetSize = maxSize * CLEANUP_THRESHOLD - requiredSpace
        
        if (currentSize <= targetSize) {
            return
        }
        
        // 按访问时间排序，删除最久未访问的文件
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
        Log.d(TAG, "Evicted cache: freed ${currentSize - freedSpace} bytes")
    }

    /**
     * 加载索引文件
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
            Log.d(TAG, "Loaded index: ${index.size} entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load index", e)
            index.clear()
        }
    }

    /**
     * 保存索引文件
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
            Log.e(TAG, "Failed to save index", e)
        }
    }

    /**
     * 缓存条目数据类
     */
    private data class CacheEntry(
        val filename: String,
        val size: Long,
        var lastAccessTime: Long
    )
}

