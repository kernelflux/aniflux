package com.kernelflux.aniflux.util

import android.net.Uri
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import java.io.File
import java.security.MessageDigest

/**
 * 动画缓存键
 * 用于唯一标识一个动画请求
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
     * 生成缓存键字符串
     * 用于内存缓存和磁盘缓存的文件名
     */
    fun toCacheKey(): String {
        return when (model) {
            is String -> {
                if (model.startsWith("http://") || model.startsWith("https://")) {
                    // 网络 URL：使用 MD5 哈希
                    md5(model)
                } else {
                    // 本地路径：使用路径哈希
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
     * MD5 哈希计算
     */
    private fun md5(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * MD5 哈希计算（字节数组）
     */
    private fun md5(bytes: ByteArray): String {
        return MessageDigest.getInstance("MD5")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
