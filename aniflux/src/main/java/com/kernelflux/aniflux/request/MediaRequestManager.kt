package com.kernelflux.aniflux.request

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.kernelflux.aniflux.AntiFlux
import com.kernelflux.aniflux.config.LoadOptions
import com.kernelflux.aniflux.config.MediaTarget
import com.kernelflux.aniflux.lifecycle.MediaLifecycle
import com.kernelflux.aniflux.lifecycle.MediaLifecycleListener
import com.kernelflux.aniflux.listener.MediaRequestListener
import com.kernelflux.aniflux.tracker.MediaRequestTracker
import com.kernelflux.aniflux.tracker.MediaTargetTracker
import com.kernelflux.aniflux.utils.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *  媒体请求管理器
 */
class MediaRequestManager : ComponentCallbacks2, MediaLifecycleListener {
    val antiFlux: AntiFlux
    val context: Context
    val mediaLifecycle: MediaLifecycle

    private val requestTracker: MediaRequestTracker
    private val treeNode: MediaRequestManagerTreeNode
    private val targetTracker: MediaTargetTracker = MediaTargetTracker()
    private val requestScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 并发控制
    private val maxConcurrentRequests = 10
    private val requestSemaphore = Semaphore(maxConcurrentRequests)

    // 请求跟踪
    private val mediaRequestTracker = MediaRequestTracker()
    private val mediaTargetTracker = MediaTargetTracker()

    // 默认请求选项
    private var defaultLoadOptions: LoadOptions = LoadOptions()

    // 默认请求监听器
    private val defaultRequestListeners = CopyOnWriteArrayList<MediaRequestListener>()

    // 是否在停止时清除请求
    private var clearOnStop = false

    // 是否在内存压力时暂停所有请求
    private var pauseAllRequestsOnTrimMemoryModerate = false

    private val addSelfToLifecycle: Runnable =
        Runnable { mediaLifecycle.addListener(this@MediaRequestManager) }

    constructor(
        antiFlux: AntiFlux,
        mediaLifecycle: MediaLifecycle,
        treeNode: MediaRequestManagerTreeNode,
        context: Context,
    ) : this(
        antiFlux,
        mediaLifecycle,
        treeNode,
        MediaRequestTracker(),
        context
    )

    constructor(
        antiFlux: AntiFlux,
        mediaLifecycle: MediaLifecycle,
        treeNode: MediaRequestManagerTreeNode,
        requestTracker: MediaRequestTracker,
        context: Context
    ) {
        this.antiFlux = antiFlux
        this.mediaLifecycle = mediaLifecycle
        this.requestTracker = requestTracker
        this.treeNode = treeNode
        this.context = context

        antiFlux.registerRequestManager(this)
        if (Util.isOnBackgroundThread()) {
            Util.postOnUiThread(addSelfToLifecycle)
        } else {
            mediaLifecycle.addListener(this)
        }

    }


    @Synchronized
    fun untrack(target: MediaTarget): Boolean {
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



    override fun onTrimMemory(level: Int) {
        //
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {

    }

    override fun onStart() {
    }

    override fun onStop() {
    }

    override fun onDestroy() {
    }

}