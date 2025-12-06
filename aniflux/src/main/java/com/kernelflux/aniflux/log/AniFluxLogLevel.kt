package com.kernelflux.aniflux.log

/**
 * AniFlux log levels
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
enum class AniFluxLogLevel(val priority: Int) {
    /**
     * Verbose - Most detailed logs, typically only enabled during development
     */
    VERBOSE(android.util.Log.VERBOSE),
    
    /**
     * Debug - Detailed logs for debugging
     */
    DEBUG(android.util.Log.DEBUG),
    
    /**
     * Info - General informational messages
     */
    INFO(android.util.Log.INFO),
    
    /**
     * Warn - Warning messages for potentially harmful situations
     */
    WARN(android.util.Log.WARN),
    
    /**
     * Error - Error messages for error events
     */
    ERROR(android.util.Log.ERROR),
    
    /**
     * None - Disable all logs
     */
    NONE(Int.MAX_VALUE);
    
    companion object {
        /**
         * Get log level from Android Log priority
         */
        fun fromPriority(priority: Int): AniFluxLogLevel {
            return when (priority) {
                android.util.Log.VERBOSE -> VERBOSE
                android.util.Log.DEBUG -> DEBUG
                android.util.Log.INFO -> INFO
                android.util.Log.WARN -> WARN
                android.util.Log.ERROR -> ERROR
                else -> NONE
            }
        }
    }
}

