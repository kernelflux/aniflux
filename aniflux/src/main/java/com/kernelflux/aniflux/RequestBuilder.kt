package com.kernelflux.aniflux

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import com.kernelflux.aniflux.request.BaseRequestOptions
import com.kernelflux.aniflux.request.MediaRequestManager
import java.io.File

/**
 * @author: kerneflux
 * @date: 2025/9/23
 *
 */
class RequestBuilder<TranscodeType> : BaseRequestOptions<RequestBuilder<TranscodeType>>,
    Cloneable,
    ModelTypes<RequestBuilder<TranscodeType>> {
    private val context: Context
    private val antiFluxContext: AntiFluxContext
    private val antiFlux: AntiFlux
    private val transcodeClass: Class<TranscodeType>
    private val mediaRequestManager: MediaRequestManager

    private var model: Any? = null


    @SuppressLint("CheckResult")
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    constructor(
        antiFlux: AntiFlux,
        mediaRequestManager: MediaRequestManager,
        transcodeClass: Class<TranscodeType>,
        context: Context
    ) {
        this.antiFlux = antiFlux
        this.mediaRequestManager = mediaRequestManager
        this.transcodeClass = transcodeClass
        this.context = context
        this.antiFluxContext = antiFlux.getAntiFluxContext()
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    constructor(
        transcodeClass: Class<TranscodeType>,
        other: RequestBuilder<*>
    ) : this(other.antiFlux, other.mediaRequestManager, transcodeClass, other.context) {
        model = other.model
    }


    override fun load(bitmap: Bitmap?): RequestBuilder<TranscodeType> {
        return loadGeneric(bitmap)
    }

    override fun load(drawable: Drawable?): RequestBuilder<TranscodeType> {
        return loadGeneric(drawable)
    }

    override fun load(string: String?): RequestBuilder<TranscodeType> {
        return loadGeneric(string)
    }

    override fun load(uri: Uri?): RequestBuilder<TranscodeType> {
        return loadGeneric(uri)
    }

    override fun load(file: File): RequestBuilder<TranscodeType> {
        return loadGeneric(file)
    }

    override fun load(resourceId: Int?): RequestBuilder<TranscodeType> {
        return loadGeneric(resourceId)
    }

    override fun load(model: ByteArray?): RequestBuilder<TranscodeType> {
        return loadGeneric(model)
    }

    override fun load(model: Any?): RequestBuilder<TranscodeType> {
        return loadGeneric(model)
    }


    private fun loadGeneric(model: Any?): RequestBuilder<TranscodeType> {
        if (isAutoCloneEnabled()) {
            return clone().loadGeneric(model)
        }
        this.model = model
        return selfOrThrowIfLocked()
    }

    override fun clone(): RequestBuilder<TranscodeType> {
        val result = super<BaseRequestOptions>.clone()
        return result
    }


}