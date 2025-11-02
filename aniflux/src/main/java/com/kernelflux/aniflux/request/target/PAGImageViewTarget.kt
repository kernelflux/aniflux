package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import org.libpag.PAGFile
import org.libpag.PAGImageView
import org.libpag.PAGView

/**
 * PAG动画的专用ViewTarget
 * 自动处理PAGFile资源到PAGImageView/PAGView的设置
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class PAGImageViewTarget(view: PAGImageView) : CustomViewAnimationTarget<PAGImageView, PAGFile>(view) {
    
    override fun onResourceReady(resource: PAGFile) {
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)
        
        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: 0
        val autoPlay = animationOptions?.autoPlay ?: true
        
        view.apply {
            composition = resource
            setRepeatCount(repeatCount)
            
            // 如果设置了自动播放，则调用 play()
            if (autoPlay) {
                play()
            }
        }
    }
    
    override fun onLoadFailed(errorDrawable: Drawable?) {
        // PAG 加载失败的处理
    }
    
    override fun onResourceCleared(placeholder: Drawable?) {
        view.composition = null
    }
}
