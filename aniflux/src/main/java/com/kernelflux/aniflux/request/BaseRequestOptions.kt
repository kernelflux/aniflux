package com.kernelflux.aniflux.request

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable

/**
 * @author: kerneflux
 * @date: 2025/9/23
 *
 */
open class BaseRequestOptions<T : BaseRequestOptions<T>> : Cloneable {
    private var errorPlaceholder: Drawable? = null
    private var errId: Int = 0
    private var placeholderId: Int = 0
    private var placeholderDrawable: Drawable? = null
    private var isAutoCloneEnabled: Boolean = false
    private var isLocked: Boolean = false

    //  protected final boolean isAutoCloneEnabled() {
    //    return isAutoCloneEnabled;
    //  }

    fun isAutoCloneEnabled(): Boolean {
        return isAutoCloneEnabled
    }

    fun getErrorPlaceholder(): Drawable? {
        return errorPlaceholder
    }

    fun getErrorId(): Int {
        return errId
    }

    fun getPlaceholderId(): Int {
        return placeholderId
    }

    fun getPlaceholderDrawable(): Drawable? {
        return placeholderDrawable
    }

    @SuppressLint("CheckResult")
    @Suppress("UNCHECKED_CAST")
    override fun clone(): T {
        try {
            val result = super.clone() as BaseRequestOptions<*>
            result.isLocked = false
            result.isAutoCloneEnabled = false
            return result as T
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }
    }


    @SuppressWarnings("unchecked")
    fun selfOrThrowIfLocked(): T {
        if (isLocked) {
            throw IllegalStateException("You cannot modify locked T, consider clone()")
        }
        return self()
    }


    @SuppressWarnings("unchecked")
    @Suppress("UNCHECKED_CAST")
    private fun self(): T {
        return this as T
    }

    companion object {
        private const val UNSET: Int = -1
        private const val SIZE_MULTIPLIER: Int = 1 shl 1
        private const val DISK_CACHE_STRATEGY: Int = 1 shl 2
        private const val PRIORITY: Int = 1 shl 3
        private const val ERROR_PLACEHOLDER: Int = 1 shl 4
        private const val ERROR_ID: Int = 1 shl 5
        private const val PLACEHOLDER: Int = 1 shl 6
        private const val PLACEHOLDER_ID: Int = 1 shl 7
        private const val IS_CACHEABLE: Int = 1 shl 8
        private const val OVERRIDE: Int = 1 shl 9
        private const val SIGNATURE: Int = 1 shl 10
        private const val TRANSFORMATION: Int = 1 shl 11
        private const val RESOURCE_CLASS: Int = 1 shl 12
        private const val FALLBACK: Int = 1 shl 13
        private const val FALLBACK_ID: Int = 1 shl 14
        private const val THEME: Int = 1 shl 15
        private const val TRANSFORMATION_ALLOWED: Int = 1 shl 16
        private const val TRANSFORMATION_REQUIRED: Int = 1 shl 17
        private const val USE_UNLIMITED_SOURCE_GENERATORS_POOL: Int = 1 shl 18
        private const val ONLY_RETRIEVE_FROM_CACHE: Int = 1 shl 19
        private const val USE_ANIMATION_POOL: Int = 1 shl 20
    }

}