package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
// AnimationPlayListenerSetupHelper 在 aniflux 模块中，格式特定的清理由格式模块处理
import com.kernelflux.aniflux.util.Util

/**
 * @author: kerneflux
 * @date: 2025/10/13
 *
 */
abstract class CustomAnimationTarget<T>(
    width: Int = AnimationTarget.SIZE_ORIGINAL,
    height: Int = AnimationTarget.SIZE_ORIGINAL
) : AnimationTarget<T> {
    protected val width: Int
    protected val height: Int
    private var request: AnimationRequest? = null
    
    // 动画播放监听器（直接持有，无需Manager包装）
    @Volatile
    var playListener: AnimationPlayListener? = null
        private set
    
    // 动画配置选项（用于播放设置）
    @Volatile
    var animationOptions: com.kernelflux.aniflux.util.AnimationOptions? = null
        internal set

    init {
        if (!Util.isValidDimensions(width, height)) {
            throw IllegalArgumentException(
                ("Width and height must both be > 0 or Target#SIZE_ORIGINAL, but given"
                        + " width: "
                        + width
                        + " and height: "
                        + height)
            )
        }
        this.width = width
        this.height = height
    }

    override fun onStart() {
        //
    }

    override fun onStop() {
        //
    }

    override fun onDestroy() {
        // 清理监听器
        cleanupPlayListeners()
    }

    override fun onLoadStarted(placeholder: Drawable?) {
        //
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        //
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        // 清理监听器设置
        cleanupPlayListeners()
    }

    override fun getSize(cb: AnimationSizeReadyCallback) {
        cb.onSizeReady(width, height)
    }

    override fun removeCallback(cb: AnimationSizeReadyCallback) {
        //
    }

    override fun setRequest(request: AnimationRequest?) {
        this.request = request
    }

    override fun getRequest(): AnimationRequest? {
        return this.request
    }
    
    /**
     * 获取目标宽度
     * 使用width()方法名避免Kotlin的get方法冲突
     */
    fun width(): Int {
        return width
    }
    
    /**
     * 获取目标高度
     * 使用height()方法名避免Kotlin的get方法冲突
     */
    fun height(): Int {
        return height
    }


    fun setPlayListener(listener: AnimationPlayListener?): Boolean {
        if (listener == null) return false
        playListener = listener
        return true
    }

    /**
     * 清除监听器
     */
    fun clearPlayListener() {
        playListener = null
    }

    /**
     * 清理监听器设置
     * 在onLoadCleared时自动调用，也会在onDestroy时调用
     * 
     * 注意：具体的清理逻辑在 aniflux 模块的 AnimationPlayListenerSetupHelper 中
     * 这里只做基础清理，格式特定的清理由格式模块处理
     */
    internal fun cleanupPlayListeners() {
        // 基础清理：清除监听器引用
        // 格式特定的清理（如移除动画监听器）由格式模块的 AnimationPlayListenerSetupHelper 处理
        playListener = null
    }
}
