package com.kernelflux.aniflux

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import java.io.File
/**
 * @author: kerneflux
 * @date: 2025/9/23
 *
 */
interface ModelTypes<T> {
    @SuppressLint("CheckResult")
    fun load(bitmap: Bitmap?): T

    @SuppressLint("CheckResult")
    fun load(drawable: Drawable?): T

    @SuppressLint("CheckResult")
    fun load(string: String?): T

    @SuppressLint("CheckResult")
    fun load(uri: Uri?): T

    @SuppressLint("CheckResult")
    fun load(file: File): T

    @SuppressLint("CheckResult")
    fun load(@RawRes @DrawableRes resourceId: Int?): T

    @SuppressLint("CheckResult")
    fun load(model: ByteArray?): T

    @SuppressLint("CheckResult")
    @Suppress("UNCHECKED_CAST")
    fun load(model: Any?): T

}