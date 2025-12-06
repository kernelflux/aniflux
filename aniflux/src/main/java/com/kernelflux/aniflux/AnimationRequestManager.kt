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
import com.kernelflux.gif.GifDrawable
import com.kernelflux.lottie.LottieDrawable
import com.kernelflux.pag.PAGFile
import com.kernelflux.svga.SVGADrawable
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Animation request manager
 * Responsible for managing the lifecycle of animation requests, avoiding memory leaks
 */
class AnimationRequestManager(
    private val aniFlux: AniFlux,
    private val lifecycle: AnimationLifecycle,
    private val treeNode: AnimationRequestManagerTreeNode,
    private val context: Context,
    private val requestTracker: AnimationRequestTracker = AnimationRequestTracker(),
    factory: AnimationConnectivityMonitorFactory = aniFlux.getConnectivityMonitorFactory()
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


    //////////////////////////////////////// Basic Business API START //////////////////////////////////////////////////

    @CheckResult
    fun <ResourceType> `as`(
        resourceClass: Class<ResourceType>
    ): AnimationRequestBuilder<ResourceType> {
        return AnimationRequestBuilder(aniFlux, this, context, resourceClass)
    }


    /**
     * Specify loading File (for VAP animation)
     */
    @CheckResult
    fun asFile(): AnimationRequestBuilder<File> {
        return `as`(File::class.java)
    }

    /**
     * Specify loading VAP animation
     */
    @CheckResult
    fun asVAP(): AnimationRequestBuilder<File> {
        return `as`(File::class.java)
    }

    /**
     * Specify loading PAG animation
     */
    @CheckResult
    fun asPAG(): AnimationRequestBuilder<PAGFile> {
        return `as`(PAGFile::class.java)
    }

    /**
     * Specify loading Lottie animation
     */
    @CheckResult
    fun asLottie(): AnimationRequestBuilder<LottieDrawable> {
        return `as`(LottieDrawable::class.java)
    }

    /**
     * Specify loading SVGA animation
     */
    @CheckResult
    fun asSVGA(): AnimationRequestBuilder<SVGADrawable> {
        return `as`(SVGADrawable::class.java)
    }

    /**
     * Specify loading GIF animation
     */
    @CheckResult
    fun asGif(): AnimationRequestBuilder<GifDrawable> {
        return `as`(GifDrawable::class.java)
    }

    
    /**
     * Create corresponding Builder based on animation type and load model
     * Unified processing logic, eliminates duplicate code
     *
     * @param animationType Detected animation type
     * @param loadAction Load action (calls different load methods based on different model types)
     * @param errorMessage Error message when detection fails
     * @param allowUnknown Whether to allow returning UNKNOWN type Builder (for URL/URI fallback handling)
     */
    private fun createBuilderForType(
        animationType: AnimationTypeDetector.AnimationType,
        loadAction: (AnimationRequestBuilder<*>) -> AnimationRequestBuilder<*>,
        errorMessage: String? = null,
        allowUnknown: Boolean = false
    ): AnimationRequestBuilder<*> {
        if (animationType == AnimationTypeDetector.AnimationType.UNKNOWN) {
            if (allowUnknown) {
                // Allow returning UNKNOWN type Builder, infer from View in into()
                return loadAction(AnimationRequestBuilder(aniFlux, this, context, Any::class.java))
            } else {
                throw IllegalArgumentException(
                    errorMessage
                        ?: "Cannot automatically detect animation type\nPlease use asPAG()/asGif()/asLottie()/asSVGA()/asVAP() to explicitly specify type"
                )
            }
        }

        return when (animationType) {
            AnimationTypeDetector.AnimationType.GIF -> loadAction(asGif())
            AnimationTypeDetector.AnimationType.LOTTIE -> loadAction(asLottie())
            AnimationTypeDetector.AnimationType.PAG -> loadAction(asPAG())
            AnimationTypeDetector.AnimationType.SVGA -> loadAction(asSVGA())
            AnimationTypeDetector.AnimationType.VAP -> loadAction(asVAP())
            AnimationTypeDetector.AnimationType.UNKNOWN -> throw IllegalArgumentException(
                errorMessage ?: "Cannot automatically detect animation type"
            )
        }
    }

    /**
     * Load from URL string (auto-detect animation type)
     * If detection fails, please use asPAG()/asGif()/asLottie()/asSVGA()/asVAP() to explicitly specify type
     */
    @CheckResult
    fun load(path: String): AnimationRequestBuilder<*> {
        if (path.isEmpty()) {
            throw IllegalArgumentException("Path cannot be empty")
        }
        val animationType = AnimationTypeDetector.detectFromPath(path)
        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(path) },
            errorMessage = "Cannot automatically detect animation type, URL: $path\nPlease use asPAG()/asGif()/asLottie()/asSVGA()/asVAP() to explicitly specify type",
            allowUnknown = true
        )
    }

    /**
     * Load from resource ID (auto-detect animation type)
     */
    @CheckResult
    fun load(@androidx.annotation.DrawableRes @androidx.annotation.RawRes resourceId: Int): AnimationRequestBuilder<*> {
        val animationType = AnimationTypeDetector.detectFromResourceId(context, resourceId)
        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(resourceId) },
            errorMessage = "Cannot automatically detect animation type, ResourceId: $resourceId\nPlease use asPAG()/asGif()/asLottie()/asSVGA()/asVAP() to explicitly specify type",
            allowUnknown = false
        )
    }

    /**
     * Load from file (auto-detect animation type)
     */
    @CheckResult
    fun load(file: java.io.File): AnimationRequestBuilder<*> {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }
        var animationType = AnimationTypeDetector.detectFromPath(file.absolutePath)

        // If detection from path fails, try detecting from file header
        if (animationType == AnimationTypeDetector.AnimationType.UNKNOWN) {
            try {
                val bytes = ByteArray(1024)
                file.inputStream().use { it.read(bytes) }
                animationType = AnimationTypeDetector.detectFromBytes(bytes, bytes.size)
            } catch (e: Exception) {
                // Read failed, continue using UNKNOWN
            }
        }

        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(file) },
            errorMessage = "Cannot automatically detect animation type, File: ${file.absolutePath}\nPlease use asPAG()/asGif()/asLottie()/asSVGA()/asVAP() to explicitly specify type",
            allowUnknown = false
        )
    }

    /**
     * Load from Uri (auto-detect animation type)
     */
    @CheckResult
    fun load(uri: android.net.Uri): AnimationRequestBuilder<*> {
        val animationType = AnimationTypeDetector.detectFromPath(uri.toString())
        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(uri) },
            errorMessage = "Cannot automatically detect animation type, Uri: $uri\nPlease use asPAG()/asGif()/asLottie()/asSVGA()/asVAP() to explicitly specify type",
            allowUnknown = true
        )
    }

    /**
     * Load from byte array (auto-detect animation type)
     */
    @CheckResult
    fun load(byteArray: ByteArray): AnimationRequestBuilder<*> {
        if (byteArray.isEmpty()) {
            throw IllegalArgumentException("ByteArray cannot be empty")
        }
        val animationType = AnimationTypeDetector.detectFromBytes(byteArray, byteArray.size)
        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(byteArray) },
            errorMessage = "Cannot automatically detect animation type (byte array header signature mismatch)\nPlease use asPAG()/asGif()/asLottie()/asSVGA()/asVAP() to explicitly specify type",
            allowUnknown = false
        )
    }

    //////////////////////////////////////// Basic Business API END  //////////////////////////////////////////////////


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

            // Clear play listener on target (avoid listener leaks and duplicate callbacks)
            when (target) {
                is com.kernelflux.aniflux.request.target.CustomAnimationTarget<*> -> {
                    target.clearPlayListener()
                }

                is com.kernelflux.aniflux.request.target.CustomViewAnimationTarget<*, *> -> {
                    target.clearPlayListener()
                }
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


    /**
     * Get all targets tracked by this manager
     * Used for operations that need to iterate over all targets (e.g., restart animations)
     */
    fun getAllTargets(): List<AnimationTarget<*>> {
        return targetTracker.getAll()
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

        override fun stopAnimation() {
            //
        }

        override fun clearAnimationFromView() {
            //
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            // Do nothing.
        }

        override fun onResourceReady(resource: Any) {
            // Do nothing.
        }
    }


}
