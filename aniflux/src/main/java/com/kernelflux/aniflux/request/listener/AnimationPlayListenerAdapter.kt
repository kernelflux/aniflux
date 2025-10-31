package com.kernelflux.aniflux.request.listener

/**
 * 动画播放监听器适配器
 * 提供空的默认实现，方便使用者只关注需要的方法
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
open class AnimationPlayListenerAdapter : AnimationPlayListener {
    
    override fun onAnimationStart() {
        // 空实现
    }
    
    override fun onAnimationEnd() {
        // 空实现
    }
    
    override fun onAnimationCancel() {
        // 空实现
    }
    
    override fun onAnimationRepeat() {
        // 空实现
    }
    
    override fun onAnimationFailed(error: Throwable?) {
        // 空实现
    }
}

