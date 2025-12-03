package com.kernelflux.aniflux.svga

import androidx.annotation.CheckResult
import com.kernelflux.aniflux.AnimationRequestBuilder
import com.kernelflux.aniflux.AnimationRequestManager
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.svga.SVGADrawable
import com.kernelflux.svga.SVGAImageView

/**
 * SVGA 格式的 AnimationRequestManager 扩展函数
 * 
 * @author: kernelflux
 * @date: 2025/01/XX
 */

/**
 * 指定加载 SVGA 动画
 */
@CheckResult
fun AnimationRequestManager.asSVGA(): AnimationRequestBuilder<SVGADrawable> {
    return AnimationRequestBuilder(aniFlux, this, context, SVGADrawable::class.java)
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
 * 加载到 SVGAImageView（类型推断版本）
 * 如果 Builder 类型未知，根据 View 类型推断为 SVGADrawable
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

