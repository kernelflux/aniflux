package com.kernelflux.aniflux.request

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.kernelflux.aniflux.AntiFlux
import com.kernelflux.aniflux.lifecycle.MediaLifecycleAdapter
import com.kernelflux.aniflux.lifecycle.MediaLifecycleListener
import com.kernelflux.aniflux.utils.Util

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 生命周期请求管理器检索器 - 支持所有生命周期类型
 */
class LifecycleMediaRequestManagerRetriever(private val factory: MediaRequestManagerRetriever.MediaRequestManagerFactory) {
    private val lifecycleToRequestManager = HashMap<Lifecycle, MediaRequestManager>()


    fun getOnly(lifecycle: Lifecycle): MediaRequestManager? {
        Util.assertMainThread()
        return lifecycleToRequestManager.get(lifecycle)
    }

    fun getOrCreate(
        context: Context,
        antiFlux: AntiFlux,
        lifecycle: Lifecycle,
        childFragmentManger: FragmentManager,
        isParentVisible: Boolean
    ): MediaRequestManager {
        Util.assertMainThread()
        var result: MediaRequestManager? = getOnly(lifecycle)
        if (result == null) {
            val mediaLifecycle = MediaLifecycleAdapter(lifecycle)
            result = factory.build(
                antiFlux,
                mediaLifecycle,
                SupportMediaRequestManagerTreeNode(childFragmentManger),
                context
            )
            lifecycleToRequestManager.put(lifecycle, result)
            mediaLifecycle.addListener(object : MediaLifecycleListener {
                override fun onStart() {}

                override fun onStop() {}

                override fun onDestroy() {
                    lifecycleToRequestManager.remove(lifecycle)
                }
            })

            if (isParentVisible) {
                result.onStart()
            }
        }
        return result
    }


    private inner class SupportMediaRequestManagerTreeNode(private val childFragmentManager: FragmentManager) :
        MediaRequestManagerTreeNode {

        override fun getDescendants(): MutableSet<MediaRequestManager> {
            val result: MutableSet<MediaRequestManager> = HashSet()
            getChildFragmentsRecursive(childFragmentManager, result)
            return result
        }

        fun getChildFragmentsRecursive(
            fragmentManager: FragmentManager,
            requestManagers: MutableSet<MediaRequestManager>
        ) {
            val children = fragmentManager.fragments
            var i = 0
            val size = children.size
            while (i < size) {
                val child = children[i]
                getChildFragmentsRecursive(child.getChildFragmentManager(), requestManagers)
                val fromChild: MediaRequestManager? = getOnly(child.lifecycle)
                if (fromChild != null) {
                    requestManagers.add(fromChild)
                }
                i++
            }
        }
    }
}