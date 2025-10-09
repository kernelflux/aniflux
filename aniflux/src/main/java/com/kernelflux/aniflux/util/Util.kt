package com.kernelflux.aniflux.util

import android.os.Handler
import android.os.Looper

/**
 * 工具类
 */
object Util {
    @Volatile
    private var mainThreadHandler: Handler? = null

    /**
     * 检查是否在主线程
     */
    @JvmStatic
    fun isOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    /**
     * 检查是否在后台线程
     */
    @JvmStatic
    fun isOnBackgroundThread(): Boolean {
        return !isOnMainThread()
    }

    /**
     * 断言在主线程
     */
    @JvmStatic
    fun assertMainThread() {
        if (!isOnMainThread()) {
            throw IllegalArgumentException("You must call this method on the main thread")
        }
    }

    /**
     * 断言在后台线程
     */
    @JvmStatic
    fun assertBackgroundThread() {
        if (isOnMainThread()) {
            throw IllegalArgumentException("You must call this method on a background thread")
        }
    }

    @JvmStatic
    private fun getUiThreadHandler(): Handler {
        return mainThreadHandler ?: synchronized(Util::class.java) {
            mainThreadHandler ?: Handler(Looper.getMainLooper()).also { mainThreadHandler = it }
        }
    }

    @JvmStatic
    fun postOnUiThread(runnable: Runnable) {
        getUiThreadHandler().post(runnable)
    }

}
