package com.kernelflux.aniflux.request

import android.content.Context
import android.util.Log
import com.bumptech.glide.util.Util
import com.kernelflux.aniflux.engine.AnimationEngine
import com.kernelflux.aniflux.engine.AnimationResource
import com.kernelflux.aniflux.engine.AnimationResourceCallback
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.request.target.AnimationSizeReadyCallback
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.util.AnimationExecutors
import com.kernelflux.aniflux.util.AnimationLogTime
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.util.AnimationStateVerifier
import com.kernelflux.aniflux.util.CacheStrategy
import java.util.concurrent.Executor

/**
 * 动画请求的具体实现
 */
class SingleAnimationRequest<T>(
    private val context: Context,
    private val requestLock: Any,
    private val model: Any?,
    private val target: CustomAnimationTarget<T>,
    private val targetListener: AnimationRequestListener<T>?,
    private val transcodeClass: Class<T>,
    private val overrideWidth: Int,
    private val overrideHeight: Int,
    private val engine: AnimationEngine,
    private val callbackExecutor: Executor = AnimationExecutors.MAIN_THREAD_EXECUTOR
) : AnimationRequest, AnimationSizeReadyCallback, AnimationResourceCallback {

    companion object {
        private const val TAG = "AnimationRequest"
        private const val GLIDE_TAG = "AniFlux"
        private val IS_VERBOSE_LOGGABLE = Log.isLoggable(TAG, Log.VERBOSE)
    }

    // 状态验证器
    private val stateVerifier = AnimationStateVerifier.newInstance()

    // 保存LoadStatus用于取消操作
    private var loadStatus: AnimationEngine.LoadStatus? = null

    // 保存已加载的资源，用于请求完成时重用
    private var resource: AnimationResource<T>? = null

    @Volatile
    private var status = Status.PENDING

    private var width = 0
    private var height = 0

    // 请求开始时间，用于性能监控
    private var startTime = 0L

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
            stateVerifier.throwIfRecycled()

            // 记录开始时间
            startTime = AnimationLogTime.getLogTime()

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

            // 检查是否正在运行 - 参考Glide抛出异常的方式
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
        stateVerifier.throwIfRecycled()
        synchronized(requestLock) {
            if (IS_VERBOSE_LOGGABLE) {
                logV("Got onSizeReady in ${AnimationLogTime.getElapsedMillis(startTime)}ms")
            }

            // 检查状态，如果已清除或失败，直接返回
            if (status != Status.WAITING_FOR_SIZE) {
                return
            }

            // 设置状态为运行中
            status = Status.RUNNING
            this.width = width
            this.height = height

            if (IS_VERBOSE_LOGGABLE) {
                logV(
                    "finished setup for calling load in ${
                        AnimationLogTime.getElapsedMillis(
                            startTime
                        )
                    }ms"
                )
            }

            // 通过Engine加载
            loadStatus = engine.load(
                context = context,
                model = model,
                target = target,
                options = createOptions(width, height),
                listener = targetListener,
                callback = this
            )


            if (status != Status.RUNNING) {
                loadStatus = null
            }
            if (IS_VERBOSE_LOGGABLE) {
                logV("finished onSizeReady in ${AnimationLogTime.getElapsedMillis(startTime)}ms")
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

    private fun createOptions(width: Int, height: Int): AnimationOptions {
        return AnimationOptions.create()
            .size(width, height)
            .cacheStrategy(CacheStrategy.ALL)
            .isAnimation(true)
    }

    override fun onResourceReady(
        resource: AnimationResource<*>?,
        dataSource: AnimationDataSource,
        isLoadedFromAlternateCacheKey: Boolean
    ) {
        stateVerifier.throwIfRecycled()
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

            if (Log.isLoggable(GLIDE_TAG, Log.DEBUG)) {
                Log.d(
                    GLIDE_TAG,
                    "Finished loading ${received.javaClass.simpleName} from $dataSource for $model " +
                            "with size [${width}x${height}] in ${
                                AnimationLogTime.getElapsedMillis(
                                    startTime
                                )
                            }ms"
                )
            }
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
                val listenerHandled = targetListener?.onResourceReady(
                    result,
                    model,
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
            } finally {
                isCallingCallbacks = false
            }
        }
    }

    override fun onLoadFailed(exception: Throwable) {
        onLoadFailed(exception, Log.WARN)
    }

    private fun onLoadFailed(exception: Throwable, maxLogLevel: Int) {
        stateVerifier.throwIfRecycled()
        synchronized(requestLock) {
            if (status == Status.CLEARED) {
                return
            }
            loadStatus = null
            status = Status.FAILED

            if (Log.isLoggable(GLIDE_TAG, maxLogLevel)) {
                Log.w(
                    GLIDE_TAG,
                    "Load failed for [$model] with dimensions [${width}x${height}]",
                    exception
                )
            }

            // 使用回调执行器处理回调
            callbackExecutor.execute {
                onLoadFailedInternal(exception)
            }
        }
    }

    /**
     * 内部加载失败回调 - 参考Glide的设计
     */
    private fun onLoadFailedInternal(exception: Throwable) {
        synchronized(requestLock) {
            if (status == Status.CLEARED) {
                return
            }

            isCallingCallbacks = true
            try {
                // 通知listener
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
