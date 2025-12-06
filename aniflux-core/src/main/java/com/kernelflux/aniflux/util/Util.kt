package com.kernelflux.aniflux.util

import android.os.Handler
import android.os.Looper
import com.kernelflux.aniflux.request.target.AnimationTarget

/**
 * Utility class
 */
object Util {
    @Volatile
    private var mainThreadHandler: Handler? = null

    /**
     * Check if on main thread
     */
    @JvmStatic
    fun isOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    /**
     * Check if on background thread
     */
    @JvmStatic
    fun isOnBackgroundThread(): Boolean {
        return !isOnMainThread()
    }

    /**
     * Assert on main thread
     */
    @JvmStatic
    fun assertMainThread() {
        if (!isOnMainThread()) {
            throw IllegalArgumentException("You must call this method on the main thread")
        }
    }

    /**
     * Assert on background thread
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
