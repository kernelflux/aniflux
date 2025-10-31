package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerSetupHelper
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

/**
 * GIF动画的专用ViewTarget
 * 自动处理GifDrawable资源到GifImageView的设置
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class GifViewTarget(view: GifImageView) : CustomViewAnimationTarget<GifImageView, GifDrawable>(view) {
    
    override fun onResourceReady(resource: GifDrawable) {
        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: -1
        
        // 根据配置设置 GIF 的循环次数
        // GifDrawable 的 loopCount: 0=无限循环，1=播放一次，>1=循环次数
        val loopCount = when {
            repeatCount < 0 -> 0 // -1 表示无限循环，转换为 0
            repeatCount == 0 -> 1 // 0 表示不循环，但 GIF 必须至少播放一次，所以设为 1
            else -> repeatCount // >0 表示循环次数
        }
        resource.loopCount = loopCount
        
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)
        
        // 设置 drawable（GIF 会自动开始播放）
        view.setImageDrawable(resource)
    }
    
    override fun onLoadFailed(errorDrawable: Drawable?) {
        // GIF 加载失败的处理
        view.setImageDrawable(errorDrawable)
    }
    
    override fun onResourceCleared(placeholder: Drawable?) {
        view.setImageDrawable(placeholder)
    }
}

