package com.kernelflux.aniflux

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.CheckResult
import com.kernelflux.aniflux.manager.AnimationConnectivityMonitor
import com.kernelflux.aniflux.manager.AnimationConnectivityMonitorFactory
import com.kernelflux.aniflux.manager.AnimationLifecycle
import com.kernelflux.aniflux.manager.AnimationLifecycleListener
import com.kernelflux.aniflux.manager.AnimationRequestManagerTreeNode
import com.kernelflux.aniflux.manager.AnimationRequestTracker
import com.kernelflux.aniflux.manager.AnimationTargetTracker
import com.kernelflux.aniflux.engine.AnimationEngine
import com.kernelflux.aniflux.engine.AnimationEngine.LoadStatus
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.util.Util
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 动画请求管理器
 * 负责管理动画请求的生命周期，避免内存泄漏
 * 
 * 参考 Glide 设计，放在 core 模块中，只包含核心方法
 * 格式特定的便捷方法（asGif/asLottie 等）通过扩展函数在 aniflux 模块中提供
 * 
 * @author: kernelflux
 * @date: 2025/10/8
 */
class AnimationRequestManager(
    val aniFlux: AniFlux,
    private val lifecycle: AnimationLifecycle,
    private val treeNode: AnimationRequestManagerTreeNode,
    val context: Context,
    private val requestTracker: AnimationRequestTracker = AnimationRequestTracker(),
    private val factory: AnimationConnectivityMonitorFactory = aniFlux.getConnectivityMonitorFactory()
) : AnimationLifecycleListener, ComponentCallbacks2 {
    private val targetTracker = AnimationTargetTracker()
    private val addSelfToLifecycle = {
        lifecycle.addListener(this@AnimationRequestManager)
    }
    private val connectivityMonitor: AnimationConnectivityMonitor = factory.build(
        context.applicationContext,
        AnimationRequestManagerConnectivityListener(requestTracker)
    )
    private val defaultRequestListeners: CopyOnWriteArrayList<AnimationRequestListener<Any>>
    private var pauseAllRequestsOnTrimMemoryModerate: Boolean = false
    private var clearOnStop = false

    init {
        aniFlux.registerRequestManager(this)
        if (Util.isOnBackgroundThread()) {
            Util.postOnUiThread(addSelfToLifecycle)
        } else {
            lifecycle.addListener(this)
        }
        lifecycle.addListener(connectivityMonitor)
        defaultRequestListeners = CopyOnWriteArrayList(aniFlux.getDefaultRequestListeners())
    }


    @Synchronized
    fun clearOnStop(): AnimationRequestManager {
        clearOnStop = true
        return this
    }

    fun addDefaultRequestListener(requestListener: AnimationRequestListener<Any>): AnimationRequestManager {
        defaultRequestListeners.add(requestListener)
        return this
    }

    fun setPauseAllRequestsOnTrimMemoryModerate(pauseAllOnTrim: Boolean) {
        pauseAllRequestsOnTrimMemoryModerate = pauseAllOnTrim
    }

    @Synchronized
    fun isPaused(): Boolean {
        return requestTracker.isPaused()
    }

    @Synchronized
    fun pauseRequests() {
        requestTracker.pauseRequests()
    }

    @Synchronized
    fun pauseAllRequests() {
        requestTracker.pauseAllRequests()
    }

    @Synchronized
    fun pauseAllRequestsRecursive() {
        pauseAllRequests()
        for (requestManager in treeNode.getDescendants()) {
            requestManager.pauseAllRequests()
        }
    }

    @Synchronized
    fun pauseRequestsRecursive() {
        pauseRequests()
        for (requestManager in treeNode.getDescendants()) {
            requestManager.pauseRequests()
        }
    }

    @Synchronized
    fun resumeRequests() {
        requestTracker.resumeRequests()
    }

    @Synchronized
    fun resumeRequestsRecursive() {
        Util.assertMainThread()
        resumeRequests()
        for (requestManager in treeNode.getDescendants()) {
            requestManager.resumeRequests()
        }
    }

    @Synchronized
    override fun onStart() {
        resumeRequests()
        targetTracker.onStart()
    }

    @Synchronized
    override fun onStop() {
        targetTracker.onStop()
        if (clearOnStop) {
            clearRequests()
        } else {
            pauseRequests()
        }
    }

    @Synchronized
    override fun onDestroy() {
        targetTracker.onDestroy()
        clearRequests()
        requestTracker.clearRequests()
        lifecycle.removeListener(this)
        lifecycle.removeListener(connectivityMonitor)
        Util.removeCallbacksOnUiThread(addSelfToLifecycle)
        aniFlux.unregisterRequestManager(this)
    }


    //////////////////////////////////////// 核心API START //////////////////////////////////////////////////

    /**
     * 指定资源类型（泛型方法，参考 Glide 设计）
     * 格式特定的便捷方法（asGif/asLottie 等）通过扩展函数在 aniflux 模块中提供
     * 
     * 注意：AnimationRequestBuilder 在 aniflux 模块中，这里返回 Any
     * 实际实现通过扩展函数在 aniflux 模块中提供类型安全的 API
     */
    @CheckResult
    fun <ResourceType> `as`(
        resourceClass: Class<ResourceType>
    ): Any {
        // AnimationRequestBuilder 在 aniflux 模块中，需要通过扩展函数来创建
        // 这里抛出异常，提示使用扩展函数
        throw UnsupportedOperationException(
            "Please use extension functions in aniflux module: asGif(), asLottie(), asPAG(), asSVGA(), etc."
        )
    }

    //////////////////////////////////////// 核心API END  //////////////////////////////////////////////////


    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_MODERATE && pauseAllRequestsOnTrimMemoryModerate) {
            pauseAllRequestsRecursive()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        //
    }

    @Synchronized
    private fun clearRequests() {
        for (target in targetTracker.getAll()) {
            clear(target)
        }
        targetTracker.clear()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // 配置变化时不需要特殊处理
    }


    // clear(view: View) 方法移到扩展函数文件中，因为它依赖 ClearTarget

    fun clear(target: AnimationTarget<*>?) {
        if (target == null) {
            return
        }

        untrackOrDelegate(target)
    }


    private fun untrackOrDelegate(target: AnimationTarget<*>) {
        val isOwnedByUs = untrack(target)
        val request = target.getRequest()
        if (!isOwnedByUs && !aniFlux.removeFromManagers(target) && request != null) {
            target.setRequest(null)
            request.clear()
        }
    }

    @Synchronized
    fun untrack(target: AnimationTarget<*>): Boolean {
        val request = target.getRequest() ?: return true
        // If the Target doesn't have a request, it's already been cleared.
        if (requestTracker.clearAndRemove(request)) {
            targetTracker.untrack(target)
            target.setRequest(null)

            // 清除target上的播放监听器（避免监听器泄漏和重复回调）
            // 注意：CustomAnimationTarget 和 CustomViewAnimationTarget 在 aniflux 模块中
            // 这里使用反射或接口来调用 clearPlayListener，或者通过扩展函数处理
            try {
                val clearMethod = target.javaClass.getMethod("clearPlayListener")
                clearMethod.invoke(target)
            } catch (e: Exception) {
                // 方法不存在或调用失败，忽略
            }

            return true
        } else {
            return false
        }
    }

    @Synchronized
    fun track(target: AnimationTarget<*>, request: AnimationRequest) {
        targetTracker.track(target)
        requestTracker.runRequest(request)
    }

    // 获取Engine实例
    private fun getEngine(): AnimationEngine {
        return aniFlux.getEngine()
    }

    // 通过Engine加载动画
    fun <T> load(
        context: Context,
        model: Any?,
        target: AnimationTarget<T>,
        options: AnimationOptions,
        listener: AnimationRequestListener<T>?
    ): LoadStatus? {
        return getEngine().load(context, model, target, options, listener)
    }


    private inner class AnimationRequestManagerConnectivityListener(
        private val requestTracker: AnimationRequestTracker
    ) : AnimationConnectivityMonitor.AnimationConnectivityListener {
        override fun onConnectivityChanged(isConnected: Boolean) {
            if (isConnected) {
                synchronized(this@AnimationRequestManager) {
                    requestTracker.restartRequests()
                }
            }
        }
    }

    // ClearTarget 移到 aniflux 模块的扩展函数文件中，因为它依赖 CustomViewAnimationTarget


}

