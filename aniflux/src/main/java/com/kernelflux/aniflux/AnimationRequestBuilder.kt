package com.kernelflux.aniflux

import android.content.Context
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.SingleAnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.target.*
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.lottie.LottieAnimationView
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView
import com.kernelflux.pag.PAGView
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView
import com.kernelflux.vap.AnimView
import java.io.File

/**
 * 动画请求构建器
 * 提供链式API来构建动画加载请求
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
     *
     * 支持的格式：
     * - GIF: 保留当前停止位置的帧（动画结束时会自动停留在当前帧）
     * - Lottie: 保留当前停止位置的帧（动画结束时会自动停留在当前帧）
     * - SVGA: 通过 fillMode 控制（Forward 保留当前停止位置的帧，Clear 清空）
     * - PAG: 保留当前停止位置的帧（动画结束时会自动停留在当前帧）
     * - VAP: 通过 retainLastFrame 配置控制（true 保留最后一帧，false 清空）
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
     * 
     * 示例：
     * ```
     * .placeholderReplacements {
     *     add("user_1", "https://example.com/user1.jpg")
     *     add("user_2", File("/sdcard/user2.jpg"))
     *     add("logo", R.drawable.logo)
     * }
     * ```
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
    internal fun getResourceClass(): Class<*> {
        return transcodeClass
    }


}

// ========== 扩展函数：提供更简洁的 API ==========

/**
 * 加载 File 到 VAP AnimView
 */
@JvmName("intoPAGImageView")
fun AnimationRequestBuilder<File>.into(view: AnimView): VAPViewTarget {
    val target = VAPViewTarget(view)
    into(target as AnimationTarget<File>)
    return target
}


/**
 * 加载 PAGFile 到 PAGImageView
 */
@JvmName("intoPAGImageView")
fun AnimationRequestBuilder<PAGFile>.into(view: PAGImageView): PAGImageViewTarget {
    val target = PAGImageViewTarget(view)
    into(target as AnimationTarget<PAGFile>)
    return target
}

/**
 * 加载 PAGFile 到 PAGView
 */
@JvmName("intoPAGView")
fun AnimationRequestBuilder<PAGFile>.into(view: PAGView): PAGViewTarget {
    val target = PAGViewTarget(view)
    into(target as AnimationTarget<PAGFile>)
    return target
}

/**
 * 加载 LottieDrawable 到 LottieAnimationView
 */
@JvmName("intoLottieView")
fun AnimationRequestBuilder<LottieDrawable>.into(view: LottieAnimationView): LottieViewTarget {
    val target = LottieViewTarget(view)
    into(target as AnimationTarget<LottieDrawable>)
    return target
}

/**
 * 加载 SVGADrawable 到 SVGAImageView
 */
@JvmName("intoSVGAView")
fun AnimationRequestBuilder<SVGADrawable>.into(view: SVGAImageView): SVGAViewTarget {
    val target = SVGAViewTarget(view)
    into(target as AnimationTarget<SVGADrawable>)
    return target
}

/**
 * 加载 GifDrawable 到 GifImageView
 */
@JvmName("intoGifView")
fun AnimationRequestBuilder<GifDrawable>.into(view: GifImageView): GifViewTarget {
    val target = GifViewTarget(view)
    into(target as AnimationTarget<GifDrawable>)
    return target
}

/**
 * 加载到通用的 AutoAnimationFrameLayout
 * 自动根据动画类型创建并显示对应的动画 View
 */
fun AnimationRequestBuilder<*>.into(container: android.widget.FrameLayout): AutoAnimationFrameLayoutTarget {
    val target = AutoAnimationFrameLayoutTarget(container)
    @Suppress("UNCHECKED_CAST")
    (this as AnimationRequestBuilder<Any>).into(target)
    return target
}

// ========== AnimationRequestBuilder<*> 的类型推断扩展 ==========
// 当 load() 无法检测类型时，根据 View 类型推断资源类型

/**
 * 加载到 PAGImageView（类型推断版本）
 * 如果 Builder 类型未知，根据 View 类型推断为 PAGFile
 */
fun AnimationRequestBuilder<*>.into(view: PAGImageView): PAGImageViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        PAGFile::class.java -> {
            // 类型匹配，直接使用
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<PAGFile>).into(view)
        }

        Any::class.java -> {
            // 类型未知，根据 View 类型推断为 PAG
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asPAG()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "类型不匹配：Builder 类型为 ${builderClass.simpleName}，但目标 View 是 PAGImageView（需要 PAGFile）\n" +
                        "请使用 asPAG().load(...) 显式指定类型"
            )
        }
    }
}

/**
 * 加载到 PAGView（类型推断版本）
 */
