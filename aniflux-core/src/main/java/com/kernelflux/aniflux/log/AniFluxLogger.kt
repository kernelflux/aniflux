package com.kernelflux.aniflux.log

/**
 * AniFlux logger interface
 * Allows external customization of logging behavior
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
interface AniFluxLogger {
    /**
     * Log a verbose message
     */
    fun v(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Log a debug message
     */
    fun d(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Log an info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Log a warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Log an error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Check if a log level is loggable
     */
    fun isLoggable(tag: String, level: AniFluxLogLevel): Boolean
}

