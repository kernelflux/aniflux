package com.kernelflux.aniflux

import android.content.Context
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.SingleAnimationRequest
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.CustomAnimationTarget

/**
 * @author: kerneflux
 * @date: 2025/10/13
 *
 */
class AnimationRequestBuilder<T>(
    private val aniFlux: AniFlux,
    private val requestManager: AnimationRequestManager,
    private val context: Context,
    private val transcodeClass: Class<T>
) {

    private var model: Any? = null
    private var isModelSet = false

    private fun isSkipMemoryCacheWithCompletePreviousRequest(previous: AnimationRequest): Boolean {
        return previous.isComplete()
    }

    /**
     * 设置要加载的模型对象
     */
    private fun loadWithModel(model: Any?): AnimationRequestBuilder<T> {
        this.model = model
        this.isModelSet = true
        return this
    }

    /**
     * 从URL字符串加载
     */
    fun load(url: String?): AnimationRequestBuilder<T> {
        return loadWithModel(url)
    }

    /**
     * 从Uri加载
     */
    fun load(uri: android.net.Uri?): AnimationRequestBuilder<T> {
        return loadWithModel(uri)
    }

    /**
     * 从文件加载
     */
    fun load(file: java.io.File?): AnimationRequestBuilder<T> {
        return loadWithModel(file)
    }

    /**
     * 从资源ID加载
     */
    fun load(@androidx.annotation.DrawableRes @androidx.annotation.RawRes resourceId: Int?): AnimationRequestBuilder<T> {
        return loadWithModel(resourceId)
    }

    /**
     * 从字节数组加载
     */
    fun load(byteArray: ByteArray?): AnimationRequestBuilder<T> {
        return loadWithModel(byteArray)
    }

    fun <Y : CustomAnimationTarget<T>> into(
        target: Y,
        targetListener: AnimationRequestListener<T>?
    ): Y {
        // 检查是否已经设置了model
        if (!isModelSet) {
            throw IllegalArgumentException("You must call #load() before calling #into()")
        }

        // 构建AnimationRequest
        val request = buildRequest(target, targetListener)

        // 检查是否有之前的请求
        val previousRequest = target.getRequest()
        if (request.isEquivalentTo(previousRequest) &&
            previousRequest != null &&
            !isSkipMemoryCacheWithCompletePreviousRequest(previousRequest)
        ) {
            // 如果请求相同且之前的请求没有完成，重用之前的请求
            if (!previousRequest.isRunning()) {
                previousRequest.begin()
            }
            return target
        }

        // 清理之前的请求并设置新请求
        requestManager.clear(target)
        target.setRequest(request)
        requestManager.track(target, request)

        return target
    }

    private fun buildRequest(
        target: CustomAnimationTarget<T>,
        targetListener: AnimationRequestListener<T>?
    ): AnimationRequest {
        return SingleAnimationRequest(
            context = context,
            requestLock = Any(),
            model = model,
            target = target,
            targetListener = targetListener,
            transcodeClass = getTranscodeClass(),
            engine = aniFlux.getEngine(),
        )
    }

    /**
     * 获取转换后的类型Class
     */
    private fun getTranscodeClass(): Class<T> {
        return transcodeClass
    }

}