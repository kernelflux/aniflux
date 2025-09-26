package com.kernelflux.aniflux.request.target

/**
 * @author: kerneflux
 * @date: 2025/9/26
 *
 */
interface MediaSizeReadyCallback {
    fun onSizeReady(width: Int, height: Int)
}