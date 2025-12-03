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
 * 动画请求管理器检索器
 * 支持细粒度的Context/Activity/Fragment管理
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
     * 获取应用级别的RequestManager
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
     * 获取RequestManager - Context版本
     */
    fun get(context: Context?): AnimationRequestManager {
        if (context == null) {
            throw IllegalArgumentException("You cannot start a load on a null Context")
        } else if (Util.isOnMainThread() && context !is Application) {
            when (context) {
                is FragmentActivity -> return get(context)
                is ContextWrapper -> {
                    // 只有当baseContext有非null的application context时才unwrap
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
     * 获取RequestManager - FragmentActivity版本
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
     * 获取RequestManager - Fragment版本
     */
    fun get(fragment: Fragment): AnimationRequestManager {
        val context = fragment.context
            ?: throw IllegalArgumentException("You cannot start a load on a fragment before it is attached or after it is destroyed")

        if (Util.isOnBackgroundThread()) {
            return get(context.applicationContext)
        }

        // 如果Fragment没有hosted by activity，使用application context
        val activity = fragment.activity
        if (activity != null) {
            // 注册frame waiter等逻辑可以在这里添加
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
     * 获取RequestManager - View版本
     */
    fun get(view: View): AnimationRequestManager {
        if (Util.isOnBackgroundThread()) {
            return get(view.context.applicationContext)
        }

        requireNotNull(view) { "View cannot be null" }
        val context = view.context
            ?: throw IllegalArgumentException("Unable to obtain a request manager for a view without a Context")

        val activity = findActivity(context)
        // View可能在service等其他地方
        if (activity == null) {
            return get(context.applicationContext)
        }

        // 支持Fragment
        if (activity is FragmentActivity) {
            val fragment = findSupportFragment(view, activity)
            return if (fragment != null) get(fragment) else get(activity)
        }

        // 标准Fragment
        return get(context.applicationContext)
    }

    /**
     * 查找所有支持Fragment及其子Fragment
     */
    private fun findAllSupportFragmentsWithViews(
        topLevelFragments: Collection<Fragment>?,
        result: MutableMap<View, Fragment>
    ) {
        if (topLevelFragments == null) return

        for (fragment in topLevelFragments) {
            val fmView = fragment.view
            if (fmView == null) continue
            result[fmView] = fragment
            findAllSupportFragmentsWithViews(
                fragment.childFragmentManager.fragments,
                result
            )
        }
    }

    /**
     * 查找包含指定View的支持Fragment
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
     * 从Context中查找Activity
     */
    private fun findActivity(context: Context): Activity? {
        return when (context) {
            is Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
    }

    /**
     * 检查Activity是否已销毁
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun assertNotDestroyed(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            throw IllegalArgumentException("You cannot start a load for a destroyed activity")
        }
    }

    /**
     * 检查Activity是否可见
     */
    private fun isActivityVisible(context: Context): Boolean {
        // 这是一个简单的启发式方法，但这是我们能做的全部
        // 我们宁愿在可见性方面出错并开始请求，也不愿在不可见性方面出错并忽略有效请求
        val activity = findActivity(context)
        return activity == null || !activity.isFinishing
    }

    /**
     * 用于创建RequestManager的工厂接口
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

