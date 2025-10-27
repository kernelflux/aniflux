package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.request.AnimationRequest
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
        //
    }

    override fun onLoadStarted(placeholder: Drawable?) {
        //
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        //
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
}
