package com.kernelflux.aniflux.registry

import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.util.concurrent.ConcurrentHashMap

/**
 * Loader注册表
 * 负责管理所有动画格式的Loader实例
 * 
 * 设计参考：
 * - Glide的Registry机制
 * - Coil的ComponentRegistry
 * - OkHttp的Interceptor注册
 * 
 * @author: kernelflux
 * @date: 2025/01/XX
 */
object LoaderRegistry {
    
    /**
     * Loader存储Map
     * 使用ConcurrentHashMap保证线程安全
     */
    private val loaders = ConcurrentHashMap<AnimationTypeDetector.AnimationType, AnimationLoader<*>>()
    
    /**
     * 注册Loader
     * 
     * @param type 动画类型
     * @param loader Loader实例
     * @return 如果之前已注册，返回旧的Loader；否则返回null
     */
    @JvmStatic
    fun register(type: AnimationTypeDetector.AnimationType, loader: AnimationLoader<*>): AnimationLoader<*>? {
        return loaders.put(type, loader)
    }
    
    /**
     * 获取Loader
     * 
     * @param type 动画类型
     * @return Loader实例，如果未注册返回null
     */
    @JvmStatic
    fun get(type: AnimationTypeDetector.AnimationType): AnimationLoader<*>? {
        return loaders[type]
    }
    
    /**
     * 检查是否已注册
     * 
     * @param type 动画类型
     * @return 如果已注册返回true，否则返回false
     */
    @JvmStatic
    fun isRegistered(type: AnimationTypeDetector.AnimationType): Boolean {
        return loaders.containsKey(type)
    }
    
    /**
     * 取消注册
     * 
     * @param type 动画类型
     * @return 被移除的Loader实例，如果未注册返回null
     */
    @JvmStatic
    fun unregister(type: AnimationTypeDetector.AnimationType): AnimationLoader<*>? {
        return loaders.remove(type)
    }
    
    /**
     * 获取所有已注册的类型
     * 
     * @return 已注册的动画类型集合
     */
    @JvmStatic
    fun getRegisteredTypes(): Set<AnimationTypeDetector.AnimationType> {
        return loaders.keys.toSet()
    }
    
    /**
     * 清空所有注册
     * 主要用于测试
     */
    @JvmStatic
    fun clear() {
        loaders.clear()
    }
    
    /**
     * 获取注册数量
     * 
     * @return 已注册的Loader数量
     */
    @JvmStatic
    fun size(): Int {
        return loaders.size
    }
}

