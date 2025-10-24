package com.kernelflux.aniflux.request

import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.request.target.AnimationTarget

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
interface AnimationRequestListener<R> {

    fun onLoadFailed(
        exception: Throwable,
        model: Any?,
        target: AnimationTarget<R>,
        isFirstResource: Boolean
    ): Boolean

    fun onResourceReady(
        resource: R,
        model: Any?,
        target: AnimationTarget<R>,
        dataSource: AnimationDataSource,
        isFirstResource: Boolean
    ): Boolean

}

