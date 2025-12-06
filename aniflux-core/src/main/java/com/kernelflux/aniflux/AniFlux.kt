package com.kernelflux.aniflux

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.kernelflux.aniflux.log.AniFluxLogLevel
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
import com.kernelflux.aniflux.util.AnimationCompatibilityHelper
import com.kernelflux.aniflux.util.Util
import java.util.Collections

/**
 * AniFlux main entry class
 * Inspired by Glide design, placed in core module
 * 
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
        defaultRequestListeners: List<AnimationRequestListener<Any>>,
        enableAnimationCompatibility: Boolean = true
    ) {
        this.appContext = context.applicationContext
        this.requestManagerRetriever = requestManagerRetriever
        this.connectivityMonitorFactory = connectivityMonitorFactory
        this.defaultRequestListeners = defaultRequestListeners
        this.logLevel = logLevel
        
        // Initialize disk cache
        val diskCacheDir = File(context.cacheDir, "aniflux_disk_cache")
        val diskCache = LruAnimationDiskCache(diskCacheDir, 100 * 1024 * 1024) // 100MB
        
        // Initialize Engine (pass disk cache)
        this.engine = AnimationEngine(animationDiskCache = diskCache)
        
        // Initialize animation compatibility (handle system animation settings)
        // This ensures animations work correctly even when system animations are disabled in developer options
        if (enableAnimationCompatibility) {
            AnimationCompatibilityHelper.initialize(context.contentResolver)
        }
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
         * Initialize with configuration
         * 
         * @param context Context
         * @param config Configuration builder
         */
        @JvmStatic
        fun init(context: Context, config: AniFluxConfiguration.() -> Unit) {
            synchronized(AniFlux::class.java) {
                aniFlux?.let { unInit() }
                val configuration = AniFluxConfiguration().apply(config)
                val instance = initializeAniFlux(context, configuration.enableAnimationCompatibility)
                configuration.placeholderImageLoader?.let {
                    instance.setPlaceholderImageLoader(it)
                }
                aniFlux = instance
            }
        }

        @JvmStatic
        private fun initializeAniFlux(context: Context, enableAnimationCompatibility: Boolean = true): AniFlux {
            val appCxt = context.applicationContext
            val connectivityMonitorFactory = DefaultAnimationConnectivityMonitorFactory()
            val defaultRequestListeners = Collections.emptyList<AnimationRequestListener<Any>>()
            val requestManagerRetriever = AnimationRequestManagerRetriever()
            val logLevel = AniFluxLogLevel.INFO.priority

            loadLoaderRegistries()
            
            val createAniFlux = AniFlux(
                appCxt,
                requestManagerRetriever,
                connectivityMonitorFactory,
                logLevel,
                defaultRequestListeners,
                enableAnimationCompatibility
            )
            appCxt.registerComponentCallbacks(createAniFlux)
            aniFlux = createAniFlux
            return createAniFlux
        }
        
        /**
         * Load all format module registration classes
         * At runtime, this method body is empty, actual registration code is inserted by plugin at compile time
         */
        private fun loadLoaderRegistries() {
            // This method body will be replaced by aniflux-gradle-plugin at compile time
            // Plugin will insert code similar to:
            // GIFLoaderRegistry.load()
            // PAGLoaderRegistry.load()
            // ...
        }

        @JvmStatic
        fun unInit() {
            synchronized(AniFlux::class.java) {
                aniFlux?.let {
                    it.appContext.applicationContext.unregisterComponentCallbacks(it)
                }
                // Unregister animation settings observer
                com.kernelflux.aniflux.util.AnimationCompatibilityHelper.unregisterSettingsObserver()
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
     * Set placeholder image loader
     * Business code needs to implement PlaceholderImageLoader interface
     * 
     * @param loader Placeholder image loader implementation
     */
    fun setPlaceholderImageLoader(loader: PlaceholderImageLoader) {
        this.placeholderImageLoader = loader
    }
    
    /**
     * Get placeholder image loader
     * 
     * @return Placeholder image loader, returns null if not set
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
     * Add RequestManager to global list
     */
    fun registerRequestManager(manager: AnimationRequestManager) {
        synchronized(managers) {
            managers.add(manager)
        }

    }

    /**
     * Remove RequestManager from global list
     */
    @Synchronized
    fun unregisterRequestManager(manager: AnimationRequestManager) {
        synchronized(managers) {
            managers.remove(manager)
        }
    }

    fun clearMemory() {
        Util.assertMainThread()
        // Clear Engine cache
        engine.clear()
    }

    fun trimMemory(level: Int) {
        Util.assertMainThread()
        synchronized(managers) {
            for (manager in managers) {
                manager.onTrimMemory(level)
            }
        }
        
        // Clear Engine cache
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

