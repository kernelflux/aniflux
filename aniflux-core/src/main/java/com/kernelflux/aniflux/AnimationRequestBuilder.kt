package com.kernelflux.aniflux

import android.content.Context
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.SingleAnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.cache.AnimationCacheStrategy

/**
 * 动画请求构建器
 * 提供链式API来构建动画加载请求
 * 
 * 这是核心版本，不包含格式特定的代码
 * 格式特定的扩展函数应该在各自的格式模块中提供
 *
 * @author: kerneflux
 * @date: 2025/10/13
 */
class AnimationRequestBuilder<T>(
    private val aniFlux: AniFlux,
    private val requestManager: AnimationRequestManager,
    private val context: Context,
    private val transcodeClass: Class<T>
) {

    private var model: Any? = null
    private var isModelSet = false
    private var options: AnimationOptions = AnimationOptions.create()
    private var playListener: AnimationPlayListener? = null
    private var requestListener: AnimationRequestListener<T>? = null

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

    // ========== 配置方法 ==========

    /**
     * 设置动画尺寸
     */
    fun size(width: Int, height: Int): AnimationRequestBuilder<T> {
        options.size(width, height)
        return this
    }

    /**
     * 设置缓存策略
     */
    fun cacheStrategy(strategy: AnimationCacheStrategy): AnimationRequestBuilder<T> {
        options.cacheStrategy(strategy)
        return this
    }

    /**
     * 设置动画循环次数
     * @param count -1表示无限循环，0表示不循环，>0表示循环次数
     */
    fun repeatCount(count: Int): AnimationRequestBuilder<T> {
        options.repeatCount(count)
        return this
    }

    /**
     * 设置是否自动播放
     */
    fun autoPlay(auto: Boolean): AnimationRequestBuilder<T> {
        options.autoPlay(auto)
        return this
    }

    /**
     * 设置是否保留动画停止时的帧（动画结束时）
     * @param retain true 表示保留当前停止位置的帧（停在当前帧），false 表示清空显示（默认 true）
     */
    fun retainLastFrame(retain: Boolean): AnimationRequestBuilder<T> {
        options.retainLastFrame(retain)
        return this
    }
    
    /**
     * 设置占位图替换配置（使用DSL）
     * 
     * 支持的格式：SVGA、PAG、Lottie
     * 
     * @param builder 占位图替换配置的构建器
     * @return this，支持链式调用
     */
    fun placeholderReplacements(builder: com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap.() -> Unit): AnimationRequestBuilder<T> {
        options.placeholderReplacements(builder)
        return this
    }
    
    /**
     * 设置占位图替换配置（直接传入）
     * 
     * 支持的格式：SVGA、PAG、Lottie
     * 
     * @param map 占位图替换映射表
     * @return this，支持链式调用
     */
    fun placeholderReplacements(map: com.kernelflux.aniflux.placeholder.PlaceholderReplacementMap): AnimationRequestBuilder<T> {
        options.placeholderReplacements(map)
        return this
    }

    /**
     * 应用自定义配置选项
     */
    fun apply(customOptions: AnimationOptions): AnimationRequestBuilder<T> {
        // 合并配置选项
        options = customOptions
        return this
    }

    fun playListener(listener: AnimationPlayListener?): AnimationRequestBuilder<T> {
        playListener = listener
        return this
    }

    fun requestListener(listener: AnimationRequestListener<T>?): AnimationRequestBuilder<T> {
        requestListener = listener
        return this
    }

    fun <Y : AnimationTarget<T>> into(target: Y): Y {
        return into(target, requestListener, playListener)
    }


    fun <Y : AnimationTarget<T>> into(
        target: Y,
        requestListener: AnimationRequestListener<T>? = null,
        playListener: AnimationPlayListener? = null
    ): Y {
        // 检查是否已经设置了model
        if (!isModelSet) {
            throw IllegalArgumentException("You must call #load() before calling #into()")
        }

        // 构建AnimationRequest
        val request = buildRequest(target, requestListener)

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
        // 关键：针对同一个target的新请求，应该清除之前的监听器，避免重复回调
        requestManager.clear(target)

        // 清除target上之前的播放监听器（避免多次回调）
        // CustomAnimationTarget 和 CustomViewAnimationTarget 都支持播放监听器管理
        when (target) {
            is CustomAnimationTarget<*> -> {
                target.clearPlayListener()
                // 添加新的监听器（如果有）
                playListener?.let { listener ->
                    target.setPlayListener(listener)
                }
            }

            is CustomViewAnimationTarget<*, *> -> {
                target.clearPlayListener()
                // 添加新的监听器（如果有）
                playListener?.let { listener ->
                    target.addPlayListener(listener)
                }
            }
        }

        target.setRequest(request)
        requestManager.track(target, request)

        return target
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildRequest(
        target: AnimationTarget<*>,
        requestListener: AnimationRequestListener<T>? = null
    ): AnimationRequest {
        return SingleAnimationRequest(
            context = context,
            requestLock = Any(),
            model = model,
            target = target as AnimationTarget<T>,
            requestListener = requestListener,
            transcodeClass = getTranscodeClass(),
            overrideWidth = options.width,
            overrideHeight = options.height,
            engine = aniFlux.getEngine(),
            options = options
        )
    }

    /**
     * 获取转换后的类型Class
     */
    private fun getTranscodeClass(): Class<T> {
        return transcodeClass
    }

    /**
     * 获取 transcodeClass（用于类型推断）
     * 暴露给扩展函数使用
     */
    fun getResourceClass(): Class<*> {
        return transcodeClass
    }
}

