package com.kernelflux.aniflux.manager

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.AnimationRequestManager
import com.kernelflux.aniflux.util.Util
import java.util.concurrent.ConcurrentHashMap

/**
 * 生命周期感知的AnimationRequestManager检索器
 */
class LifecycleAnimationRequestManagerRetriever(
    private val factory: AnimationRequestManagerRetriever.AnimationRequestManagerFactory
) {
    private val lifecycleToRequestManager = ConcurrentHashMap<Lifecycle, AnimationRequestManager>()

    /**
     * 获取指定生命周期的RequestManager
     */
    fun getOnly(lifecycle: Lifecycle): AnimationRequestManager? {
        Util.assertMainThread()
        return lifecycleToRequestManager[lifecycle]
    }

    /**
     * 获取或创建RequestManager
     */
    fun getOrCreate(
        context: Context,
        aniFlux: AniFlux,
        lifecycle: Lifecycle,
        childFragmentManager: FragmentManager,
        isParentVisible: Boolean
    ): AnimationRequestManager {
        Util.assertMainThread()

        var result = getOnly(lifecycle)
        if (result == null) {
            val lifecycleLifecycle = AnimationLifecycleLifecycle(lifecycle)
            result = factory.build(
                aniFlux = aniFlux,
                lifecycle = lifecycleLifecycle,
                treeNode = SupportAnimationRequestManagerTreeNode(childFragmentManager),
                context = context
            )

            lifecycleToRequestManager[lifecycle] = result
            aniFlux.registerRequestManager(result)

            lifecycleLifecycle.addListener(object : AnimationLifecycleListener {
                override fun onStart() {}
                override fun onStop() {}
                override fun onDestroy() {
                    lifecycleToRequestManager.remove(lifecycle)
                }
            })

            // 如果父级可见，启动RequestManager
            if (isParentVisible) {
                result.onStart()
            }
        }
        return result
    }

    /**
     * 支持Fragment的RequestManagerTreeNode实现
     */
    private inner class SupportAnimationRequestManagerTreeNode(
        private val childFragmentManager: FragmentManager
    ) : AnimationRequestManagerTreeNode {

        override fun getDescendants(): Set<AnimationRequestManager> {
            val result = mutableSetOf<AnimationRequestManager>()
            getChildFragmentsRecursive(childFragmentManager, result)
            return result
        }

        private fun getChildFragmentsRecursive(
            fragmentManager: FragmentManager,
            requestManagers: MutableSet<AnimationRequestManager>
        ) {
            val children = fragmentManager.fragments
            for (i in children.indices) {
                val child = children[i]
                getChildFragmentsRecursive(child.childFragmentManager, requestManagers)
                val fromChild = getOnly(child.lifecycle)
                if (fromChild != null) {
                    requestManagers.add(fromChild)
                }
            }
        }
    }
}

