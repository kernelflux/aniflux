package com.kernelflux.aniflux.request

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.ArrayMap
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kernelflux.aniflux.AntiFlux
import com.kernelflux.aniflux.lifecycle.ApplicationLifecycle
import com.kernelflux.aniflux.lifecycle.MediaLifecycle
import com.kernelflux.aniflux.utils.AntiFluxLogger
import com.kernelflux.aniflux.utils.Util
import kotlin.concurrent.Volatile

/**
 * @author: kerneflux
 * @date: 2025/9/21
 *  请求管理器检索器
 */
class MediaRequestManagerRetriever(factory: MediaRequestManagerFactory? = null) {

    companion object {
        private const val TAG = "media_request_manager_retriever_tag"

        @SuppressLint("ObsoleteSdkInt")
        @JvmStatic
        fun assertNotDestroyed(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
                throw IllegalArgumentException("You cannot start a load for a destroyed activity")
            }
        }

        @JvmStatic
        private fun findActivity(context: Context): Activity? {
            return context as? Activity
                ?: if (context is ContextWrapper) {
                    findActivity(context.baseContext)
                } else {
                    null
                }
        }

        @JvmStatic
        private fun isActivityVisible(context: Context): Boolean {
            val activity = findActivity(context)
            return activity == null || !activity.isFinishing
        }

        @JvmStatic
        private fun findAllSupportFragmentsWithViews(
            topLevelFragments: Collection<Fragment>?,
            result: MutableMap<View, Fragment>
        ) {
            if (topLevelFragments.isNullOrEmpty()) {
                return
            }
            for (fragment in topLevelFragments) {
                val fmView = fragment.view
                if (fmView == null) {
                    continue
                }
                result.put(fmView, fragment)
                findAllSupportFragmentsWithViews(fragment.childFragmentManager.fragments, result)
            }
        }

    }

    private val sDefaultFactory: MediaRequestManagerFactory =
        object : MediaRequestManagerFactory {
            override fun build(
                antiFlux: AntiFlux,
                mediaLifecycle: MediaLifecycle,
                mediaRequestManagerTreeNode: MediaRequestManagerTreeNode,
                context: Context
            ): MediaRequestManager {
                return MediaRequestManager(
                    antiFlux,
                    mediaLifecycle,
                    mediaRequestManagerTreeNode,
                    context
                )
            }
        }

    @Volatile
    private var applicationManager: MediaRequestManager? = null
    private val tempViewToSupportFragment = ArrayMap<View, Fragment>()
    private val factory: MediaRequestManagerFactory
    private val lifecycleMediaRequestManagerRetriever: LifecycleMediaRequestManagerRetriever

    init {
        AntiFluxLogger.d(TAG) {
            "MediaRequestManagerRetriever init..."
        }
        this.factory = factory ?: sDefaultFactory
        lifecycleMediaRequestManagerRetriever = LifecycleMediaRequestManagerRetriever(this.factory)
    }

    private fun getApplicationManager(context: Context): MediaRequestManager {
        return applicationManager ?: synchronized(this) {
            applicationManager ?: factory.build(
                AntiFlux.get(context.applicationContext),
                ApplicationLifecycle(),
                EmptyMediaRequestManagerTreeNode(),
                context.applicationContext
            ).also {
                applicationManager = it
            }
        }
    }

    fun get(context: Context?): MediaRequestManager {
        if (context == null) {
            throw IllegalArgumentException("You cannot start a load on a null Context")
        } else if (Util.isOnMainThread() && context !is Application) {
            if (context is FragmentActivity) {
                return get(context)
            } else if (context is ContextWrapper && context.baseContext.applicationContext != null) {
                return get(context.baseContext)
            }
        }
        return getApplicationManager(context)
    }

    fun get(activity: FragmentActivity): MediaRequestManager {
        if (Util.isOnBackgroundThread()) {
            return get(activity.applicationContext)
        }
        assertNotDestroyed(activity)
        val isActivityVisible = isActivityVisible(activity)
        val antiFlux = AntiFlux.get(activity.applicationContext)
        return lifecycleMediaRequestManagerRetriever.getOrCreate(
            activity,
            antiFlux,
            activity.lifecycle,
            activity.supportFragmentManager,
            isActivityVisible
        )
    }

    fun get(fragment: Fragment): MediaRequestManager {
        val fmCxt = fragment.context
        if (fmCxt == null) {
            throw NullPointerException("You cannot start a load on a fragment before it is attached or after it is destroyed")
        }
        if (Util.isOnBackgroundThread()) {
            return get(fmCxt.applicationContext)
        }

        val fm = fragment.childFragmentManager
        val antiFlux = AntiFlux.get(fmCxt.applicationContext)
        return lifecycleMediaRequestManagerRetriever.getOrCreate(
            fmCxt,
            antiFlux,
            fragment.lifecycle,
            fm,
            fragment.isVisible
        )
    }

    fun get(activity: Activity): MediaRequestManager {
        return get(activity.applicationContext)
    }

    fun get(view: View): MediaRequestManager {
        if (Util.isOnBackgroundThread()) {
            return get(view.context.applicationContext)
        }
        val viewCxt = view.context
        if (viewCxt == null) {
            throw NullPointerException("Unable to obtain a request manager for a view without a Context")
        }
        val activity = findActivity(viewCxt)
        if (activity == null) {
            return get(viewCxt.applicationContext)
        }

        if (activity is FragmentActivity) {
            val fragment = findAndroidXFragment(view, activity)
            return if (fragment != null) get(fragment) else get(activity)
        }
        return get(viewCxt.applicationContext)
    }


    private fun findAndroidXFragment(
        target: View,
        activity: FragmentActivity
    ): Fragment? {
        tempViewToSupportFragment.clear()
        findAllSupportFragmentsWithViews(
            activity.supportFragmentManager.fragments,
            tempViewToSupportFragment
        )
        var result: Fragment? = null
        val activityRoot = activity.findViewById<View>(android.R.id.content)
        var current = target
        while (current != activityRoot) {
            result = tempViewToSupportFragment.get(current)
            if (result != null) {
                break
            }
            if (current.parent is View) {
                current = current.parent as View
            } else {
                break
            }
        }
        tempViewToSupportFragment.clear()
        return result
    }

    @Suppress("DEPRECATION")
    fun get(fragment: android.app.Fragment): MediaRequestManager {
        val fmActivity = fragment.activity
        if (fmActivity == null) {
            throw IllegalArgumentException("You cannot start a load on a fragment before it is attached")
        }
        return get(fmActivity.applicationContext)
    }

    interface MediaRequestManagerFactory {
        fun build(
            antiFlux: AntiFlux,
            mediaLifecycle: MediaLifecycle,
            mediaRequestManagerTreeNode: MediaRequestManagerTreeNode,
            context: Context
        ): MediaRequestManager
    }

}