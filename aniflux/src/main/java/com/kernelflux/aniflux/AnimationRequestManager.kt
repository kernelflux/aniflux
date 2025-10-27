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
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.aniflux.util.Util
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 动画请求管理器
 * 负责管理动画请求的生命周期，避免内存泄漏
 */
class AnimationRequestManager(
    private val aniFlux: AniFlux,
    private val lifecycle: AnimationLifecycle,
    private val treeNode: AnimationRequestManagerTreeNode,
    private val context: Context,
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
        com.bumptech.glide.util.Util.removeCallbacksOnUiThread(addSelfToLifecycle)
        aniFlux.unregisterRequestManager(this)
    }


    //////////////////////////////////////// 基础业务API START //////////////////////////////////////////////////

    @CheckResult
    fun <ResourceType> `as`(
        resourceClass: Class<ResourceType>
    ): AnimationRequestBuilder<ResourceType> {
        return AnimationRequestBuilder(aniFlux, this, context, resourceClass)
    }

    @CheckResult
    fun load(path: String): AnimationRequestBuilder<*> {
        if (path.isEmpty()) {
            throw IllegalArgumentException("Path cannot be empty")
        }
        val animationType = AnimationTypeDetector.detectFromPath(path)
        val resourceClass = AnimationTypeDetector.getClassForAnimationType(animationType)
        return `as`(resourceClass).load(path)
    }


    @CheckResult
    fun load(@androidx.annotation.DrawableRes @androidx.annotation.RawRes resourceId: Int): AnimationRequestBuilder<*> {
        val animationType = AnimationTypeDetector.detectFromResourceId(
            context,
            resourceId
        )
        val resourceClass = AnimationTypeDetector.getClassForAnimationType(animationType)
        return `as`(resourceClass).load(resourceId)
    }

    @CheckResult
    fun load(file: java.io.File): AnimationRequestBuilder<*> {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }
        val animationType =
            AnimationTypeDetector.detectFromPath(file.absolutePath)
        val resourceClass =
            AnimationTypeDetector.getClassForAnimationType(animationType)
        return `as`(resourceClass).load(file)
    }


    @CheckResult
    fun load(uri: android.net.Uri): AnimationRequestBuilder<*> {
        val animationType =
            AnimationTypeDetector.detectFromPath(uri.toString())
        val resourceClass =
            AnimationTypeDetector.getClassForAnimationType(animationType)
        return `as`(resourceClass).load(uri)
    }

    @CheckResult
    fun load(byteArray: ByteArray): AnimationRequestBuilder<*> {
        if (byteArray.isEmpty()) {
            throw IllegalArgumentException("ByteArray cannot be empty")
        }
        val animationType = AnimationTypeDetector.detectFromBytes(
            byteArray,
            byteArray.size
        )
        val resourceClass =
            AnimationTypeDetector.getClassForAnimationType(animationType)
        return `as`(resourceClass).load(byteArray)
    }

    //////////////////////////////////////// 基础业务API END  //////////////////////////////////////////////////


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


    fun clear(view: View) {
        clear(ClearTarget(view))
    }

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
        val request = target.getRequest()
        // If the Target doesn't have a request, it's already been cleared.
        if (request == null) {
            return true
        }
        if (requestTracker.clearAndRemove(request)) {
            targetTracker.untrack(target)
            target.setRequest(null)
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

    private class ClearTarget(view: View) : CustomViewAnimationTarget<View, Any>(view) {
        override fun onResourceCleared(placeholder: Drawable?) {
            // Do nothing, we don't retain a reference to our resource.
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            // Do nothing.
        }

        override fun onResourceReady(resource: Any) {
            // Do nothing.
        }
    }


}
