package com.kernelflux.aniflux.utils

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *
 */
object Preconditions {

    @JvmStatic
    fun checkArgument(expression: Boolean) {
        checkArgument(expression, "")
    }

    @JvmStatic
    fun checkArgument(expression: Boolean, message: String) {
        if (!expression) {
            throw IllegalStateException(message)
        }
    }

    @JvmStatic
    fun <T> checkNotNull(arg: T?): T {
        return checkNotNull(arg, "Argument must not be null")
    }

    @JvmStatic
    fun <T> checkNotNull(arg: T?, message: String): T {
        if (arg == null) {
            throw NullPointerException(message)
        }
        return arg
    }

    @JvmStatic
    fun checkNotEmpty(string: String?): String {
        if (string.isNullOrEmpty()) {
            throw IllegalArgumentException("Must not be null or empty")
        }
        return string
    }

    @JvmStatic
    fun <T : Collection<Y>, Y> checkNotEmpty(collection: T?): T {
        if (collection.isNullOrEmpty()) {
            throw IllegalArgumentException("Must not be empty.")
        }
        return collection
    }
}
