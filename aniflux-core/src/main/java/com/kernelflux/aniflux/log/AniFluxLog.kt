package com.kernelflux.aniflux.log

/**
 * AniFlux unified logging API
 * 
 * Provides a centralized logging interface for the entire AniFlux framework.
 * External apps can customize logging behavior by setting a custom logger.
 * 
 * Usage:
 * ```kotlin
 * // Simple usage
 * AniFluxLog.i("CustomViewAnimationTarget", "View attached")
 * 
 * // With category
 * AniFluxLog.i(AniFluxLogCategory.TARGET, "View attached")
 * 
 * // With exception
 * AniFluxLog.e("AnimationJob", "Failed to load", exception)
 * 
 * // Conditional logging (performance optimization)
 * if (AniFluxLog.isLoggable("Tag", AniFluxLogLevel.DEBUG)) {
 *     AniFluxLog.d("Tag", "Expensive log: ${expensiveOperation()}")
 * }
 * ```
 * 
 * Configuration:
 * ```kotlin
 * // Set custom logger
 * AniFluxLog.setLogger(customLogger)
 * 
 * // Set minimum log level
 * AniFluxLog.setMinLogLevel(AniFluxLogLevel.DEBUG)
 * ```
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
object AniFluxLog {
    @Volatile
    private var logger: AniFluxLogger = DefaultAniFluxLogger()
    
    /**
     * Set custom logger implementation
     */
    @JvmStatic
    fun setLogger(logger: AniFluxLogger) {
        this.logger = logger
    }
    
    /**
     * Get current logger
     */
    @JvmStatic
    fun getLogger(): AniFluxLogger = logger
    
    /**
     * Set minimum log level (creates a new DefaultAniFluxLogger with the specified level)
     */
    @JvmStatic
    fun setMinLogLevel(level: AniFluxLogLevel) {
        logger = DefaultAniFluxLogger(level)
    }
    
    /**
     * Check if a log level is loggable for a tag
     */
    @JvmStatic
    fun isLoggable(tag: String, level: AniFluxLogLevel): Boolean {
        return logger.isLoggable(tag, level)
    }
    
    // ========== Verbose ==========
    
    @JvmStatic
    fun v(tag: String, message: String) {
        logger.v(tag, message)
    }
    
    @JvmStatic
    fun v(tag: String, message: String, throwable: Throwable) {
        logger.v(tag, message, throwable)
    }
    
    @JvmStatic
    fun v(category: AniFluxLogCategory, message: String) {
        logger.v(category.tag, message)
    }
    
    @JvmStatic
    fun v(category: AniFluxLogCategory, message: String, throwable: Throwable) {
        logger.v(category.tag, message, throwable)
    }
    
    // ========== Debug ==========
    
    @JvmStatic
    fun d(tag: String, message: String) {
        logger.d(tag, message)
    }
    
    @JvmStatic
    fun d(tag: String, message: String, throwable: Throwable) {
        logger.d(tag, message, throwable)
    }
    
    @JvmStatic
    fun d(category: AniFluxLogCategory, message: String) {
        logger.d(category.tag, message)
    }
    
    @JvmStatic
    fun d(category: AniFluxLogCategory, message: String, throwable: Throwable) {
        logger.d(category.tag, message, throwable)
    }
    
    // ========== Info ==========
    
    @JvmStatic
    fun i(tag: String, message: String) {
        logger.i(tag, message)
    }
    
    @JvmStatic
    fun i(tag: String, message: String, throwable: Throwable) {
        logger.i(tag, message, throwable)
    }
    
    @JvmStatic
    fun i(category: AniFluxLogCategory, message: String) {
        logger.i(category.tag, message)
    }
    
    @JvmStatic
    fun i(category: AniFluxLogCategory, message: String, throwable: Throwable) {
        logger.i(category.tag, message, throwable)
    }
    
    // ========== Warn ==========
    
    @JvmStatic
    fun w(tag: String, message: String) {
        logger.w(tag, message)
    }
    
    @JvmStatic
    fun w(tag: String, message: String, throwable: Throwable) {
        logger.w(tag, message, throwable)
    }
    
    @JvmStatic
    fun w(category: AniFluxLogCategory, message: String) {
        logger.w(category.tag, message)
    }
    
    @JvmStatic
    fun w(category: AniFluxLogCategory, message: String, throwable: Throwable) {
        logger.w(category.tag, message, throwable)
    }
    
    // ========== Error ==========
    
    @JvmStatic
    fun e(tag: String, message: String) {
        logger.e(tag, message)
    }
    
    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable) {
        logger.e(tag, message, throwable)
    }
    
    @JvmStatic
    fun e(category: AniFluxLogCategory, message: String) {
        logger.e(category.tag, message)
    }
    
    @JvmStatic
    fun e(category: AniFluxLogCategory, message: String, throwable: Throwable) {
        logger.e(category.tag, message, throwable)
    }
}

