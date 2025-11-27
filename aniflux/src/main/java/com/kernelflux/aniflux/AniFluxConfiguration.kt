package com.kernelflux.aniflux

import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader

/**
 * AniFlux 配置类
 * 用于配置框架的各种组件
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
class AniFluxConfiguration {
    /**
     * 占位图加载器（业务方实现）
     */
    var placeholderImageLoader: PlaceholderImageLoader? = null
    
    /**
     * 设置占位图加载器
     * 
     * @param loader 占位图加载器实现
     * @return this，支持链式调用
     */
    fun setPlaceholderImageLoader(loader: PlaceholderImageLoader): AniFluxConfiguration {
        this.placeholderImageLoader = loader
        return this
    }
}

