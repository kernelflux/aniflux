package com.kernelflux.aniflux.manager

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.kernelflux.aniflux.AniFlux

/**
 * 动画请求管理器
 * 负责管理动画请求的生命周期，避免内存泄漏
 */
class AnimationRequestManager(
    private val aniFlux: AniFlux,
    private val lifecycle: AnimationLifecycle,
    private val treeNode: AnimationRequestManagerTreeNode,
    private val context: Context
) : AnimationLifecycleListener, ComponentCallbacks2 {


    init {
        lifecycle.addListener(this)
    }


    // ComponentCallbacks2
    override fun onTrimMemory(level: Int) {
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // 配置变化时不需要特殊处理
    }

    override fun onStart() {
    }

    override fun onStop() {
    }

    override fun onDestroy() {
    }


}
