package com.kernelflux.anifluxsample

import android.util.Log

/**
 * @author: QT
 * @date: 2025/10/27
 */
object AniFluxLogger {
    private const val TAG = "aniflux_logger_tag"


    @JvmStatic
    fun i(msg: String) {
        i(TAG, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

}