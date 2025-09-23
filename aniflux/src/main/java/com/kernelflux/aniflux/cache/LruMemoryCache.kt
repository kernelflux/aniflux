package com.kernelflux.aniflux.cache

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * LRU内存缓存实现 - 支持泛型
 */
class LruMemoryCache<T : CachedMedia>(private val maxSize: Long) {
    private val cache = LinkedHashMap<String, T>(16, 0.75f, true)
    private var currentSize = 0L

    @Synchronized
    fun get(key: String): T? {
        return cache[key]
    }

    @Synchronized
    fun put(key: String, media: T) {
        val oldValue = cache.put(key, media)
        if (oldValue != null) {
            currentSize -= oldValue.getSize()
        }
        currentSize += media.getSize()

        // 检查是否需要清理
        while (currentSize > maxSize && cache.isNotEmpty()) {
            val eldest = cache.entries.iterator().next()
            cache.remove(eldest.key)
            currentSize -= eldest.value.getSize()
        }
    }

    @Synchronized
    fun remove(key: String) {
        val removed = cache.remove(key)
        if (removed != null) {
            currentSize -= removed.getSize()
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
        currentSize = 0L
    }

    @Synchronized
    fun size(): Long = currentSize
}