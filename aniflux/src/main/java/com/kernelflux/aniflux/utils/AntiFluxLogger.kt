package com.kernelflux.aniflux.utils
/**
 * @author: kerneflux
 * @date: 2025/9/23
 *
 */
object AntiFluxLogger {
    private var loggable: Boolean = false

    @JvmStatic
    fun setDebuggable(flag: Boolean) {
        loggable = flag
    }

    @JvmStatic
    fun d(tag: String, msgFunc: () -> String) {
        if (!loggable) {
            return
        }
        msgFunc.invoke()
    }
}