package com.kernelflux.aniflux.loader

import com.kernelflux.aniflux.config.LoadOptions
import com.kernelflux.aniflux.config.LoadResource
import com.kernelflux.aniflux.config.MediaTarget


/**
 * @author: kerneflux
 * @date: 2025/9/21
 *  媒体加载器接口
 */
interface MediaLoader {
    fun canHandle(source: LoadResource): Boolean
    suspend fun load(source: LoadResource, options: LoadOptions, target: MediaTarget): Any
}