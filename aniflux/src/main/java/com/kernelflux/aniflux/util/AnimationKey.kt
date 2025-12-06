package com.kernelflux.aniflux.util

import android.net.Uri
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import java.io.File
import java.security.MessageDigest

/**
 * Animation cache key
 * Used to uniquely identify an animation request
 */
data class AnimationKey(
    val model: Any?,
    val cacheStrategy: AnimationCacheStrategy
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimationKey) return false

        return model == other.model &&
                cacheStrategy == other.cacheStrategy
    }

    override fun hashCode(): Int {
        var result = model?.hashCode() ?: 0
        result = 31 * result + cacheStrategy.hashCode()
        return result
    }

    override fun toString(): String {
        return "AnimationKey(model=$model, cacheStrategy=$cacheStrategy)"
    }

    /**
     * Generate cache key string
     * Used for memory cache and disk cache file names
     */
    fun toCacheKey(): String {
        return when (model) {
            is String -> {
                if (model.startsWith("http://") || model.startsWith("https://")) {
                    // Network URL: use MD5 hash
                    md5(model)
                } else {
                    // Local path: use path hash
                    model.hashCode().toString(36)
                }
            }

            is File -> model.absolutePath.hashCode().toString(36)
            is Int -> "res_$model"
            is ByteArray -> md5(model)
            is Uri -> md5(model.toString())
            else -> model?.hashCode()?.toString(36) ?: "unknown"
        }
    }

    /**
     * MD5 hash calculation
     */
    private fun md5(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * MD5 hash calculation (byte array)
     */
    private fun md5(bytes: ByteArray): String {
        return MessageDigest.getInstance("MD5")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
