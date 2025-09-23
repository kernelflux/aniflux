package com.kernelflux.aniflux.loader

import com.kernelflux.aniflux.config.LoadOptions
import com.kernelflux.aniflux.config.LoadResource
import com.kernelflux.aniflux.config.MediaTarget

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 媒体加载器工厂
 */
object MediaLoaderFactory {
    private val loaderChain = MediaLoaderChain()
        .addLoader(PAGMediaLoader())

    fun getLoader(source: LoadResource): MediaLoader? {
        return loaderChain.getCapableLoaders(source).firstOrNull()
    }

    suspend fun load(
        source: LoadResource,
        options: LoadOptions,
        target: MediaTarget
    ): Any? {
        return loaderChain.load(source, options, target)
    }
}