package com.kernelflux.aniflux.request

import android.content.Context
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
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
import com.kernelflux.aniflux.util.Util

/**
 * Concrete implementation of animation request
 */
class SingleAnimationRequest<T>(
    private val context: Context,
    private val requestLock: Any,
    private val model: Any?,
    private val target: AnimationTarget<T>,
    private val requestListener: AnimationRequestListener<T>?,
    private val transcodeClass: Class<T>,
    private val overrideWidth: Int,
    private val overrideHeight: Int,
    private val engine: AnimationEngine,
    private val options: AnimationOptions,
    private val callbackExecutor: Executor = AnimationExecutors.MAIN_THREAD_EXECUTOR
) : AnimationRequest, AnimationSizeReadyCallback, AnimationResourceCallback {

    companion object {
        private const val TAG = "AnimationRequest"
    }

    // Save LoadStatus for cancel operation
    private var loadStatus: AnimationEngine.LoadStatus? = null

    // Save loaded resource for reuse when request completes
    private var resource: AnimationResource<T>? = null

    @Volatile
    private var status = Status.PENDING

    private var width = 0
    private var height = 0

    // Whether callbacks are being called, prevent duplicate calls
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
            // State validation
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

            // Check if running
            if (status == Status.RUNNING) {
                throw IllegalArgumentException("Cannot restart a running request")
            }

            // If completed, reuse result
            if (status == Status.COMPLETE && resource != null) {
                // ✅ Need to acquire when reusing resource (Target re-holds resource)
                resource?.acquire()
                callbackExecutor.execute {
                    onResourceReady(resource, AnimationDataSource.MEMORY_CACHE, false)
                }
                return
            }

            // If target has View and is invisible, delay loading
            val view = when (target) {
                is CustomViewAnimationTarget<*, *> -> (target as CustomViewAnimationTarget<*, *>).getViewForVisibilityCheck()
                else -> null
            }

            if (view != null && (view.isGone || view.isInvisible)) {
                // View is invisible, delay loading (wait for View to become visible)
                // Wait via getSize, getSize will check visibility
                status = Status.WAITING_FOR_SIZE
                target.getSize(this)
                return
            }

            // Set status to waiting for size
            status = Status.WAITING_FOR_SIZE

            // If valid dimensions provided, use directly
            if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
                onSizeReady(overrideWidth, overrideHeight)
            } else {
                // Otherwise get target's size
                target.getSize(this)
            }
        }
    }

    /**
     * Check if callbacks are being called
     */
    private fun assertNotCallingCallbacks() {
        if (isCallingCallbacks) {
            throw IllegalStateException("Cannot call begin() while callbacks are being executed")
        }
    }


    override fun onSizeReady(width: Int, height: Int) {
        synchronized(requestLock) {
            // Check status, if cleared or failed, return directly
            if (status != Status.WAITING_FOR_SIZE) {
                return
            }

            // Set status to running
            status = Status.RUNNING
            this.width = width
            this.height = height


            // Load via Engine
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
        var toRelease: AnimationResource<T>? = null
        synchronized(requestLock) {
            assertNotCallingCallbacks()
            
            if (status == Status.CLEARED) {
                return
            }
            
            // Cancel loading task in Engine
            loadStatus?.cancel()
            loadStatus = null
            
            // Resource must be released before calling target.onLoadCleared()
            if (resource != null) {
                toRelease = resource
                resource = null
            }
            
            // ✅ Call target.onLoadCleared() when clearing request (following Glide's pattern)
            // This ensures resources are released when Fragment/Activity is destroyed
            // Note: We don't have requestCoordinator, so we always call onLoadCleared()
            // Similar to Glide's canNotifyCleared() which returns true when requestCoordinator is null
            callbackExecutor.execute {
                try {
                    target.onLoadCleared(null)
                } catch (e: Exception) {
                    // Log error but don't throw
                    AniFluxLog.e(AniFluxLogCategory.REQUEST, "Error calling target.onLoadCleared()", e)
                }
            }
            
            status = Status.CLEARED
        }
        
        // Release resource outside of synchronized block
        toRelease?.release()
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
            val typedResource = resource as AnimationResource<T>
            this.resource = typedResource
            // ✅ Acquire when setting resource to Target (Target holds resource)
            typedResource.acquire()
            callbackExecutor.execute {
                @Suppress("UNCHECKED_CAST")
                onResourceReadyInternal(received as T, dataSource)
            }
        }
    }

    /**
     * Internal resource ready callback
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

                // If listener didn't handle, call target callback
                if (!listenerHandled) {
                    // Set options to target before calling onResourceReady
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
                // If exception occurs during callback, convert to failure handling
                onLoadFailed(e)
            } finally {
                isCallingCallbacks = false
            }
        }
    }

    override fun onLoadFailed(exception: Throwable) {
        onLoadFailed(exception, AniFluxLogLevel.WARN.priority)
    }

    private fun onLoadFailed(exception: Throwable, maxLogLevel: Int) {
        synchronized(requestLock) {
            if (status == Status.CLEARED) {
                return
            }
            loadStatus = null
            status = Status.FAILED
            // Use callback executor to handle callbacks
            callbackExecutor.execute {
                onLoadFailedInternal(exception)
            }
        }
    }

    /**
     * Internal load failure callback
     */
    private fun onLoadFailedInternal(exception: Throwable) {
        synchronized(requestLock) {
            if (status == Status.CLEARED) {
                return
            }

            isCallingCallbacks = true
            try {
                // Notify listener
                val listenerHandled = requestListener?.onLoadFailed(
                    exception,
                    model,
                    target,
                    false
                ) ?: false

                // If listener didn't handle, call target callback
                if (!listenerHandled) {
                    target.onLoadFailed(null)
                }
            } catch (e: Exception) {
                // If exception also occurs during error callback, log but don't throw
                AniFluxLog.e(AniFluxLogCategory.REQUEST, "Error in error handling", e)
            } finally {
                isCallingCallbacks = false
            }
        }
    }

    override fun getLock(): Any {
        return requestLock
    }

    /**
     * Log verbose message
     */
    private fun logV(message: String) {
        if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.VERBOSE)) {
            AniFluxLog.v(AniFluxLogCategory.REQUEST, message)
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
