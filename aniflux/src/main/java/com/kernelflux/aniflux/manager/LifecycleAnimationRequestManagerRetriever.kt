package com.kernelflux.aniflux.manager

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.AnimationRequestManager
import com.kernelflux.aniflux.util.Util
import java.util.concurrent.ConcurrentHashMap

/**
 * Lifecycle-aware AnimationRequestManager retriever
 */
class LifecycleAnimationRequestManagerRetriever(
    private val factory: AnimationRequestManagerRetriever.AnimationRequestManagerFactory
) {
    private val lifecycleToRequestManager = ConcurrentHashMap<Lifecycle, AnimationRequestManager>()

    /**
     * Get RequestManager for specified lifecycle
     */
    fun getOnly(lifecycle: Lifecycle): AnimationRequestManager? {
        Util.assertMainThread()
        return lifecycleToRequestManager[lifecycle]
    }

    /**
     * Get or create RequestManager
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

            // If parent is visible, start RequestManager
            if (isParentVisible) {
                result.onStart()
            }
        }
        return result
    }

    /**
     * Support Fragment RequestManagerTreeNode implementation
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
