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
 * Animation request manager
 * Responsible for managing animation request lifecycle to avoid memory leaks
 * 
 * Inspired by Glide design, placed in core module, only contains core methods
 * Format-specific convenience methods (asGif/asLottie, etc.) are provided via extension functions in aniflux module
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


    //////////////////////////////////////// Core API START //////////////////////////////////////////////////

    /**
     * Specify resource type (generic method, references Glide design)
     * Format-specific convenience methods (asGif/asLottie, etc.) are provided via extension functions in aniflux module
     * 
     * Note: AnimationRequestBuilder is in aniflux module, here returns Any
     * Actual implementation provides type-safe API via extension functions in aniflux module
     */
    @CheckResult
    fun <ResourceType> `as`(
        resourceClass: Class<ResourceType>
    ): Any {
        // AnimationRequestBuilder is in aniflux module, needs to be created via extension functions
        // Here throws exception to prompt using extension functions
        throw UnsupportedOperationException(
            "Please use extension functions in aniflux module: asGif(), asLottie(), asPAG(), asSVGA(), etc."
        )
    }

    //////////////////////////////////////// Core API END  //////////////////////////////////////////////////


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
        // No special handling needed when configuration changes
    }


    // clear(view: View) method moved to extension function file, as it depends on ClearTarget

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

            // Clear play listener on target (avoid listener leaks and duplicate callbacks)
            // Note: CustomAnimationTarget and CustomViewAnimationTarget are in aniflux module
            // Here use reflection or interface to call clearPlayListener, or handle via extension functions
            try {
                val clearMethod = target.javaClass.getMethod("clearPlayListener")
                clearMethod.invoke(target)
            } catch (e: Exception) {
                // Method doesn't exist or call failed, ignore
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

    // Get Engine instance
    private fun getEngine(): AnimationEngine {
        return aniFlux.getEngine()
    }

    // Load animation via Engine
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

    // ClearTarget moved to aniflux module's extension function file, as it depends on CustomViewAnimationTarget


}

