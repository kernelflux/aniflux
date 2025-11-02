package com.kernelflux.aniflux.cache

import java.io.File

/**
 * 磁盘缓存接口
 * 用于管理动画文件的磁盘缓存
 */
interface AnimationDiskCache {
    /**
     * 获取缓存文件
     * @param key 缓存键
     * @return 缓存文件，如果不存在返回 null
     */
    fun get(key: String): File?
    
    /**
     * 保存到磁盘缓存
     * @param key 缓存键
     * @param file 要缓存的文件
     */
    fun put(key: String, file: File)
    
    /**
     * 删除缓存
     * @param key 缓存键
     */
    fun remove(key: String)
    
    /**
     * 清空缓存
     */
    fun clear()
    
    /**
     * 获取缓存大小（字节）
     */
    fun getSize(): Long
    
    /**
     * 获取最大缓存大小（字节）
     */
    fun getMaxSize(): Long
}

