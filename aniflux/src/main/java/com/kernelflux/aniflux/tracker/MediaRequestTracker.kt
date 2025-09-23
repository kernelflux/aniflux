package com.kernelflux.aniflux.tracker

import com.kernelflux.aniflux.request.MediaRequest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 请求跟踪器
 */
class MediaRequestTracker {
    private val requests = Collections.newSetFromMap(ConcurrentHashMap<MediaRequest, Boolean>())
    private val pendingRequests = CopyOnWriteArraySet<MediaRequest>()
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock = lock.readLock()
    private val writeLock = lock.writeLock()
    @Volatile
    private var isPaused = false

    /**
     * 开始跟踪请求
     */
    fun runRequest(request: MediaRequest) {
        requests.add(request)
        if (!isPaused) {
            request.begin()
        } else {
            request.clear()
            pendingRequests.add(request)
        }
    }

    /**
     * 清除并移除请求
     */
    fun clearAndRemove(request: MediaRequest?): Boolean {
        if (request == null) {
            // 如果请求为null，认为已经清除
            return true
        }

        var isOwnedByUs = requests.remove(request)
        // 避免短路，确保两个操作都执行
        isOwnedByUs = pendingRequests.remove(request) || isOwnedByUs

        if (isOwnedByUs) {
            request.clear()
        }

        return isOwnedByUs
    }

    /**
     * 是否暂停
     */
    fun isPaused(): Boolean {
        return readLock.withLock { isPaused }
    }

    /**
     * 暂停请求
     */
    fun pauseRequests() {
        writeLock.withLock {
            isPaused = true
            for (request in getSnapshot(requests)) {
                if (request.isRunning()) {
                    // 避免清除已完成的部分（如缩略图）以避免UI闪烁
                    // 同时确保进行中的请求部分立即停止
                    request.pause()
                    pendingRequests.add(request)
                }
            }
        }
    }

    /**
     * 暂停所有请求并释放已完成请求的位图
     */
    fun pauseAllRequests() {
        writeLock.withLock {
            isPaused = true
            for (request in getSnapshot(requests)) {
                when {
                    // 正在运行的请求：暂停并添加到待处理列表
                    request.isRunning() -> {
                        request.pause()
                        pendingRequests.add(request)
                    }
                    // 已完成的请求：清除资源并添加到待处理列表
                    request.isComplete() -> {
                        request.clear()
                        pendingRequests.add(request)
                    }
                    // 失败的请求：清除并添加到待处理列表
                    // 失败的请求通常不会从isComplete()返回true，所以我们需要单独处理
                    request.isFailed() -> {
                        request.clear()
                        pendingRequests.add(request)
                    }
                    // 已清除的请求：不需要处理
                    request.isCleared() -> {
                        // 已清除的请求不需要处理
                    }
                    // 其他状态的请求：清除并添加到待处理列表
                    else -> {
                        request.clear()
                        pendingRequests.add(request)
                    }
                }
            }
        }
    }

    /**
     * 开始任何尚未完成或失败的请求
     */
    fun resumeRequests() {
        writeLock.withLock {
            isPaused = false
            for (request in getSnapshot(requests)) {
                // 我们不需要在这里检查清除状态。任何用户的显式清除都会
                // 从跟踪器中移除请求，所以在这里找到清除请求的唯一方式
                // 是我们清除了它。因此，恢复清除的请求应该是安全的。
                if (!request.isComplete() && !request.isRunning()) {
                    request.begin()
                }
            }
            pendingRequests.clear()
        }
    }

    /**
     * 重启失败的请求并取消和重启进行中的请求
     */
    fun restartRequests() {
        writeLock.withLock {
            for (request in getSnapshot(requests)) {
                if (!request.isComplete() && !request.isCleared()) {
                    request.clear()
                    if (!isPaused) {
                        request.begin()
                    } else {
                        // 确保请求将在onResume中重启
                        pendingRequests.add(request)
                    }
                }
            }
        }
    }

    /**
     * 取消所有请求并清除它们的资源
     *
     * 在此调用后请求无法重启
     */
    fun clearRequests() {
        for (request in getSnapshot(requests)) {
            clearAndRemove(request)
        }
        pendingRequests.clear()
    }

    /**
     * 获取请求快照
     */
    private fun getSnapshot(requests: Set<MediaRequest>): List<MediaRequest> {
        return requests.toList()
    }

    /**
     * 获取活跃请求数量
     */
    fun getActiveRequestCount(): Int {
        return requests.size
    }

    /**
     * 获取待处理请求数量
     */
    fun getPendingRequestCount(): Int {
        return pendingRequests.size
    }

    /**
     * 获取所有请求
     */
    fun getAllRequests(): Set<MediaRequest> {
        return requests.toSet()
    }

    /**
     * 获取所有待处理请求
     */
    fun getAllPendingRequests(): Set<MediaRequest> {
        return pendingRequests.toSet()
    }

    /**
     * 检查请求是否被跟踪
     */
    fun isTracked(request: MediaRequest): Boolean {
        return requests.contains(request)
    }

    /**
     * 检查请求是否待处理
     */
    fun isPending(request: MediaRequest): Boolean {
        return pendingRequests.contains(request)
    }


    /**
     * 获取统计信息
     */
    fun getStats(): RequestStats {
        return RequestStats(
            totalRequests = requests.size,
            pendingRequests = pendingRequests.size,
            isPaused = isPaused
        )
    }

    override fun toString(): String {
        return "RequestTracker{numRequests=${requests.size}, isPaused=$isPaused, pendingRequests=${pendingRequests.size}}"
    }

    /**
     * 请求统计信息
     */
    data class RequestStats(
        val totalRequests: Int,
        val pendingRequests: Int,
        val isPaused: Boolean
    )

}