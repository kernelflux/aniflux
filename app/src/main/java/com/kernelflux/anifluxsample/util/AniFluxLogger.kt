package com.kernelflux.anifluxsample.util

import android.util.Log

/**
 * Test logger for AniFlux sample app
 * 
 * Usage:
 * - AniFluxLogger.i("message") - Info level
 * - AniFluxLogger.d("message") - Debug level
 * - AniFluxLogger.w("message") - Warning level
 * - AniFluxLogger.e("message", exception) - Error level
 * 
 * View logs:
 * - Android Studio Logcat: filter by tag "aniflux_logger_tag"
 * - adb logcat: adb logcat -s aniflux_logger_tag
 * 
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

    @JvmStatic
    fun d(msg: String) {
        d(TAG, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    @JvmStatic
    fun w(msg: String) {
        w(TAG, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    @JvmStatic
    fun e(msg: String, throwable: Throwable? = null) {
        e(TAG, msg, throwable)
    }

    @JvmStatic
    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, msg, throwable)
        } else {
            Log.e(tag, msg)
        }
    }
}