package com.kernelflux.aniflux.request

import android.content.Context
import android.util.Log
import com.bumptech.glide.util.Util
import com.kernelflux.aniflux.engine.AnimationEngine
import com.kernelflux.aniflux.engine.AnimationResource
import com.kernelflux.aniflux.engine.AnimationResourceCallback
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.target.AnimationSizeReadyCallback
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.aniflux.util.AnimationExecutors
import com.kernelflux.aniflux.util.AnimationOptions
import java.util.concurrent.Executor
import androidx.core.view.isGone
import androidx.core.view.isInvisible

/**
 * 动画请求的具体实现
 */
class SingleAnimationRequest<T>(
    private val context: Context,
    private val requestLock: Any,
    private val model: Any?,
    private val target: AnimationTarget<T>,
    private val requestListener: AnimationRequestListener<T>?,
    private val playListener: AnimationPlayListener?,
    private val transcodeClass: Class<T>,
    private val overrideWidth: Int,
    private val overrideHeight: Int,
    private val engine: AnimationEngine,
    private val options: AnimationOptions,
    private val callbackExecutor: Executor = AnimationExecutors.MAIN_THREAD_EXECUTOR
) : AnimationRequest, AnimationSizeReadyCallback, AnimationResourceCallback {

    companion object {
        private const val TAG = "AnimationRequest"
        private val IS_VERBOSE_LOGGABLE = Log.isLoggable(TAG, Log.VERBOSE)
    }

    // 保存LoadStatus用于取消操作
    private var loadStatus: AnimationEngine.LoadStatus? = null

    // 保存已加载的资源，用于请求完成时重用
    private var resource: AnimationResource<T>? = null

    @Volatile
    private var status = Status.PENDING

    private var width = 0
    private var height = 0

    // 是否正在调用回调，防止重复调用
    private var isCallingCallbacks = false


    override fun isComplete(): Boolean {
        synchronized(requestLock) {
            return status == Status.COMPLETE
        }
    }

    override fun isCleared(): Boolean {
        synchronized(requestLock) {
            return status == Status.CLEARED
        }
    }

    override fun isAnyResourceSet(): Boolean {
        synchronized(requestLock) {
            return status == Status.COMPLETE
        }
    }

    override fun isRunning(): Boolean {
        synchronized(requestLock) {
            return status == Status.RUNNING || status == Status.WAITING_FOR_SIZE
        }
    }

    override fun isEquivalentTo(other: AnimationRequest?): Boolean {
        if (other !is SingleAnimationRequest<*>) return false
        return model == other.model && transcodeClass == other.transcodeClass
    }

    override fun begin() {
        synchronized(requestLock) {
            // 状态验证
            assertNotCallingCallbacks()

            if (model == null) {
                if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
                    width = overrideWidth
                    height = overrideHeight
                }
                status = Status.FAILED
                callbackExecutor.execute {
                    onLoadFailed(IllegalArgumentException("Received null model"))
                }
                return
            }

            // 检查是否正在运行
            if (status == Status.RUNNING) {
                throw IllegalArgumentException("Cannot restart a running request")
            }

            // 如果已完成，重用结果
            if (status == Status.COMPLETE) {
                callbackExecutor.execute {
                    onResourceReady(resource, AnimationDataSource.MEMORY_CACHE, false)
                }
                return
            }

            // 如果target有View且不可见，延迟加载
            val view = when (target) {
                is CustomViewAnimationTarget<*, *> -> (target as CustomViewAnimationTarget<*, *>).getViewForVisibilityCheck()
                else -> null
            }

            if (view != null && (view.isGone || view.isInvisible)) {
                // View不可见，延迟加载（等待View变为可见）
                // 通过getSize来等待，getSize会检查visibility
                status = Status.WAITING_FOR_SIZE
                target.getSize(this)
                return
            }

            // 设置状态为等待尺寸
            status = Status.WAITING_FOR_SIZE

            // 如果提供了有效的尺寸，直接使用
            if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
                onSizeReady(overrideWidth, overrideHeight)
            } else {
                // 否则获取target的尺寸
                target.getSize(this)
            }
        }
    }

    /**
     * 检查是否正在调用回调 - 参考Glide的assertNotCallingCallbacks
     */
    private fun assertNotCallingCallbacks() {
        if (isCallingCallbacks) {
            throw IllegalStateException("Cannot call begin() while callbacks are being executed")
        }
    }


    override fun onSizeReady(width: Int, height: Int) {
        synchronized(requestLock) {
            // 检查状态，如果已清除或失败，直接返回
            if (status != Status.WAITING_FOR_SIZE) {
                return
            }

            // 设置状态为运行中
            status = Status.RUNNING
            this.width = width
            this.height = height


            // 通过Engine加载
            loadStatus = engine.load(
                context = context,
                model = model,
                target = target,
                options = options,
                listener = requestListener,
                cb = this
            )


            if (status != Status.RUNNING) {
                loadStatus = null
            }
        }
    }


    override fun clear() {
        synchronized(requestLock) {
            status = Status.CLEARED

            // 取消Engine中的加载任务
            loadStatus?.cancel()
            loadStatus = null

            // 清理资源
            resource = null
        }
    }

    override fun pause() {
        synchronized(requestLock) {
            if (isRunning()) {
                clear()
            }
        }
    }

    override fun onResourceReady(
        resource: AnimationResource<*>?,
        dataSource: AnimationDataSource,
        isLoadedFromAlternateCacheKey: Boolean
    ) {
        synchronized(requestLock) {
            loadStatus = null

            if (resource == null) {
                val exception = IllegalArgumentException(
                    "Expected to receive a Resource with an object of ${transcodeClass.simpleName} " +
                            "inside, but instead got null."
                )
                onLoadFailed(exception)
                return
            }

            val received = resource.get()
            if (received == null || !transcodeClass.isAssignableFrom(received.javaClass)) {
                val exception = IllegalArgumentException(
                    "Expected to receive an object of ${transcodeClass.simpleName} but instead " +
                            "got ${received?.javaClass?.simpleName ?: "null"}"
                )
                onLoadFailed(exception)
                return
            }

            if (status == Status.CLEARED || status == Status.FAILED) {
                return
            }

            status = Status.COMPLETE
            @Suppress("UNCHECKED_CAST")
            this.resource = resource as AnimationResource<T>
            callbackExecutor.execute {
                @Suppress("UNCHECKED_CAST")
                onResourceReadyInternal(received as T, dataSource)
            }
        }
    }

    /**
     * 内部资源准备回调
     */
    private fun onResourceReadyInternal(
        result: T,
        dataSource: AnimationDataSource
    ) {
        synchronized(requestLock) {
            if (status == Status.CLEARED || status == Status.FAILED) {
                return
            }

            isCallingCallbacks = true
            try {
                val listenerHandled = requestListener?.onResourceReady(
                    result,
                    model,
                    target,
                    dataSource,
                    false
                ) ?: false

                // 如果listener没有处理，则调用target回调
                if (!listenerHandled) {
                    // 在调用 onResourceReady 之前，设置 options 到 target
                    when (target) {
                        is CustomViewAnimationTarget<*, *> -> {
                            target.animationOptions = options
                        }
                        is CustomAnimationTarget<*> -> {
                            target.animationOptions = options
                        }
                    }
                    target.onResourceReady(result)
                }
            } catch (e: Exception) {
                // 如果回调过程中出现异常，转换为失败处理
                onLoadFailed(e)
            } finally {
                isCallingCallbacks = false
            }
        }
    }

    override fun onLoadFailed(exception: Throwable) {
        onLoadFailed(exception, Log.WARN)
    }

    private fun onLoadFailed(exception: Throwable, maxLogLevel: Int) {
        synchronized(requestLock) {
            if (status == Status.CLEARED) {
                return
            }
            loadStatus = null
            status = Status.FAILED
            // 使用回调执行器处理回调
            callbackExecutor.execute {
                onLoadFailedInternal(exception)
            }
        }
    }

    /**
     * 内部加载失败回调
     */
    private fun onLoadFailedInternal(exception: Throwable) {
        synchronized(requestLock) {
            if (status == Status.CLEARED) {
                return
            }

            isCallingCallbacks = true
            try {
                // 通知listener
                val listenerHandled = requestListener?.onLoadFailed(
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
                Log.e(TAG, "Error in error handling", e)
            } finally {
                isCallingCallbacks = false
            }
        }
    }

    override fun getLock(): Any {
        return requestLock
    }

    /**
     * 记录详细日志 - 参考Glide的logV设计
     */
    private fun logV(message: String) {
        if (IS_VERBOSE_LOGGABLE) {
            Log.v(TAG, message)
        }
    }


    private enum class Status {
        PENDING,
        RUNNING,
        WAITING_FOR_SIZE,
        COMPLETE,
        FAILED,
        CLEARED,
    }

}
