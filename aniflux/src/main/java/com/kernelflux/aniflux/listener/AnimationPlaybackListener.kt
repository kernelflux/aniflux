package com.kernelflux.aniflux.listener

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 动画播放监听器接口
 */
interface AnimationPlaybackListener {
    /**
     * 动画开始播放
     */
    fun onAnimationStart()

    /**
     * 动画播放结束
     */
    fun onAnimationEnd()

    /**
     * 动画播放取消
     */
    fun onAnimationCancel()

    /**
     * 动画重复播放
     * @param currentCount 当前重复次数
     * @param totalCount 总重复次数
     */
    fun onAnimationRepeat(currentCount: Int, totalCount: Int)
}

/**
 * 动画播放监听器适配器 - 提供默认实现
 */
abstract class AnimationPlaybackListenerAdapter : AnimationPlaybackListener {

    override fun onAnimationStart() {
        // 默认空实现
    }

    override fun onAnimationEnd() {
        // 默认空实现
    }

    override fun onAnimationCancel() {
        // 默认空实现
    }

    override fun onAnimationRepeat(currentCount: Int, totalCount: Int) {
        // 默认空实现
    }
}
