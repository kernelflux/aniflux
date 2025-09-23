package com.kernelflux.aniflux.request

import androidx.lifecycle.Lifecycle
import java.util.concurrent.ConcurrentHashMap

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 请求管理器注册表 - 用于解决循环依赖问题
 */
object MediaRequestManagerRegistry {
    private val lifecycleToRequestManager = ConcurrentHashMap<Lifecycle, MediaRequestManager>()

    /**
     * 注册RequestManager
     */
    @JvmStatic
    fun register(lifecycle: Lifecycle, requestManager: MediaRequestManager) {
        lifecycleToRequestManager[lifecycle] = requestManager
    }

    /**
     * 获取RequestManager
     */
    @JvmStatic
    fun getRequestManager(lifecycle: Lifecycle): MediaRequestManager? {
        return lifecycleToRequestManager[lifecycle]
    }

    /**
     * 移除RequestManager
     */
    @JvmStatic
    fun unregister(lifecycle: Lifecycle) {
        lifecycleToRequestManager.remove(lifecycle)
    }
}