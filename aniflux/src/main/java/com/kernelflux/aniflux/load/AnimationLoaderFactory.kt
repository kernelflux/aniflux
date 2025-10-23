package com.kernelflux.aniflux.load

import com.kernelflux.aniflux.util.AnimationTypeDetector

/**
 * 动画加载器工厂 - 根据动画类型创建对应的加载器
 * 参考各动画库的加载方式，提供统一的加载接口
 */
object AnimationLoaderFactory {
    
    /**
     * 根据动画类型创建对应的加载器
     */
    fun createLoader(animationType: AnimationTypeDetector.AnimationType): AnimationLoader<*>? {
        return when (animationType) {
            AnimationTypeDetector.AnimationType.GIF -> GifAnimationLoader()
            AnimationTypeDetector.AnimationType.LOTTIE -> LottieAnimationLoader()
            AnimationTypeDetector.AnimationType.PAG -> PagAnimationLoader()
            AnimationTypeDetector.AnimationType.SVGA -> SvgaAnimationLoader()
            AnimationTypeDetector.AnimationType.UNKNOWN -> null
        }
    }
    
    /**
     * 创建GIF加载器
     */
    fun createGifLoader(): AnimationLoader<pl.droidsonroids.gif.GifDrawable> {
        return GifAnimationLoader()
    }
    
    /**
     * 创建Lottie加载器
     */
    fun createLottieLoader(): AnimationLoader<com.airbnb.lottie.LottieDrawable> {
        return LottieAnimationLoader()
    }
    
    /**
     * 创建PAG加载器
     */
    fun createPagLoader(): AnimationLoader<org.libpag.PAGFile> {
        return PagAnimationLoader()
    }
    
    /**
     * 创建SVGA加载器
     */
    fun createSvgaLoader(): AnimationLoader<com.opensource.svgaplayer.SVGADrawable> {
        return SvgaAnimationLoader()
    }
}
