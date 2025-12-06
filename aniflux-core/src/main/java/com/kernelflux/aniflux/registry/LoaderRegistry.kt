package com.kernelflux.aniflux.registry

import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.util.concurrent.ConcurrentHashMap

/**
 * Loader registry
 * Responsible for managing Loader instances for all animation formats
 * 
 * Design references:
 * - Glide's Registry mechanism
 * - Coil's ComponentRegistry
 * - OkHttp's Interceptor registration
 * 
 * @author: kernelflux
 * @date: 2025/01/XX
 */
object LoaderRegistry {
    
    /**
     * Loader storage Map
     * Uses ConcurrentHashMap to ensure thread safety
     */
    private val loaders = ConcurrentHashMap<AnimationTypeDetector.AnimationType, AnimationLoader<*>>()
    
    /**
     * Register Loader
     * 
     * @param type Animation type
     * @param loader Loader instance
     * @return If previously registered, returns old Loader; otherwise returns null
     */
    @JvmStatic
    fun register(type: AnimationTypeDetector.AnimationType, loader: AnimationLoader<*>): AnimationLoader<*>? {
        return loaders.put(type, loader)
    }
    
    /**
     * Get Loader
     * 
     * @param type Animation type
     * @return Loader instance, returns null if not registered
     */
    @JvmStatic
    fun get(type: AnimationTypeDetector.AnimationType): AnimationLoader<*>? {
        return loaders[type]
    }
    
    /**
     * Check if registered
     * 
     * @param type Animation type
     * @return Returns true if registered, otherwise false
     */
    @JvmStatic
    fun isRegistered(type: AnimationTypeDetector.AnimationType): Boolean {
        return loaders.containsKey(type)
    }
    
    /**
     * Unregister
     * 
     * @param type Animation type
     * @return Removed Loader instance, returns null if not registered
     */
    @JvmStatic
    fun unregister(type: AnimationTypeDetector.AnimationType): AnimationLoader<*>? {
        return loaders.remove(type)
    }
    
    /**
     * Get all registered types
     * 
     * @return Set of registered animation types
     */
    @JvmStatic
    fun getRegisteredTypes(): Set<AnimationTypeDetector.AnimationType> {
        return loaders.keys.toSet()
    }
    
    /**
     * Clear all registrations
     * Mainly used for testing
     */
    @JvmStatic
    fun clear() {
        loaders.clear()
    }
    
    /**
     * Get registration count
     * 
     * @return Number of registered Loaders
     */
    @JvmStatic
    fun size(): Int {
        return loaders.size
    }
}

