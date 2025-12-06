package com.kernelflux.aniflux.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.AnimationRequestManager
import com.kernelflux.aniflux.util.Util

/**
 * Animation request manager retriever
 * Supports fine-grained Context/Activity/Fragment management
 */
class AnimationRequestManagerRetriever(
    sFactory: AnimationRequestManagerFactory? = null
) {
    private val factory: AnimationRequestManagerFactory = sFactory ?: DEFAULT_FACTORY

    // Objects used to find Fragments and Activities containing views.
    private val tempViewToSupportFragment = ArrayMap<View, Fragment>()

    @Volatile
    private var applicationManager: AnimationRequestManager? = null

    private val lifecycleRequestManagerRetriever: LifecycleAnimationRequestManagerRetriever =
        LifecycleAnimationRequestManagerRetriever(this.factory)

    /**
     * Get application-level RequestManager
     */
    @Synchronized
    private fun getApplicationManager(context: Context): AnimationRequestManager {
        return applicationManager ?: synchronized(this) {
            val applicationContext = context.applicationContext
            val aniFluxInstance = AniFlux.get(applicationContext)
            applicationManager ?: factory.build(
                aniFlux = aniFluxInstance,
                lifecycle = ApplicationAnimationLifecycle(),
                treeNode = EmptyAnimationRequestManagerTreeNode(),
                context = applicationContext
            ).also {
                aniFluxInstance.registerRequestManager(it)
                applicationManager = it
            }
        }
    }

    /**
     * Get RequestManager - Context version
     */
    fun get(context: Context?): AnimationRequestManager {
        if (context == null) {
            throw IllegalArgumentException("You cannot start a load on a null Context")
        } else if (Util.isOnMainThread() && context !is Application) {
            when (context) {
                is FragmentActivity -> return get(context)
                is ContextWrapper -> {
                    // Only unwrap when baseContext has non-null application context
                    val baseContext = context.baseContext
                    if (baseContext.applicationContext != null) {
                        return get(baseContext)
                    }
                }
            }
        }
        return getApplicationManager(context)
    }

    /**
     * Get RequestManager - FragmentActivity version
     */
    fun get(activity: FragmentActivity): AnimationRequestManager {
        if (Util.isOnBackgroundThread()) {
            return get(activity.applicationContext)
        }
        assertNotDestroyed(activity)

        val isActivityVisible = isActivityVisible(activity)

        val aniFluxInstance = AniFlux.get(activity)
        return lifecycleRequestManagerRetriever.getOrCreate(
            context = activity,
            aniFlux = aniFluxInstance,
            lifecycle = activity.lifecycle,
            childFragmentManager = activity.supportFragmentManager,
            isParentVisible = isActivityVisible
        )
    }

    /**
     * Get RequestManager - Fragment version
     */
    fun get(fragment: Fragment): AnimationRequestManager {
        val context = fragment.context
            ?: throw IllegalArgumentException("You cannot start a load on a fragment before it is attached or after it is destroyed")

        if (Util.isOnBackgroundThread()) {
            return get(context.applicationContext)
        }
        val childFragmentManager = fragment.childFragmentManager
        val aniFluxInstance = AniFlux.get(context)
        return lifecycleRequestManagerRetriever.getOrCreate(
            context = context,
            aniFlux = aniFluxInstance,
            lifecycle = fragment.lifecycle,
            childFragmentManager = childFragmentManager,
            isParentVisible = fragment.isVisible
        )
    }

    /**
     * Get RequestManager - View version
     */
    fun get(view: View): AnimationRequestManager {
        if (Util.isOnBackgroundThread()) {
            return get(view.context.applicationContext)
        }
        val context = view.context ?: throw IllegalArgumentException("Unable to obtain a request manager for a view without a Context")
        val activity = findActivity(context) ?: return get(context.applicationContext)

        // Support Fragment
        if (activity is FragmentActivity) {
            val fragment = findSupportFragment(view, activity)
            return if (fragment != null) get(fragment) else get(activity)
        }

        // Standard Fragment
        return get(context.applicationContext)
    }

    /**
     * Find all support Fragments and their child Fragments
     */
    private fun findAllSupportFragmentsWithViews(
        topLevelFragments: Collection<Fragment>?,
        result: MutableMap<View, Fragment>
    ) {
        if (topLevelFragments == null) return

        for (fragment in topLevelFragments) {
            val fmView = fragment.view ?: continue
            result[fmView] = fragment
            findAllSupportFragmentsWithViews(
                fragment.childFragmentManager.fragments,
                result
            )
        }
    }

    /**
     * Find support Fragment containing specified View
     */
    private fun findSupportFragment(target: View, activity: FragmentActivity): Fragment? {
        tempViewToSupportFragment.clear()
        findAllSupportFragmentsWithViews(
            activity.supportFragmentManager.fragments,
            tempViewToSupportFragment
        )

        var result: Fragment? = null
        val activityRoot = activity.findViewById<View>(android.R.id.content)
        var current: View? = target

        while (current != null && current != activityRoot) {
            result = tempViewToSupportFragment[current]
            if (result != null) break

            current = if (current.parent is View) {
                current.parent as View
            } else {
                break
            }
        }
        tempViewToSupportFragment.clear()
        return result
    }

    /**
     * Find Activity from Context
     */
    private fun findActivity(context: Context): Activity? {
        return when (context) {
            is Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
    }

    /**
     * Check if Activity is destroyed
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun assertNotDestroyed(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            throw IllegalArgumentException("You cannot start a load for a destroyed activity")
        }
    }

    /**
     * Check if Activity is visible
     */
    private fun isActivityVisible(context: Context): Boolean {
        // This is a simple heuristic, but it's all we can do
        // We'd rather err on the side of visibility and start requests, than err on invisibility and ignore valid requests
        val activity = findActivity(context)
        return activity == null || !activity.isFinishing
    }

    /**
     * Factory interface for creating RequestManager
     */
    fun interface AnimationRequestManagerFactory {
        fun build(
            aniFlux: AniFlux,
            lifecycle: AnimationLifecycle,
            treeNode: AnimationRequestManagerTreeNode,
            context: Context
        ): AnimationRequestManager
    }

    companion object {
        private val DEFAULT_FACTORY =
            AnimationRequestManagerFactory { aniFlux, lifecycle, treeNode, context ->
                AnimationRequestManager(aniFlux, lifecycle, treeNode, context)
            }
    }
}

