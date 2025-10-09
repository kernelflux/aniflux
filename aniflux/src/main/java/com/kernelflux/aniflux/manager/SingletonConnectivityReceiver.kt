package com.kernelflux.aniflux.manager

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.RequiresApi
import com.kernelflux.aniflux.util.AnifluxSuppliers
import com.kernelflux.aniflux.util.Util

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
class SingletonConnectivityReceiver(context: Context) {
    val listeners = HashSet<AnimationConnectivityMonitor.AnimationConnectivityListener>()
    private val frameworkConnectivityMonitor: FrameworkConnectivityMonitor
    private var isRegistered: Boolean = false

    companion object {
        @Volatile
        @JvmStatic
        private var instance: SingletonConnectivityReceiver? = null

        @JvmStatic
        fun get(context: Context): SingletonConnectivityReceiver {
            return instance ?: synchronized(SingletonConnectivityReceiver::class.java) {
                instance ?: SingletonConnectivityReceiver(context.applicationContext)
            }
        }
    }

    init {
        val connectivityManager = AnifluxSuppliers.memorize(object :
            AnifluxSuppliers.AnifluxSupplier<ConnectivityManager> {
            override fun get(): ConnectivityManager {
                return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            }
        }
        )
        val connectivityListener =
            object : AnimationConnectivityMonitor.AnimationConnectivityListener {
                override fun onConnectivityChanged(isConnected: Boolean) {
                    Util.assertMainThread()
                    var toNotify: List<AnimationConnectivityMonitor.AnimationConnectivityListener>
                    synchronized(this@SingletonConnectivityReceiver) {
                        toNotify = ArrayList(listeners)
                    }
                    for (listener in toNotify) {
                        listener.onConnectivityChanged(isConnected)
                    }
                }
            }

        frameworkConnectivityMonitor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FrameworkConnectivityMonitorPostApi24(connectivityManager, connectivityListener)
        } else {
            FrameworkConnectivityMonitorPreApi24(context, connectivityManager, connectivityListener)
        }

    }

    @Synchronized
    fun register(listener: AnimationConnectivityMonitor.AnimationConnectivityListener) {
        listeners.add(listener)

    }

    @Synchronized
    fun unregister(listener: AnimationConnectivityMonitor.AnimationConnectivityListener) {
        listeners.remove(listener)

    }

    private fun maybeRegisterReceiver() {
        if (isRegistered || listeners.isEmpty()) {
            return
        }
        isRegistered = frameworkConnectivityMonitor.register()
    }

    private fun maybeUnregisterReceiver() {
        if (!isRegistered || !listeners.isEmpty()) {
            return
        }
        frameworkConnectivityMonitor.unregister()
        isRegistered = false
    }
}

private interface FrameworkConnectivityMonitor {
    fun register(): Boolean
    fun unregister()
}

@RequiresApi(Build.VERSION_CODES.N)
private class FrameworkConnectivityMonitorPostApi24(
    private val connectivityManager: AnifluxSuppliers.AnifluxSupplier<ConnectivityManager>,
    private val listener: AnimationConnectivityMonitor.AnimationConnectivityListener
) : FrameworkConnectivityMonitor {
    var isConnected: Boolean = false
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            postOnConnectivityChange(true)
        }

        override fun onLost(network: Network) {
            postOnConnectivityChange(false)
        }

        private fun postOnConnectivityChange(newState: Boolean) {
            Util.postOnUiThread {
                onConnectivityChange(newState)
            }
        }

        fun onConnectivityChange(newState: Boolean) {
            Util.assertMainThread()
            val wasConnected = isConnected
            isConnected = newState
            if (wasConnected != newState) {
                listener.onConnectivityChanged(newState)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun register(): Boolean {
        isConnected = connectivityManager.get().activeNetwork != null
        return try {
            connectivityManager.get().registerDefaultNetworkCallback(networkCallback)
            true
        } catch (_: RuntimeException) {
            //
            false
        }

    }

    override fun unregister() {
        connectivityManager.get().unregisterNetworkCallback(networkCallback)
    }
}


private class FrameworkConnectivityMonitorPreApi24(
    private val context: Context,
    private val connectivityManager: AnifluxSuppliers.AnifluxSupplier<ConnectivityManager>,
    private val listener: AnimationConnectivityMonitor.AnimationConnectivityListener
) : FrameworkConnectivityMonitor {
    @Volatile
    private var isConnected: Boolean = false

    @Volatile
    private var isRegistered: Boolean = false

    val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onConnectivityChange()
        }
    }

    companion object {
        val EXECUTOR = AsyncTask.SERIAL_EXECUTOR
    }

    override fun register(): Boolean {
        EXECUTOR.execute {
            isConnected = isConnected()
            try {
                context.registerReceiver(
                    connectivityReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )
                isRegistered = true
            } catch (_: SecurityException) {
                isRegistered = false
            }
        }
        return true
    }

    override fun unregister() {
        EXECUTOR.execute {
            if (!isRegistered) {
                return@execute
            }
            isRegistered = false
            context.unregisterReceiver(connectivityReceiver)
        }
    }

    fun onConnectivityChange() {
        EXECUTOR.execute {
            val wasConnected = isConnected
            isConnected = isConnected()
            if (wasConnected != isConnected) {
                notifyChangeOnUiThread(isConnected)
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun isConnected(): Boolean {
        return try {
            val networkInfo = connectivityManager.get().activeNetworkInfo
            networkInfo != null && networkInfo.isConnected()
        } catch (_: RuntimeException) {
            //
            true
        }
    }

    fun notifyChangeOnUiThread(isConnected: Boolean) {
        Util.postOnUiThread {
            listener.onConnectivityChanged(isConnected)
        }
    }

}