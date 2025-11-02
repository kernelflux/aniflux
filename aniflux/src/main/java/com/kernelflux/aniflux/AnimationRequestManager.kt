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
import com.kernelflux.svgaplayer.SVGADrawable
import java.io.File
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
        Util.removeCallbacksOnUiThread(addSelfToLifecycle)
        aniFlux.unregisterRequestManager(this)
    }


    //////////////////////////////////////// 基础业务API START //////////////////////////////////////////////////

    @CheckResult
    fun <ResourceType> `as`(
        resourceClass: Class<ResourceType>
    ): AnimationRequestBuilder<ResourceType> {
        return AnimationRequestBuilder(aniFlux, this, context, resourceClass)
    }


    /**
     * 指定加载 PAG 动画
     */
    @CheckResult
    fun asFile(): AnimationRequestBuilder<File> {
        return `as`(File::class.java)
    }


    /**
     * 指定加载 PAG 动画
     */
    @CheckResult
    fun asPAG(): AnimationRequestBuilder<org.libpag.PAGFile> {
        return `as`(org.libpag.PAGFile::class.java)
    }

    /**
     * 指定加载 Lottie 动画
     */
    @CheckResult
    fun asLottie(): AnimationRequestBuilder<com.airbnb.lottie.LottieDrawable> {
        return `as`(com.airbnb.lottie.LottieDrawable::class.java)
    }

    /**
     * 指定加载 SVGA 动画
     */
    @CheckResult
    fun asSVGA(): AnimationRequestBuilder<SVGADrawable> {
        return `as`(SVGADrawable::class.java)
    }

    /**
     * 指定加载 GIF 动画
     */
    @CheckResult
    fun asGif(): AnimationRequestBuilder<GifDrawable> {
        return `as`(GifDrawable::class.java)
    }

    /**
     * 根据动画类型创建对应的 Builder 并加载模型
     * 统一的处理逻辑，消除重复代码
     *
     * @param animationType 检测到的动画类型
     * @param loadAction 加载动作（根据不同的 model 类型调用不同的 load 方法）
     * @param errorMessage 检测失败时的错误消息
     * @param allowUnknown 是否允许返回 UNKNOWN 类型的 Builder（用于 URL/URI 的降级处理）
     */
    private fun createBuilderForType(
        animationType: AnimationTypeDetector.AnimationType,
        loadAction: (AnimationRequestBuilder<*>) -> AnimationRequestBuilder<*>,
        errorMessage: String? = null,
        allowUnknown: Boolean = false
    ): AnimationRequestBuilder<*> {
        if (animationType == AnimationTypeDetector.AnimationType.UNKNOWN) {
            if (allowUnknown) {
                // 允许返回 UNKNOWN 类型的 Builder，在 into() 时根据 View 推断
                return loadAction(AnimationRequestBuilder(aniFlux, this, context, Any::class.java))
            } else {
                throw IllegalArgumentException(
                    errorMessage
                        ?: "无法自动检测动画类型\n请使用 asPAG()/asGif()/asLottie()/asSVGA() 显式指定类型"
                )
            }
        }

        return when (animationType) {
            AnimationTypeDetector.AnimationType.GIF -> loadAction(asGif())
            AnimationTypeDetector.AnimationType.LOTTIE -> loadAction(asLottie())
            AnimationTypeDetector.AnimationType.PAG -> loadAction(asPAG())
            AnimationTypeDetector.AnimationType.SVGA -> loadAction(asSVGA())
            AnimationTypeDetector.AnimationType.VAP -> loadAction(asFile())
            AnimationTypeDetector.AnimationType.UNKNOWN -> throw IllegalArgumentException(
                errorMessage ?: "无法自动检测动画类型"
            )
        }
    }

    /**
     * 从URL字符串加载（自动检测动画类型）
     * 如果检测失败，请使用 asPAG()/asGif()/asLottie()/asSVGA() 显式指定类型
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
            errorMessage = "无法自动检测动画类型，URL: $path\n请使用 asPAG()/asGif()/asLottie()/asSVGA() 显式指定类型",
            allowUnknown = true
        )
    }

    /**
     * 从资源ID加载（自动检测动画类型）
     */
    @CheckResult
    fun load(@androidx.annotation.DrawableRes @androidx.annotation.RawRes resourceId: Int): AnimationRequestBuilder<*> {
        val animationType = AnimationTypeDetector.detectFromResourceId(context, resourceId)
        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(resourceId) },
            errorMessage = "无法自动检测动画类型，ResourceId: $resourceId\n请使用 asPAG()/asGif()/asLottie()/asSVGA() 显式指定类型",
            allowUnknown = false
        )
    }

    /**
     * 从文件加载（自动检测动画类型）
     */
    @CheckResult
    fun load(file: java.io.File): AnimationRequestBuilder<*> {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }
        var animationType = AnimationTypeDetector.detectFromPath(file.absolutePath)

        // 如果从路径检测失败，尝试从文件头检测
        if (animationType == AnimationTypeDetector.AnimationType.UNKNOWN) {
            try {
                val bytes = ByteArray(1024)
                file.inputStream().use { it.read(bytes) }
                animationType = AnimationTypeDetector.detectFromBytes(bytes, bytes.size)
            } catch (e: Exception) {
                // 读取失败，继续使用 UNKNOWN
            }
        }

        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(file) },
            errorMessage = "无法自动检测动画类型，File: ${file.absolutePath}\n请使用 asPAG()/asGif()/asLottie()/asSVGA() 显式指定类型",
            allowUnknown = false
        )
    }

    /**
     * 从Uri加载（自动检测动画类型）
     */
    @CheckResult
    fun load(uri: android.net.Uri): AnimationRequestBuilder<*> {
        val animationType = AnimationTypeDetector.detectFromPath(uri.toString())
        return createBuilderForType(
            animationType = animationType,
            loadAction = { it.load(uri) },
            errorMessage = "无法自动检测动画类型，Uri: $uri\n请使用 asPAG()/asGif()/asLottie()/asSVGA() 显式指定类型",
            allowUnknown = true
        )
    }

    /**
     * 从字节数组加载（自动检测动画类型）
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
            errorMessage = "无法自动检测动画类型（字节数组头部特征不匹配）\n请使用 asPAG()/asGif()/asLottie()/asSVGA() 显式指定类型",
            allowUnknown = false
        )
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

            // 清除target上的播放监听器（避免监听器泄漏和重复回调）
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
