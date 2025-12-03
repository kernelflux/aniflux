package com.kernelflux.aniflux.pag

import android.graphics.drawable.Drawable
import android.view.View
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.placeholder.PlaceholderManager
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView

/**
 * PAG动画的专用ViewTarget
 * 自动处理PAGFile资源到PAGImageView/PAGView的设置
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
class PAGImageViewTarget(view: PAGImageView) : CustomViewAnimationTarget<PAGImageView, PAGFile>(view) {
    
    private var placeholderManager: PlaceholderManager? = null
    private var currentAdapter: PAGImageViewPlayListenerAdapter? = null
    private var currentListener: PAGImageView.PAGImageViewListener? = null
    
    override fun setupPlayListeners(resource: Any, view: View?) {
        val pagImageView = view as? PAGImageView ?: return
        val listener = playListener ?: return
        
        // 移除旧的监听器
        currentListener?.let { oldListener ->
            try {
                pagImageView.removeListener(oldListener)
            } catch (e: Exception) {
                // 忽略移除时的异常
            }
        }
        
        // 获取 retainLastFrame 配置
        val retainLastFrame = animationOptions?.retainLastFrame ?: true
        
        // 创建新的适配器
        val adapter = PAGImageViewPlayListenerAdapter(listener, pagImageView, retainLastFrame)
        val pagListener = adapter.createAnimatorListener()
        pagImageView.addListener(pagListener)
        
        // 保存引用以便清理
        currentAdapter = adapter
        currentListener = pagListener
    }
    
    override fun onResourceReady(resource: PAGFile) {
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)
        // 获取配置选项
        val repeatCount = animationOptions?.repeatCount ?: 0
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
        
        // 处理占位图替换
        animationOptions?.placeholderReplacements?.let { replacements ->
            // 先清理旧的占位图管理器（如果存在）
            placeholderManager?.clear()
            placeholderManager = null
            
            val imageLoader = AniFlux.get(view.context).getPlaceholderImageLoader()
            if (imageLoader != null) {
                val lifecycle = getLifecycle()
                
                placeholderManager = PlaceholderManagerFactory.create(
                    view = view,
                    resource = resource,
                    replacements = replacements,
                    imageLoader = imageLoader,
                    lifecycle = lifecycle
                )
                
                placeholderManager?.applyReplacements()
            }
        }
    }
    
    override fun onLoadFailed(errorDrawable: Drawable?) {
        // PAG 加载失败的处理
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
        placeholderManager = null
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
        
        try {
            placeholderManager?.clear()
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
        placeholderManager = null
        try {
            view.composition = null
        } catch (e: Exception) {
            // 忽略清理时的异常
        }
    }
}

