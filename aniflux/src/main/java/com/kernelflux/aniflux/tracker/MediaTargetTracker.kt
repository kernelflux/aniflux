package com.kernelflux.aniflux.tracker

import com.kernelflux.aniflux.config.MediaTarget
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 目标跟踪器
 */
class MediaTargetTracker {

    // 使用CopyOnWriteArraySet保证线程安全
    private val targets = CopyOnWriteArraySet<MediaTarget>()

    // 读写锁，用于保护状态变更
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock = lock.readLock()
    private val writeLock = lock.writeLock()

    // 目标状态跟踪
    private val targetStates = ConcurrentHashMap<MediaTarget, TargetState>()

    // 是否已开始
    private var isStarted = false

    // 是否已停止
    private var isStopped = false

    // 是否已销毁
    private var isDestroyed = false

    /**
     * 跟踪目标
     */
    fun track(target: MediaTarget?) {
        if (target == null) {
            return
        }

        writeLock.withLock {
            targets.add(target)
            targetStates[target] = TargetState.TRACKED

            // 如果已经开始，立即通知目标
            if (isStarted && !isStopped && !isDestroyed) {
                target.onLoadStarted()
                targetStates[target] = TargetState.STARTED
            }
        }
    }

    /**
     * 取消跟踪目标
     */
    fun untrack(target: MediaTarget?) {
        if (target == null) {
            return
        }

        writeLock.withLock {
            val removed = targets.remove(target)
            targetStates.remove(target)

            if (removed) {
                // 如果目标正在加载，通知清除
                if (targetStates[target] == TargetState.STARTED) {
                    target.onLoadCleared()
                }
            }
        }
    }

    /**
     * 获取所有目标
     */
    fun getAll(): Set<MediaTarget> {
        return readLock.withLock { targets.toSet() }
    }

    /**
     * 获取目标数量
     */
    fun getTargetCount(): Int {
        return readLock.withLock { targets.size }
    }

    /**
     * 检查目标是否被跟踪
     */
    fun isTracked(target: MediaTarget): Boolean {
        return readLock.withLock { targets.contains(target) }
    }

    /**
     * 获取目标状态
     */
    fun getTargetState(target: MediaTarget): TargetState? {
        return readLock.withLock { targetStates[target] }
    }

    /**
     * 开始 - 通知所有目标开始加载
     */
    fun onStart() {
        writeLock.withLock {
            if (isStarted) {
                return // 已经开始了
            }

            isStarted = true
            isStopped = false

            for (target in targets) {
                try {
                    target.onLoadStarted()
                    targetStates[target] = TargetState.STARTED
                } catch (e: Exception) {
                    //
                }
            }
        }

    }

    /**
     * 停止 - 通知所有目标停止加载
     */
    fun onStop() {
        writeLock.withLock {
            if (isStopped) {
                return // 已经停止了
            }

            isStopped = true

            for (target in targets) {
                try {
                    // 注意：这里不调用onLoadCleared，因为目标可能只是暂停
                    // 真正的清除应该在onDestroy中处理
                    targetStates[target] = TargetState.STOPPED
                } catch (e: Exception) {
                    //
                }
            }
        }
    }

    /**
     * 销毁 - 清除所有目标
     */
    fun onDestroy() {
        writeLock.withLock {
            if (isDestroyed) {
                return // 已经销毁了
            }

            isDestroyed = true
            isStarted = false
            isStopped = false

            for (target in targets) {
                try {
                    target.onLoadCleared()
                    targetStates[target] = TargetState.DESTROYED
                } catch (e: Exception) {
                    //
                }
            }

            targets.clear()
            targetStates.clear()
        }
    }

    /**
     * 清除所有目标
     */
    fun clear() {
        writeLock.withLock {
            for (target in targets) {
                try {
                    target.onLoadCleared()
                } catch (e: Exception) {
                    //
                }
            }

            targets.clear()
            targetStates.clear()
            isStarted = false
            isStopped = false
            isDestroyed = false
        }
    }

    /**
     * 清除指定目标
     */
    fun clear(target: MediaTarget?) {
        if (target == null) {
            return
        }

        writeLock.withLock {
            if (targets.contains(target)) {
                try {
                    target.onLoadCleared()
                    targets.remove(target)
                    targetStates.remove(target)
                } catch (e: Exception) {
                    //
                }
            }
        }
    }


    /**
     * 获取所有目标状态
     */
    fun getAllTargetStates(): Map<MediaTarget, TargetState> {
        return readLock.withLock { targetStates.toMap() }
    }

    /**
     * 强制清除所有目标（用于测试）
     */
    fun forceClearAll() {
        writeLock.withLock {
            for (target in targets) {
                try {
                    target.onLoadCleared()
                } catch (e: Exception) {
                    // 忽略异常
                }
            }

            targets.clear()
            targetStates.clear()
            isStarted = false
            isStopped = false
            isDestroyed = false
        }
    }

    /**
     * 检查是否已开始
     */
    fun isStarted(): Boolean {
        return readLock.withLock { isStarted }
    }

    /**
     * 检查是否已停止
     */
    fun isStopped(): Boolean {
        return readLock.withLock { isStopped }
    }

    /**
     * 检查是否已销毁
     */
    fun isDestroyed(): Boolean {
        return readLock.withLock { isDestroyed }
    }

    override fun toString(): String {
        return "TargetTracker{numTargets=${targets.size}, isStarted=$isStarted, isStopped=$isStopped, isDestroyed=$isDestroyed}"
    }

    /**
     * 目标状态
     */
    enum class TargetState {
        TRACKED,    // 已跟踪
        STARTED,    // 已开始
        STOPPED,    // 已停止
        DESTROYED   // 已销毁
    }


}