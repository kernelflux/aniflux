package com.kernelflux.aniflux.listener

import com.kernelflux.aniflux.config.LoadResult

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *
 */
interface MediaRequestListener {
    fun onLoadStarted()
    fun onLoadSucceeded(result: LoadResult.Success)
    fun onLoadFailed(result: LoadResult.Failure)
}