package com.kernelflux.aniflux.pag

import android.graphics.drawable.Drawable
import android.view.View
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGView

/**
 * @author: kerneflux
 * @date: 2025/11/2
 * PAGView的专用Target（用于PAGView，而非PAGImageView）
 */
class PAGViewTarget(view: PAGView) : CustomViewAnimationTarget<PAGView, PAGFile>(view) {

    private var currentAdapter: PAGViewPlayListenerAdapter? = null
    private var currentListener: PAGView.PAGViewListener? = null
    
    override fun setupPlayListeners(resource: Any, view: View?) {
        val pagView = view as? PAGView ?: return
        val listener = playListener ?: return
        
        // 移除旧的监听器
        currentListener?.let { oldListener ->
            try {
                pagView.removeListener(oldListener)
            } catch (e: Exception) {
                // 忽略移除时的异常
            }
        }
        
        // 获取 retainLastFrame 配置
        val retainLastFrame = animationOptions?.retainLastFrame ?: true
        
        // 创建新的适配器
        val adapter = PAGViewPlayListenerAdapter(listener, pagView, retainLastFrame)
        val pagListener = adapter.createAnimatorListener()
        pagView.addListener(pagListener)
        
        // 保存引用以便清理
        currentAdapter = adapter
        currentListener = pagListener
    }

    override fun onResourceReady(resource: PAGFile) {
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)

        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: -1
        val autoPlay = animationOptions?.autoPlay ?: true
        view.apply {
            //防止多个view共用导致的复用问题
            composition = resource.copyOriginal()
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
        // 清理监听器
        currentListener?.let { listener ->
            try {
                view.removeListener(listener)
            } catch (e: Exception) {
                // 忽略清理时的异常
            }
        }
        currentAdapter?.onClear()
        currentAdapter = null
        currentListener = null
        
        view.composition = null
    }
}

