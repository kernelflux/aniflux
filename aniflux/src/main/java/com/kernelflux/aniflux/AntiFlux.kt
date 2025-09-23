package com.kernelflux.aniflux

import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kernelflux.aniflux.cache.MediaCache
import com.kernelflux.aniflux.config.MediaTarget
import com.kernelflux.aniflux.request.MediaRequestManager
import com.kernelflux.aniflux.request.MediaRequestManagerRetriever
import kotlin.concurrent.Volatile

/**
 * @author: kerneflux
 * @date: 2025/9/20
 *  媒体加载器入口点
 */
class AntiFlux : ComponentCallbacks2 {
    private val managers: MutableList<MediaRequestManager> = ArrayList()
    private val antiFluxContext: AntiFluxContext
    private val mediaCache: MediaCache
    private val mediaRequestManagerRetriever: MediaRequestManagerRetriever

    companion object {
        private const val DESTROYED_ACTIVITY_WARNING: String =
            ("You cannot start a load on a not yet attached View or a Fragment where getActivity() "
                    + "returns null (which usually occurs when getActivity() is called before the Fragment "
                    + "is attached or after the Fragment is destroyed).")

        @Volatile
        @JvmStatic
        private var isInitializing: Boolean = false

        @Volatile
        @JvmStatic
        private var antiFlux: AntiFlux? = null


        @JvmStatic
        fun init(context: Context) {
            synchronized(AntiFlux::class.java) {
                antiFlux?.also {
                    unInit()
                }
                initializeAntiFlux(context).apply {
                    antiFlux = this
                }
            }
        }

        @JvmStatic
        private fun initializeAntiFlux(context: Context): AntiFlux {
            val applicationContext = context.applicationContext
            val mediaCache = MediaCache.getInstance()
            val mediaRequestManagerRetriever = MediaRequestManagerRetriever()
            val createAntiFlux = AntiFlux(context, mediaCache, mediaRequestManagerRetriever)
            applicationContext.registerComponentCallbacks(createAntiFlux)
            return createAntiFlux
        }


        @JvmStatic
        fun unInit() {
            synchronized(AntiFlux::class.java) {
                antiFlux?.also {
                    it.getContext().applicationContext.unregisterComponentCallbacks(it)
                }
                antiFlux = null
            }
        }

        @JvmStatic
        fun get(context: Context): AntiFlux {
            return antiFlux ?: synchronized(AntiFlux::class.java) {
                antiFlux ?: checkAndInitializeAntiFlux(context).apply {
                    antiFlux = this
                }
            }
        }


        @JvmStatic
        private fun checkAndInitializeAntiFlux(context: Context): AntiFlux {
            if (isInitializing) {
                throw IllegalStateException("AntiFlux has been called recursively, this is probably an internal library error!")
            }

            isInitializing = true
            val tmpAntiFlux = try {
                initializeAntiFlux(context)
            } finally {
                isInitializing = false
            }
            return tmpAntiFlux
        }


        private fun getRetriever(context: Context?): MediaRequestManagerRetriever {
            // Context could be null for other reasons (ie the user passes in null), but in practice it will
            // only occur due to errors with the Fragment lifecycle.
            if (context == null) {
                throw NullPointerException(DESTROYED_ACTIVITY_WARNING)
            }
            return get(context).getMediaRequestManagerRetriever()
        }

        @JvmStatic
        fun with(context: Context): MediaRequestManager {
            return getRetriever(context).get(context)
        }

        @JvmStatic
        fun with(activity: FragmentActivity): MediaRequestManager {
            return getRetriever(activity).get(activity)
        }

        @JvmStatic
        fun with(fragment: Fragment): MediaRequestManager {
            return getRetriever(fragment.context).get(fragment)
        }

        @JvmStatic
        @Suppress("DEPRECATION")
        fun with(fragment: android.app.Fragment): MediaRequestManager {
            val activity = fragment.activity
            if (activity == null) {
                throw NullPointerException(DESTROYED_ACTIVITY_WARNING)
            }
            return with(activity.applicationContext)
        }

        @JvmStatic
        fun with(view: View): MediaRequestManager {
            return getRetriever(view.context).get(view)
        }

    }

    constructor(
        context: Context,
        mediaCache: MediaCache,
        mediaRequestManagerRetriever: MediaRequestManagerRetriever
    ) {
        this.mediaCache = mediaCache
        this.mediaRequestManagerRetriever = mediaRequestManagerRetriever
        antiFluxContext = AntiFluxContext(context)
    }


    fun getContext(): Context {
        return antiFluxContext.baseContext
    }

    fun getAntiFluxContext(): AntiFluxContext {
        return antiFluxContext
    }

    fun getMediaRequestManagerRetriever(): MediaRequestManagerRetriever {
        return mediaRequestManagerRetriever
    }

    fun registerRequestManager(requestManager: MediaRequestManager) {
        synchronized(managers) {
            check(!managers.contains(requestManager)) { "Cannot register already registered manager" }
            managers.add(requestManager)
        }
    }

    fun unregisterRequestManager(requestManager: MediaRequestManager) {
        synchronized(managers) {
            check(managers.contains(requestManager)) { "Cannot unregister not yet registered manager" }
            managers.remove(requestManager)
        }
    }

    fun removeFromManagers(target: MediaTarget): Boolean {
        synchronized(managers) {
            for (manager in managers) {
                if (manager.untrack(target)) {
                    return true
                }
            }
        }
        return false
    }


    fun trimMemory(level: Int) {
        // Request managers need to be trimmed before the caches and pools, in order for the latter to
        // have the most benefit.
        synchronized(managers) {
            for (manager in managers) {
                manager.onTrimMemory(level)
            }
        }
    }

    fun clearMemory() {

    }

    override fun onTrimMemory(level: Int) {
        trimMemory(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        //
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        clearMemory()
    }

}