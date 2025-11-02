package com.kernelflux.aniflux.request.listener

import android.os.Handler
import android.os.Looper

/**
 * @author: kerneflux
 * @date: 2025/11/2
 *
 */
abstract class InternalBasePlayListenerAdapter<T>(private val listener: AnimationPlayListener) {
    private val mHandler = Handler(Looper.getMainLooper())

    abstract fun createAnimatorListener(loopCount: Int? = null): T

    fun onClear() {
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun shouldNotifyInMainLoop(): Boolean {
        return Looper.myLooper() != Looper.getMainLooper()
    }

    fun notifyAnimationStart() {
        if (shouldNotifyInMainLoop()) {
            mHandler.post { listener.onAnimationStart() }
        } else {
            listener.onAnimationStart()
        }
    }


    fun notifyAnimationEnd() {
        if (shouldNotifyInMainLoop()) {
            mHandler.post { listener.onAnimationEnd() }
        } else {
            listener.onAnimationEnd()
        }
    }

    fun notifyAnimationCancel() {
        if (shouldNotifyInMainLoop()) {
            mHandler.post { listener.onAnimationCancel() }
        } else {
            listener.onAnimationCancel()
        }
    }

    fun notifyAnimationRepeat() {
        if (shouldNotifyInMainLoop()) {
            mHandler.post { listener.onAnimationRepeat() }
        } else {
            listener.onAnimationRepeat()
        }
    }

    fun notifyAnimationUpdate(currentFrame: Int, totalFrames: Int) {
        if (shouldNotifyInMainLoop()) {
            mHandler.post { listener.onAnimationUpdate(currentFrame, totalFrames) }
        } else {
            listener.onAnimationUpdate(currentFrame, totalFrames)
        }
    }

    fun notifyAnimationFailed(error: Throwable?) {
        if (shouldNotifyInMainLoop()) {
            mHandler.post { listener.onAnimationFailed(error) }
        } else {
            listener.onAnimationFailed(error)
        }
    }

}