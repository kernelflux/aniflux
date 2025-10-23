package com.kernelflux.aniflux.request

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kernelflux.aniflux.engine.AnimationEngine
import com.kernelflux.aniflux.engine.AnimationResource
import com.kernelflux.aniflux.engine.AnimationResourceCallback
import com.kernelflux.aniflux.engine.LoadStatus
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.util.CacheStrategy

/**
 * 动画请求的具体实现
 * 参考Glide的SingleRequest设计，结合各动画库的加载方式
 */
class AnimationRequestImpl<T>(
    private val engine: AnimationEngine,
    private val context: Context,
    private val model: Any?,
    private val target: CustomAnimationTarget<T>,
    private val targetListener: AnimationRequestListener<T>?,
    private val transcodeClass: Class<T>
) : AnimationRequest, AnimationResourceCallback {

    private val mainHandler = Handler(Looper.getMainLooper())

    // 保存LoadStatus用于取消操作
    private var loadStatus: LoadStatus? = null

    // 保存已加载的资源，用于请求完成时重用
    private var resource: AnimationResource<T>? = null

    @Volatile
    private var isComplete = false

    @Volatile
    private var isRunning = false

    @Volatile
    private var isCleared = false

    @Volatile
    private var isFailed = false

    override fun isComplete(): Boolean = isComplete
    override fun isCleared(): Boolean = isCleared
    override fun isAnyResourceSet(): Boolean = false

    override fun isRunning(): Boolean = isRunning

    override fun isEquivalentTo(other: AnimationRequest?): Boolean {
        if (other !is AnimationRequestImpl<*>) return false
        return model == other.model && transcodeClass == other.transcodeClass
    }

    override fun begin() {
        if (isCleared || isRunning || isComplete || isFailed) return

        // 检查model是否为null
        if (model == null) {
            onLoadFailed(IllegalArgumentException("Received null model"))
            return
        }

        // 如果已经完成，直接返回结果
        if (isComplete && resource != null) {
            // 重用之前的结果
            onResourceReady(resource!!, null, false)
            return
        }

        // 设置状态为等待尺寸
        isRunning = true
        isFailed = false

        // 获取target的尺寸 - 参考Glide的SingleRequest设计
        target.getSize(object : com.kernelflux.aniflux.request.target.AnimationSizeReadyCallback {
            override fun onSizeReady(width: Int, height: Int) {
                onTargetSizeReady(width, height)
            }
        })
    }

    /**
     * 当target尺寸准备好时调用 - 参考Glide的SingleRequest.onSizeReady设计
     */
    private fun onTargetSizeReady(width: Int, height: Int) {
        if (isCleared || isFailed) return

        // 通过Engine加载 - 参考Glide的SingleRequest设计
        // 传递this作为回调，Engine会回调onResourceReady或onLoadFailed
        loadStatus = engine.load(
            context = context,
            model = model,
            target = target,
            options = createOptions(width, height),
            listener = targetListener,
            callback = this  // 传递this作为回调
        )

        // 根据LoadStatus状态更新本地状态
        when (loadStatus?.status) {
            LoadStatus.Status.COMPLETED_FROM_ACTIVE,
            LoadStatus.Status.COMPLETED_FROM_CACHE -> {
                isComplete = true
                isRunning = false
            }

            LoadStatus.Status.RUNNING -> {
                // 保持运行状态
            }

            LoadStatus.Status.FAILED -> {
                isFailed = true
                isRunning = false
                isComplete = true
            }

            LoadStatus.Status.CANCELLED -> {
                isCleared = true
                isRunning = false
            }

            else -> {
                // 处理null情况
                isFailed = true
                isRunning = false
                isComplete = true
            }
        }
    }

    override fun clear() {
        isCleared = true
        isRunning = false
        isComplete = false
        isFailed = false

        // 取消Engine中的加载任务
        loadStatus?.cancel()
        loadStatus = null

        // 清理资源
        resource = null
    }

    override fun pause() {

    }

    private fun createOptions(width: Int, height: Int): AnimationOptions {
        return AnimationOptions.create()
            .size(width, height)
            .cacheStrategy(CacheStrategy.ALL)
            .isAnimation(true)
    }

    override fun onResourceReady(
        resource: AnimationResource<*>,
        dataSource: Any?,
        isLoadedFromAlternateCacheKey: Boolean
    ) {
        if (isCleared || isFailed) return

        mainHandler.post {
            if (!isCleared && !isFailed) {
                try {
                    isComplete = true
                    isRunning = false
                    isFailed = false

                    // 保存资源用于重用
                    @Suppress("UNCHECKED_CAST")
                    this.resource = resource as AnimationResource<T>

                    // 获取资源内容
                    @Suppress("UNCHECKED_CAST")
                    val result = resource.get()
                    val listenerHandled = targetListener?.onResourceReady(
                        result,
                        model as Any,
                        target,
                        dataSource,
                        false
                    ) ?: false

                    // 如果listener没有处理，则调用target回调
                    if (!listenerHandled) {
                        target.onResourceReady(result)
                    }

                } catch (e: Exception) {
                    // 如果回调过程中出现异常，转换为失败处理
                    onLoadFailed(e)
                }
            }
        }
    }

    override fun onLoadFailed(exception: Throwable) {
        if (isCleared) return

        mainHandler.post {
            if (!isCleared) {
                try {
                    isComplete = true
                    isRunning = false
                    isFailed = true

                    // 通知listener - 参考Glide的RequestListener接口
                    // 如果listener返回true，表示listener已经处理了target更新，不需要再调用target回调
                    val listenerHandled = targetListener?.onLoadFailed(
                        exception,
                        model,
                        target,
                        false
                    ) ?: false

                    // 如果listener没有处理，则调用target回调
                    if (!listenerHandled) {
                        target.onLoadFailed(null)
                    }
                } catch (e: Exception) {
                    // 如果错误回调过程中也出现异常，记录日志但不抛出
                    android.util.Log.e("AnimationRequestImpl", "Error in error handling", e)
                }
            }
        }
    }

    override fun getLock(): Any {
        return this  // 使用this作为锁对象
    }
}
