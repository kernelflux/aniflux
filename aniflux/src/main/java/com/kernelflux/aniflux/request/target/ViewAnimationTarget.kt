package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import android.view.View
import com.kernelflux.aniflux.request.AnimationRequest

/**
 * 将动画加载到View中的Target实现
 * @author: kernelflux
 * @date: 2025/10/13
 */
class ViewAnimationTarget(
    private val view: View,
    width: Int = AnimationTarget.SIZE_ORIGINAL,
    height: Int = AnimationTarget.SIZE_ORIGINAL
) : CustomAnimationTarget<Drawable>(width, height) {

    private var placeholder: Drawable? = null
    private var errorDrawable: Drawable? = null

    override fun onLoadStarted(placeholder: Drawable?) {
        this.placeholder = placeholder
        view.background = placeholder
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        this.errorDrawable = errorDrawable
        view.background = errorDrawable
    }

    override fun onResourceReady(resource: Drawable) {
        view.background = resource
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        view.background = placeholder
    }

    override fun getSize(cb: AnimationSizeReadyCallback) {
        if (width == AnimationTarget.SIZE_ORIGINAL || height == AnimationTarget.SIZE_ORIGINAL) {
            // 如果尺寸是原始尺寸，使用View的实际尺寸
            view.post {
                val viewWidth = if (width == AnimationTarget.SIZE_ORIGINAL) {
                    view.width
                } else {
                    width
                }
                val viewHeight = if (height == AnimationTarget.SIZE_ORIGINAL) {
                    view.height
                } else {
                    height
                }
                cb.onSizeReady(viewWidth, viewHeight)
            }
        } else {
            super.getSize(cb)
        }
    }
}
