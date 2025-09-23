package com.kernelflux.aniflux.utils

import android.os.Handler
import android.os.Looper
import kotlin.concurrent.Volatile

/**
 * @author: kerneflux
 * @date: 2025/9/22
 *
 */
object Util {

    @Volatile
    private var mainThreadHandler: Handler? = null


    fun postOnUiThread(runnable: Runnable) {
        getUiThreadHandler().post(runnable)
    }

    @JvmStatic
    private fun getUiThreadHandler(): Handler {
        return mainThreadHandler ?: synchronized(Util::class.java) {
            mainThreadHandler ?: Handler(Looper.getMainLooper()).also { mainThreadHandler = it }
        }
    }

    @JvmStatic
    fun isOnBackgroundThread(): Boolean {
        return !isOnMainThread()
    }

    @JvmStatic
    fun isOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    @JvmStatic
    fun assertMainThread() {
        if (!isOnMainThread()) {
            throw IllegalArgumentException("You must call this method on the main thread")
        }
    }


}