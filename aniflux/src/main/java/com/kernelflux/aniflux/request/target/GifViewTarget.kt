package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView

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
        // ✅ GIF 的 loopCount 语义：0=无限循环，N=播放N次（总播放次数）
        // 统一 API 语义：repeatCount <= 0 = 无限循环，N = 总播放N次
        resource.loopCount = when {
            repeatCount <= 0 -> 0  // 无限循环
            else -> repeatCount     // 总播放次数
        }
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

