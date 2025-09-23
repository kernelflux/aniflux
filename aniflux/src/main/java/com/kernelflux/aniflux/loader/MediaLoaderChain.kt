package com.kernelflux.aniflux.loader

import com.kernelflux.aniflux.config.LoadOptions
import com.kernelflux.aniflux.config.LoadResource
import com.kernelflux.aniflux.config.MediaTarget


/**
 * @author: kerneflux
 * @date: 2025/9/21
 *
 */
class MediaLoaderChain {
    private val loaders = mutableListOf<MediaLoader>()
    private val fallbackLoaders = mutableListOf<MediaLoader>()

    fun addLoader(loader: MediaLoader): MediaLoaderChain {
        loaders.add(loader)
        return this
    }

    fun addFallbackLoader(loader: MediaLoader): MediaLoaderChain {
        fallbackLoaders.add(loader)
        return this
    }

    suspend fun load(
        source: LoadResource,
        options: LoadOptions,
        target: MediaTarget
    ): Any? {
        // 尝试主要加载器
        for (loader in loaders) {
            if (loader.canHandle(source)) {
                try {
                    return loader.load(source, options, target)
                } catch (e: Exception) {
                    // 记录错误，继续尝试下一个
                    println("Loader ${loader::class.simpleName} failed: ${e.message}")
                }
            }
        }

        // 尝试兜底加载器
        for (loader in fallbackLoaders) {
            if (loader.canHandle(source)) {
                try {
                    return loader.load(source, options, target)
                } catch (e: Exception) {
                    println("Fallback loader ${loader::class.simpleName} failed: ${e.message}")
                }
            }
        }
        return null
    }

    fun getCapableLoaders(source: LoadResource): List<MediaLoader> {
        return (loaders + fallbackLoaders).filter { it.canHandle(source) }
    }
}