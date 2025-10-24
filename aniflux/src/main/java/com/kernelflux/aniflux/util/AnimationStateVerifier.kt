package com.kernelflux.aniflux.util

/**
 * 动画请求状态验证器
 * 用于检测对象是否已被回收，防止在已回收对象上调用方法
 */
class AnimationStateVerifier private constructor() {
    
    companion object {
        private const val IS_REUSING = "StateVerifier{isReusing=true}"
        private const val IS_NOT_REUSING = "StateVerifier{isReusing=false}"
        
        fun newInstance(): AnimationStateVerifier {
            return AnimationStateVerifier()
        }
    }
    
    private var isReusing = false
    
    /**
     * 标记对象为正在重用状态
     */
    fun setReusing() {
        isReusing = true
    }
    
    /**
     * 标记对象为非重用状态
     */
    fun setNotReusing() {
        isReusing = false
    }
    
    /**
     * 如果对象正在重用，抛出异常
     */
    fun throwIfRecycled() {
        if (isReusing) {
            throw IllegalStateException("Cannot access object after it has been recycled")
        }
    }
    
    override fun toString(): String {
        return if (isReusing) IS_REUSING else IS_NOT_REUSING
    }
}
