package com.kernelflux.aniflux.log

import android.util.Log

/**
 * Default AniFlux logger implementation using Android Log
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
class DefaultAniFluxLogger(
    private val minLogLevel: AniFluxLogLevel = AniFluxLogLevel.INFO
) : AniFluxLogger {
    
    override fun v(tag: String, message: String, throwable: Throwable?) {
        if (isLoggable(tag, AniFluxLogLevel.VERBOSE)) {
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        if (isLoggable(tag, AniFluxLogLevel.DEBUG)) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        if (isLoggable(tag, AniFluxLogLevel.INFO)) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (isLoggable(tag, AniFluxLogLevel.WARN)) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (isLoggable(tag, AniFluxLogLevel.ERROR)) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    override fun isLoggable(tag: String, level: AniFluxLogLevel): Boolean {
        // Check Android Log's loggable first
        if (!Log.isLoggable(tag, level.priority)) {
            return false
        }
        // Then check our min log level
        return level.priority >= minLogLevel.priority
    }
}

