package com.kernelflux.aniflux.request.listener

/**
 * 统一的动画播放监听接口
 * 提供动画播放过程中的各种事件回调
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
interface AnimationPlayListener {
    
    /**
     * 动画开始播放
     * 在动画首次开始播放时调用
     */
    fun onAnimationStart() {
        // 默认空实现，子类可以选择性重写
    }
    
    /**
     * 动画播放结束
     * 在动画正常播放完成时调用（不包括取消的情况）
     */
    fun onAnimationEnd() {
        // 默认空实现，子类可以选择性重写
    }
    
    /**
     * 动画播放被取消
     * 在动画播放被取消时调用（如调用stop()方法）
     */
    fun onAnimationCancel() {
        // 默认空实现，子类可以选择性重写
    }
    
    /**
     * 动画播放重复
     * 在动画循环播放重新开始时调用
     */
    fun onAnimationRepeat() {
        // 默认空实现，子类可以选择性重写
    }
    
    /**
     * 动画播放失败
     * 在动画播放过程中发生错误时调用
     * 
     * @param error 错误信息
     */
    fun onAnimationFailed(error: Throwable?) {
        // 默认空实现，子类可以选择性重写
    }
}

