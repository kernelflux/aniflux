package com.kernelflux.aniflux

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.kernelflux.aniflux.manager.AnimationRequestManager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
class AniFlux private constructor(
    private val context: Context
) : ComponentCallbacks2 {

    /** 全局RequestManager列表，参考Glide设计 */
    private val managers = mutableListOf<AnimationRequestManager>()

    /**
     * 添加RequestManager到全局列表
     */
    fun registerRequestManager(manager: AnimationRequestManager) {
        synchronized(managers) {
            managers.add(manager)
        }
    }

    /**
     * 从全局列表中移除RequestManager
     */
    @Synchronized
    private fun unregisterRequestManager(manager: AnimationRequestManager) {
        synchronized(managers) {
            managers.remove(manager)
        }
    }


    override fun onTrimMemory(p0: Int) {
    }

    override fun onConfigurationChanged(p0: Configuration) {
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AniFlux? = null

        fun get(context: Context): AniFlux {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AniFlux(context.applicationContext).also {
                    INSTANCE = it
                    // 注册系统回调
                    context.registerComponentCallbacks(it)
                }
            }
        }
    }
}