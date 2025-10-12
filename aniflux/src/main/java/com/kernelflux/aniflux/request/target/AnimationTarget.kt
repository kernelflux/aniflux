package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.aniflux.manager.AnimationLifecycleListener
import com.kernelflux.aniflux.request.AnimationRequest

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
interface AnimationTarget<R> : AnimationLifecycleListener {

    companion object {
        const val SIZE_ORIGINAL = Int.MIN_VALUE
    }

    fun onLoadStarted(placeholder: Drawable?)
    fun onLoadFailed(errorDrawable: Drawable?)
    fun onResourceReady(resource: R)
    fun onLoadCleared(placeholder: Drawable?)
    fun getSize(cb: AnimationSizeReadyCallback)
    fun removeCallback(cb: AnimationSizeReadyCallback)
    fun setRequest(request: AnimationRequest?)
    fun getRequest(): AnimationRequest?
}