fun AnimationRequestBuilder<*>.into(view: PAGView): PAGViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        PAGFile::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<PAGFile>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asPAG()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "类型不匹配：Builder 类型为 ${builderClass.simpleName}，但目标 View 是 PAGView（需要 PAGFile）\n" +
                        "请使用 asPAG().load(...) 显式指定类型"
            )
        }
    }
}

/**
 * 加载到 LottieAnimationView（类型推断版本）
 */
fun AnimationRequestBuilder<*>.into(view: LottieAnimationView): LottieViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        LottieDrawable::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<LottieDrawable>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asLottie()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "类型不匹配：Builder 类型为 ${builderClass.simpleName}，但目标 View 是 LottieAnimationView（需要 LottieDrawable）\n" +
                        "请使用 asLottie().load(...) 显式指定类型"
            )
        }
    }
}

/**
 * 加载到 SVGAImageView（类型推断版本）
 */
fun AnimationRequestBuilder<*>.into(view: SVGAImageView): SVGAViewTarget {
    val builderClass = getBuilderTranscodeClass(this)

    return when (builderClass) {
        SVGADrawable::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<SVGADrawable>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asSVGA()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "类型不匹配：Builder 类型为 ${builderClass.simpleName}，但目标 View 是 SVGAImageView（需要 SVGADrawable）\n" +
                        "请使用 asSVGA().load(...) 显式指定类型"
            )
        }
    }
}

/**
 * 加载到 GifImageView（类型推断版本）
 */
fun AnimationRequestBuilder<*>.into(view: GifImageView): GifViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        GifDrawable::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<GifDrawable>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asGif()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "类型不匹配：Builder 类型为 ${builderClass.simpleName}，但目标 View 是 GifImageView（需要 GifDrawable）\n" +
                        "请使用 asGif().load(...) 显式指定类型"
            )
        }
    }
}

/**
 * 加载到 AnimView（类型推断版本）
 */
fun AnimationRequestBuilder<*>.into(view: AnimView): VAPViewTarget {
    return when (val builderClass = getBuilderTranscodeClass(this)) {
        File::class.java -> {
            @Suppress("UNCHECKED_CAST")
            (this as AnimationRequestBuilder<File>).into(view)
        }

        Any::class.java -> {
            val requestManager = getRequestManagerFromBuilder(this)
            val newBuilder = requestManager.asGif()
            copyBuilderOptions(newBuilder, this)
            newBuilder.into(view)
        }

        else -> {
            throw IllegalArgumentException(
                "类型不匹配：Builder 类型为 ${builderClass.simpleName}，但目标 View 是 AnimView（需要 File）\n" +
                        "请使用 asFile().load(...) 显式指定类型"
            )
        }
    }
}


/**
 * 获取 Builder 的 transcodeClass
 */
private fun getBuilderTranscodeClass(builder: AnimationRequestBuilder<*>): Class<*> {
    return builder.getResourceClass()
}

/**
 * 从 Builder 获取 AnimationRequestManager（通过反射）
 */
private fun getRequestManagerFromBuilder(builder: AnimationRequestBuilder<*>): AnimationRequestManager {
    return try {
        val field = AnimationRequestBuilder::class.java.getDeclaredField("requestManager")
        field.isAccessible = true
        field.get(builder) as AnimationRequestManager
    } catch (e: Exception) {
        throw IllegalStateException("无法获取 AnimationRequestManager", e)
    }
}

/**
 * 复制 Builder 的配置选项（options, listeners 等）
 */
private fun copyBuilderOptions(
    target: AnimationRequestBuilder<*>,
    source: AnimationRequestBuilder<*>
) {
    try {
        // 复制 options
        val optionsField = AnimationRequestBuilder::class.java.getDeclaredField("options")
        optionsField.isAccessible = true
        val sourceOptions = optionsField.get(source) as AnimationOptions
        optionsField.set(target, sourceOptions)

        // 复制 playListener
        val playListenerField = AnimationRequestBuilder::class.java.getDeclaredField("playListener")
        playListenerField.isAccessible = true
        val playListener = playListenerField.get(source)
        playListenerField.set(target, playListener)

        // 复制 requestListener
        val requestListenerField =
            AnimationRequestBuilder::class.java.getDeclaredField("requestListener")
        requestListenerField.isAccessible = true
        val requestListener = requestListenerField.get(source)
        requestListenerField.set(target, requestListener)

        // 复制 model
        val modelField = AnimationRequestBuilder::class.java.getDeclaredField("model")
        modelField.isAccessible = true
        val model = modelField.get(source)
        modelField.set(target, model)

        // 复制 isModelSet
        val isModelSetField = AnimationRequestBuilder::class.java.getDeclaredField("isModelSet")
        isModelSetField.isAccessible = true
        val isModelSet = isModelSetField.get(source)
        isModelSetField.set(target, isModelSet)
    } catch (e: Exception) {
        android.util.Log.w("AnimationRequestBuilder", "Failed to copy builder options", e)
        // 继续执行，即使复制失败
    }
}