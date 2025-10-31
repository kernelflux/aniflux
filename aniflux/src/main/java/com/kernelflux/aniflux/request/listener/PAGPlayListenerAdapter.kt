package com.kernelflux.aniflux.request.listener

import org.libpag.PAGImageView
import org.libpag.PAGView
import java.lang.ref.WeakReference

/**
 * PAG动画播放监听器适配器
 * 将PAG的监听接口适配到统一的AnimationPlayListener
 * 
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class PAGPlayListenerAdapter(
    private val listener: AnimationPlayListener
) {
    
    /**
     * PAGView的监听器实现
     */
    fun createPAGViewListener(): PAGView.PAGViewListener {
        return object : PAGView.PAGViewListener {
            override fun onAnimationStart(view: PAGView) {
                listener.onAnimationStart()
            }
            
            override fun onAnimationEnd(view: PAGView) {
                listener.onAnimationEnd()
            }
            
            override fun onAnimationCancel(view: PAGView) {
                listener.onAnimationCancel()
            }
            
            override fun onAnimationRepeat(view: PAGView) {
                listener.onAnimationRepeat()
            }
            
            override fun onAnimationUpdate(view: PAGView) {
                // PAG特有的更新回调，暂不映射到统一接口
            }
        }
    }
    
    /**
     * PAGImageView的监听器实现
     */
    fun createPAGImageViewListener(): PAGImageView.PAGImageViewListener {
        return object : PAGImageView.PAGImageViewListener {
            override fun onAnimationStart(view: PAGImageView) {
                android.util.Log.d("PAGPlayListenerAdapter", "PAG onAnimationStart called")
                listener.onAnimationStart()
            }
            
            override fun onAnimationEnd(view: PAGImageView) {
                android.util.Log.d("PAGPlayListenerAdapter", "PAG onAnimationEnd called")
                listener.onAnimationEnd()
            }
            
            override fun onAnimationCancel(view: PAGImageView) {
                android.util.Log.d("PAGPlayListenerAdapter", "PAG onAnimationCancel called")
                listener.onAnimationCancel()
            }
            
            override fun onAnimationRepeat(view: PAGImageView) {
                android.util.Log.d("PAGPlayListenerAdapter", "PAG onAnimationRepeat called")
                listener.onAnimationRepeat()
            }
            
            override fun onAnimationUpdate(view: PAGImageView) {
                // PAG特有的更新回调，暂不映射到统一接口
            }
        }
    }
}

