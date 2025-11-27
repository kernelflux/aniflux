package com.kernelflux.aniflux

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kernelflux.aniflux.cache.LruAnimationDiskCache
import com.kernelflux.aniflux.engine.AnimationEngine
import java.io.File
import com.kernelflux.aniflux.manager.AnimationConnectivityMonitorFactory
import com.kernelflux.aniflux.manager.AnimationRequestManagerRetriever
import com.kernelflux.aniflux.manager.DefaultAnimationConnectivityMonitorFactory
import com.kernelflux.aniflux.placeholder.PlaceholderImageLoader
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.Util
import java.util.Collections

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
class AniFlux : ComponentCallbacks2 {
    private val managers = mutableListOf<AnimationRequestManager>()
    private val appContext: Context
    private val requestManagerRetriever: AnimationRequestManagerRetriever
    private val connectivityMonitorFactory: AnimationConnectivityMonitorFactory
    private val defaultRequestListeners: List<AnimationRequestListener<Any>>
    private val logLevel: Int
    private val engine: AnimationEngine
    
    @Volatile
    private var placeholderImageLoader: PlaceholderImageLoader? = null

    constructor(
        context: Context,
        requestManagerRetriever: AnimationRequestManagerRetriever,
        connectivityMonitorFactory: AnimationConnectivityMonitorFactory,
        logLevel: Int,
        defaultRequestListeners: List<AnimationRequestListener<Any>>
    ) {
        this.appContext = context.applicationContext
        this.requestManagerRetriever = requestManagerRetriever
        this.connectivityMonitorFactory = connectivityMonitorFactory
        this.defaultRequestListeners = defaultRequestListeners
        this.logLevel = logLevel
        
        // 初始化磁盘缓存
        val diskCacheDir = File(context.cacheDir, "aniflux_disk_cache")
        val diskCache = LruAnimationDiskCache(diskCacheDir, 100 * 1024 * 1024) // 100MB
        
        // 初始化Engine（传入磁盘缓存）
        this.engine = AnimationEngine(animationDiskCache = diskCache)
    }


    companion object {
        private const val DESTROYED_ACTIVITY_WARNING: String =
            ("You cannot start a load on a not yet attached View or a Fragment where getActivity() "
                    + "returns null (which usually occurs when getActivity() is called before the Fragment "
                    + "is attached or after the Fragment is destroyed).")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var aniFlux: AniFlux? = null

        @JvmStatic
        fun get(context: Context): AniFlux {
            return aniFlux ?: synchronized(AniFlux::class.java) {
                aniFlux ?: initializeAniFlux(context)
            }
        }

        @JvmStatic
        fun init(context: Context) {
            synchronized(AniFlux::class.java) {
                aniFlux?.let { unInit() }
                initializeAniFlux(context)
            }
        }
        
        /**
         * 带配置的初始化
         * 
         * @param context 上下文
         * @param config 配置构建器
         */
        @JvmStatic
        fun init(context: Context, config: AniFluxConfiguration.() -> Unit) {
            synchronized(AniFlux::class.java) {
                aniFlux?.let { unInit() }
                val configuration = AniFluxConfiguration().apply(config)
                val instance = initializeAniFlux(context)
                configuration.placeholderImageLoader?.let {
                    instance.setPlaceholderImageLoader(it)
                }
                aniFlux = instance
            }
        }

        @JvmStatic
        private fun initializeAniFlux(context: Context): AniFlux {
            val appCxt = context.applicationContext
            val connectivityMonitorFactory = DefaultAnimationConnectivityMonitorFactory()
            val defaultRequestListeners = Collections.emptyList<AnimationRequestListener<Any>>()
            val requestManagerRetriever = AnimationRequestManagerRetriever()
            val logLevel = Log.INFO
            val createAniFlux = AniFlux(
                appCxt,
                requestManagerRetriever,
                connectivityMonitorFactory,
                logLevel,
                defaultRequestListeners
            )
            appCxt.registerComponentCallbacks(createAniFlux)
            aniFlux = createAniFlux
            return createAniFlux
        }

        @JvmStatic
        fun unInit() {
            synchronized(AniFlux::class.java) {
                aniFlux?.let {
                    it.appContext.applicationContext.unregisterComponentCallbacks(it)
                }
                aniFlux = null
            }
        }

        @JvmStatic
        private fun getRetriever(context: Context?): AnimationRequestManagerRetriever {
            if (context == null) {
                throw NullPointerException(DESTROYED_ACTIVITY_WARNING)
            }
            return get(context).requestManagerRetriever
        }

        @JvmStatic
        fun with(context: Context): AnimationRequestManager {
            return getRetriever(context).get(context)
        }

        @JvmStatic
        fun with(activity: Activity): AnimationRequestManager {
            return with(activity.applicationContext)
        }

        @JvmStatic
        fun with(activity: FragmentActivity): AnimationRequestManager {
            return getRetriever(activity).get(activity)
        }

        @JvmStatic
        fun with(fragment: Fragment): AnimationRequestManager {
            return getRetriever(fragment.context).get(fragment)
        }

        @JvmStatic
        fun with(view: View): AnimationRequestManager {
            return getRetriever(view.context).get(view)
        }

    }


    fun getDefaultRequestListeners(): List<AnimationRequestListener<Any>> {
        return defaultRequestListeners
    }

    fun getConnectivityMonitorFactory(): AnimationConnectivityMonitorFactory {
        return connectivityMonitorFactory
    }

    fun getEngine(): AnimationEngine = engine
    
    /**
     * 设置占位图加载器
     * 业务方需要实现 PlaceholderImageLoader 接口
     * 
     * @param loader 占位图加载器实现
     */
    fun setPlaceholderImageLoader(loader: PlaceholderImageLoader) {
        this.placeholderImageLoader = loader
    }
    
    /**
     * 获取占位图加载器
     * 
     * @return 占位图加载器，如果未设置则返回null
     */
    fun getPlaceholderImageLoader(): PlaceholderImageLoader? = placeholderImageLoader

    fun removeFromManagers(target: AnimationTarget<*>): Boolean {
        synchronized(managers) {
            for (requestManager in managers) {
                if (requestManager.untrack(target)) {
                    return true
                }
            }
        }
        return false
    }


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
    fun unregisterRequestManager(manager: AnimationRequestManager) {
        synchronized(managers) {
            managers.remove(manager)
        }
    }

    fun clearMemory() {
        Util.assertMainThread()
        // 清理Engine缓存
        engine.clear()
    }

    fun trimMemory(level: Int) {
        Util.assertMainThread()
        synchronized(managers) {
            for (manager in managers) {
                manager.onTrimMemory(level)
            }
        }
        
        // 清理Engine缓存
        engine.clear()
    }


    override fun onTrimMemory(level: Int) {
        trimMemory(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Do nothing.
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        clearMemory()
    }

}