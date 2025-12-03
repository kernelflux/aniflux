package com.kernelflux.aniflux.util

import android.os.Handler
import android.os.Looper
import com.kernelflux.aniflux.request.target.AnimationTarget

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

    @JvmStatic
    fun removeCallbacksOnUiThread(runnable: Runnable){
        getUiThreadHandler().removeCallbacks(runnable)
    }

    @JvmStatic
    fun <T> getSnapshot(other: MutableCollection<T>): MutableList<T> {
        val result: MutableList<T> = ArrayList<T>(other.size)
        for (item in other) {
            if (item != null) {
                result.add(item)
            }
        }
        return result
    }

    @JvmStatic
    fun isValidDimensions(width: Int, height: Int): Boolean {
        return isValidDimension(width) && isValidDimension(height)
    }

    @JvmStatic
    fun isValidDimension(dimen: Int): Boolean {
        return dimen > 0 || dimen == AnimationTarget.SIZE_ORIGINAL
    }


}
