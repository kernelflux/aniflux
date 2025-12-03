package com.kernelflux.aniflux.gif

import androidx.annotation.CheckResult
import com.kernelflux.aniflux.AnimationRequestBuilder
import com.kernelflux.aniflux.AnimationRequestManager
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.gif.GifDrawable
import com.kernelflux.gif.GifImageView

/**
 * GIF 格式的 AnimationRequestManager 扩展函数
 * 
 * @author: kernelflux
 * @date: 2025/01/XX
 */

/**
 * 指定加载 GIF 动画
 */
@CheckResult
fun AnimationRequestManager.asGif(): AnimationRequestBuilder<GifDrawable> {
    return AnimationRequestBuilder(aniFlux, this, context, GifDrawable::class.java)
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
 * 加载到 GifImageView（类型推断版本）
 * 如果 Builder 类型未知，根据 View 类型推断为 GifDrawable
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
        val sourceOptions = optionsField.get(source) as com.kernelflux.aniflux.util.AnimationOptions
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
