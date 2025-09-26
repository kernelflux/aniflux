package com.kernelflux.aniflux.request

import com.kernelflux.aniflux.config.MediaTarget

/**
 * @author: kerneflux
 * @date: 2025/9/26
 *
 */
interface MediaRequestListener<R> {

    fun onLoadFailed(
        e: Throwable,
        model: Any,
        target: MediaTarget<R>,
        isFirstResource: Boolean
    ): Boolean

    fun onResourceReady(
        resource: R,
        model: Any,
        target: MediaTarget<R>,
        isFirstResource: Boolean
    ): Boolean


}