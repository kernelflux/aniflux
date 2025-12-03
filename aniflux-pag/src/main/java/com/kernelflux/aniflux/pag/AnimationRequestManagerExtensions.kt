package com.kernelflux.aniflux.pag

import androidx.annotation.CheckResult
import com.kernelflux.aniflux.AnimationRequestBuilder
import com.kernelflux.aniflux.AnimationRequestManager
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.pag.PAGFile
import com.kernelflux.pag.PAGImageView
import com.kernelflux.pag.PAGView

/**
 * PAG 格式的 AnimationRequestManager 扩展函数
 * 
 * @author: kernelflux
 * @date: 2025/01/XX
 */

/**
 * 指定加载 PAG 动画
 */
@CheckResult
fun AnimationRequestManager.asPAG(): AnimationRequestBuilder<PAGFile> {
    return AnimationRequestBuilder(aniFlux, this, context, PAGFile::class.java)
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
 * 加载到 PAGImageView（类型推断版本）
 * 如果 Builder 类型未知，根据 View 类型推断为 PAGFile
 */
fun AnimationRequestBuilder<*>.into(view: PAGImageView): PAGImageViewTarget {
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

