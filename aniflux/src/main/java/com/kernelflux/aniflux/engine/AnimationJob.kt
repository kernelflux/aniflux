package com.kernelflux.aniflux.engine

import android.content.Context
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import android.os.Handler
import android.os.Looper
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationExecutor
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.load.GifAnimationLoader
import com.kernelflux.aniflux.load.LottieAnimationLoader
import com.kernelflux.aniflux.load.OkHttpAnimationDownloader
import com.kernelflux.aniflux.load.PAGAnimationLoader
import com.kernelflux.aniflux.load.SVGAAnimationLoader
import com.kernelflux.aniflux.load.VAPAnimationLoader
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.AnimationKey
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.util.concurrent.ExecutorService
import androidx.core.net.toUri
import com.kernelflux.aniflux.cache.AnimationDiskCache

/**
 * Animation task
 * Manages the complete lifecycle of a single animation request
 */
class AnimationJob<T>(
    private val engine: AnimationEngine,
    private val context: Context,
    private val model: Any?,
    private val target: AnimationTarget<T>,
    private val options: AnimationOptions,
    private val key: AnimationKey,
    private val listener: AnimationRequestListener<T>?,
    private val callback: AnimationResourceCallback? = null,
    private val animationDiskCache: AnimationDiskCache? = null,
    private val diskCachedFile: java.io.File? = null
) {

    companion object {
        private const val TAG = "AnimationJob"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // Downloader
    private val downloader: AnimationDownloader = OkHttpAnimationDownloader()
    
    // Download and cache helper class
    private val downloadHelper = AnimationJobDownloadHelper(context, key, animationDiskCache, downloader)

    // State management
    @Volatile
    private var isCancelled = false

    @Volatile
    private var isComplete = false

    @Volatile
    private var hasResource = false

    @Volatile
    private var hasLoadFailed = false

    private var resource: AnimationResource<T>? = null
    private var exception: Throwable? = null
    private var dataSource: AnimationDataSource = AnimationDataSource.LOCAL

    // Add callback list management
    private val callbacks = mutableListOf<AnimationResourceCallback>()

    init {
        // Add first callback to list
        callback?.let { callbacks.add(it) }
    }

    /**
     * Add callback (for waiting requests)
     * Reference Glide EngineJob.addCallback()
     */
    @Synchronized
    fun addCallback(cb: AnimationResourceCallback) {
        if (isCancelled || isComplete) {
            return
        }

        callbacks.add(cb)

        // If resource is already ready, immediately notify newly added callback
        if (hasResource && resource != null) {
            // Acquire in advance to avoid resource being recycled
            resource!!.acquire()
            mainHandler.post {
                try {
                    cb.onResourceReady(resource!!, dataSource, false)
                } catch (e: Exception) {
                    AniFluxLog.e(AniFluxLogCategory.ENGINE, "Error in callback onResourceReady", e)
                    // If error occurs, release resource
                    resource?.release()
                }
            }
        } else if (hasLoadFailed && exception != null) {
            mainHandler.post {
                try {
                    cb.onLoadFailed(exception!!)
                } catch (e: Exception) {
                    AniFluxLog.e(AniFluxLogCategory.ENGINE, "Error in callback onLoadFailed", e)
                }
            }
        }
    }

    /**
     * Start task
     */
    fun start() {
        if (isCancelled || isComplete) return

        val executor = selectExecutor()
        executor.execute {
            try {
                executeTask()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Select executor
     */
    private fun selectExecutor(): ExecutorService {
        return AnimationExecutor.getSourceExecutor()
    }

    /**
     * Execute task
     */
    private fun executeTask() {
        if (isCancelled) return
        val result = loadAnimation()
        if (isCancelled) return
        handleSuccess(result)
    }

    /**
     * Create corresponding loader based on animation type
     */
    private fun createLoader(animationType: AnimationTypeDetector.AnimationType): AnimationLoader<*>? {
        return when (animationType) {
            AnimationTypeDetector.AnimationType.GIF -> GifAnimationLoader()
            AnimationTypeDetector.AnimationType.LOTTIE -> LottieAnimationLoader()
            AnimationTypeDetector.AnimationType.PAG -> PAGAnimationLoader()
            AnimationTypeDetector.AnimationType.SVGA -> SVGAAnimationLoader()
            AnimationTypeDetector.AnimationType.VAP -> VAPAnimationLoader()
            AnimationTypeDetector.AnimationType.UNKNOWN -> null
        }
    }

    /**
     * Load animation - integrate specific animation loading logic
     * References loading approaches of various animation libraries, supports GIF, Lottie, SVGA, PAG, VAP and other animation types
     * 
     * Cache flow:
     * 1. If diskCachedFile exists, load from disk cache
     * 2. Otherwise, load based on model type (network/local)
     * 3. If network resource and disk cache needed, save to disk cache
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadAnimation(): AnimationResource<T> {
        try {
            // 1. Detect animation type
            val animationType = detectAnimationType()

            // 2. Create corresponding loader
            val loader = createLoader(animationType)
                ?: throw IllegalArgumentException("Unsupported animation type: $animationType")

            // 3. Load animation based on diskCachedFile or model type
            val animation = when {
                // Prefer disk cache file
                diskCachedFile != null -> {
                    dataSource = AnimationDataSource.DISK_CACHE
                    loadFromFile(loader, diskCachedFile)
                }
                model is String -> {
                    val pathType = AnimationTypeDetector.detectPathType(model)
                    when (pathType) {
                        AnimationTypeDetector.PathType.NETWORK_URL -> {
                            // Network URL: download and save to disk cache
                            val (downloadedFile, isFromCache) = downloadHelper.downloadAndCache(model)
                            if (downloadedFile != null) {
                                // Determine data source: if from cache, then DISK_CACHE, otherwise REMOTE
                                dataSource = if (isFromCache) {
                                    AnimationDataSource.DISK_CACHE
                                } else {
                                    AnimationDataSource.REMOTE
                                }
                                loadFromFile(loader, downloadedFile)
                            } else {
                                null
                            }
                        }

                        AnimationTypeDetector.PathType.LOCAL_FILE -> {
                            // Local file path
                            dataSource = AnimationDataSource.LOCAL
                            loadFromPath(loader, model)
                        }

                        AnimationTypeDetector.PathType.ASSET_PATH -> {
                            // Asset path
                            dataSource = AnimationDataSource.LOCAL
                            val assetPath = model.replace("file:///android_asset/", "")
                                .replace("asset://", "")
                            loadFromAssetPath(loader, assetPath)
                        }

                        AnimationTypeDetector.PathType.ASSET_URI -> {
                            // Asset URI
                            dataSource = AnimationDataSource.LOCAL
                            loadFromAssetUri(loader, model)
                        }

                        AnimationTypeDetector.PathType.CONTENT_URI -> {
                            // Content URI
                            dataSource = AnimationDataSource.LOCAL
                            loadFromContentUri(loader, model)
                        }

                        else -> {
                            // Default: handle as file path
                            dataSource = AnimationDataSource.LOCAL
                            loadFromPath(loader, model)
                        }
                    }
                }
                model is java.io.File -> {
                    // File
                    dataSource = AnimationDataSource.LOCAL
                    loadFromFile(loader, model)
                }
                model is android.net.Uri -> {
                    // URI
                    dataSource = AnimationDataSource.LOCAL
                    loadFromUri(loader, model)
                }
                model is Int -> {
                    // Resource ID
                    dataSource = AnimationDataSource.LOCAL
                    loadFromResource(loader, model)
                }
                model is ByteArray -> {
                    // Byte array
                    dataSource = AnimationDataSource.LOCAL
                    loadFromBytes(loader, model)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported model type: ${model?.javaClass}")
                }
            }
            // 4. Create AnimationResource
            val animationResult = animation as? T
            if (animationResult != null) {
                // Create ResourceListener, notify Engine when resource is released
                val resourceListener = object : AnimationResource.ResourceListener {
                    override fun onResourceReleased(key: String, resource: AnimationResource<*>) {
                        engine.onResourceReleased(this@AnimationJob.key, resource)
                    }
                }
                return AnimationResource<T>(animationResult, true, key.toString(), resourceListener)
            } else {
                throw IllegalStateException("Failed to load animation")
            }

        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.ENGINE, "Failed to load animation", e)
            throw e
        }
    }

    /**
     * Detect animation type
     */
    private fun detectAnimationType(): AnimationTypeDetector.AnimationType {
        return when (model) {
            is String -> {
                AnimationTypeDetector.detectFromPath(model)
            }

            is java.io.File -> {
                AnimationTypeDetector.detectFromPath(model.absolutePath)
            }

            is android.net.Uri -> {
                AnimationTypeDetector.detectFromPath(model.toString())
            }

            is Int -> {
                AnimationTypeDetector.detectFromResourceId(context, model)
            }

            is ByteArray -> {
                AnimationTypeDetector.detectFromBytes(model, model.size)
            }

            else -> {
                AnimationTypeDetector.AnimationType.UNKNOWN
            }
        }
    }

    /**
     * Check if it is a network URL
     */
    private fun isNetworkUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    /**
     * Load animation from URL
     */
    private fun loadFromUrl(loader: AnimationLoader<*>, url: String): Any? {
        return when (loader.getAnimationType()) {
            AnimationTypeDetector.AnimationType.GIF -> {
                (loader as GifAnimationLoader).loadFromUrl(
                    context,
                    url,
                    downloader
                )
            }

            AnimationTypeDetector.AnimationType.LOTTIE -> {
                val loadFromUrl = (loader as LottieAnimationLoader).loadFromUrl(
                    context,
                    url,
                    downloader
                )
                loadFromUrl
            }

            AnimationTypeDetector.AnimationType.PAG -> {
                (loader as PAGAnimationLoader).loadFromUrl(
                    context,
                    url,
                    downloader
                )
            }

            AnimationTypeDetector.AnimationType.SVGA -> {
                (loader as SVGAAnimationLoader).apply {
                    setContext(context)
                }.loadFromUrl(context, url, downloader)
            }

            else -> null
        }
    }

    /**
     * Load animation from file path
     */
    private fun loadFromPath(loader: AnimationLoader<*>, path: String): Any? {
        return when (loader.getAnimationType()) {
            AnimationTypeDetector.AnimationType.GIF -> {
                (loader as GifAnimationLoader).loadFromPath(context, path)
            }

            AnimationTypeDetector.AnimationType.LOTTIE -> {
                (loader as LottieAnimationLoader).loadFromPath(context, path)
            }

            AnimationTypeDetector.AnimationType.PAG -> {
                (loader as PAGAnimationLoader).loadFromPath(context, path)
            }

            AnimationTypeDetector.AnimationType.SVGA -> {
                (loader as SVGAAnimationLoader).apply {
                    setContext(context)
                }.loadFromPath(context, path)
            }

            else -> null
        }
    }

    /**
     * Load animation from file
     */
    private fun loadFromFile(loader: AnimationLoader<*>, file: java.io.File): Any? {
        return when (loader.getAnimationType()) {
            AnimationTypeDetector.AnimationType.GIF -> {
                (loader as GifAnimationLoader).loadFromFile(context, file)
            }

            AnimationTypeDetector.AnimationType.LOTTIE -> {
                (loader as LottieAnimationLoader).loadFromFile(context, file)
            }

            AnimationTypeDetector.AnimationType.PAG -> {
                (loader as PAGAnimationLoader).loadFromFile(context, file)
            }

            AnimationTypeDetector.AnimationType.SVGA -> {
                (loader as SVGAAnimationLoader).apply {
                    setContext(context)
                }.loadFromFile(context, file)
            }

            else -> null
        }
    }

    /**
     * Load animation from URI
     */
    private fun loadFromUri(loader: AnimationLoader<*>, uri: android.net.Uri): Any? {
        return when (loader.getAnimationType()) {
            AnimationTypeDetector.AnimationType.GIF -> {
                (loader as GifAnimationLoader).loadFromPath(context, uri.toString())
            }

            AnimationTypeDetector.AnimationType.LOTTIE -> {
                (loader as LottieAnimationLoader).loadFromPath(context, uri.toString())
            }

            AnimationTypeDetector.AnimationType.PAG -> {
                (loader as PAGAnimationLoader).loadFromPath(context, uri.toString())
            }

            AnimationTypeDetector.AnimationType.SVGA -> {
                (loader as SVGAAnimationLoader).apply {
                    setContext(context)
                }.loadFromPath(context, uri.toString())
            }

            else -> null
        }
    }

    /**
     * Load animation from resource ID
     */
    private fun loadFromResource(loader: AnimationLoader<*>, resourceId: Int): Any? {
        return when (loader.getAnimationType()) {
            AnimationTypeDetector.AnimationType.GIF -> {
                (loader as GifAnimationLoader).loadFromResource(
                    context,
                    resourceId
                )
            }

            AnimationTypeDetector.AnimationType.LOTTIE -> {
                (loader as LottieAnimationLoader).loadFromResource(
                    context,
                    resourceId
                )
            }

            AnimationTypeDetector.AnimationType.PAG -> {
                (loader as PAGAnimationLoader).loadFromResource(
                    context,
                    resourceId
                )
            }

            AnimationTypeDetector.AnimationType.SVGA -> {
                (loader as SVGAAnimationLoader).apply {
                    setContext(context)
                }.loadFromResource(context, resourceId)
            }

            else -> null
        }
    }

    /**
     * Load animation from byte array
     */
    private fun loadFromBytes(loader: AnimationLoader<*>, bytes: ByteArray): Any? {
        return when (loader.getAnimationType()) {
            AnimationTypeDetector.AnimationType.GIF -> {
                (loader as GifAnimationLoader).loadFromBytes(context, bytes)
            }

            AnimationTypeDetector.AnimationType.LOTTIE -> {
                (loader as LottieAnimationLoader).loadFromBytes(context, bytes)
            }

            AnimationTypeDetector.AnimationType.PAG -> {
                (loader as PAGAnimationLoader).loadFromBytes(context, bytes)
            }

            AnimationTypeDetector.AnimationType.SVGA -> {
                (loader as SVGAAnimationLoader).apply {
                    setContext(context)
                }.loadFromBytes(context, bytes)
            }

            else -> null
        }
    }

    /**
     * Load animation from Asset path
     */
    private fun loadFromAssetPath(loader: AnimationLoader<*>, assetPath: String): Any? {
        return when (loader.getAnimationType()) {
            AnimationTypeDetector.AnimationType.GIF -> {
                (loader as GifAnimationLoader).loadFromAssetPath(
                    context,
                    assetPath
                )
            }

            AnimationTypeDetector.AnimationType.LOTTIE -> {
                (loader as LottieAnimationLoader).loadFromAssetPath(
                    context,
                    assetPath
                )
            }

            AnimationTypeDetector.AnimationType.PAG -> {
                (loader as PAGAnimationLoader).loadFromAssetPath(
                    context,
                    assetPath
                )
            }

            AnimationTypeDetector.AnimationType.SVGA -> {
                (loader as SVGAAnimationLoader).apply {
                    setContext(context)
                }.loadFromAssetPath(context, assetPath)
            }

            AnimationTypeDetector.AnimationType.VAP -> {
                (loader as VAPAnimationLoader).loadFromAssetPath(context, assetPath)
            }

            else -> null
        }
    }

    /**
     * Load animation from Asset URI
     */
    private fun loadFromAssetUri(loader: AnimationLoader<*>, assetUri: String): Any? {
        // Convert file:///android_asset/animations/loading.gif to animations/loading.gif
        val assetPath = assetUri.replace("file:///android_asset/", "")
            .replace("asset://","")
        return loadFromAssetPath(loader, assetPath)
    }

    /**
     * Load animation from Content URI
     */
    private fun loadFromContentUri(loader: AnimationLoader<*>, contentUri: String): Any? {
        return try {
            val uri = contentUri.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val bytes = inputStream.readBytes()
                inputStream.close()
                loadFromBytes(loader, bytes)
            } else {
                null
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.ENGINE, "Failed to load from content URI: $contentUri", e)
            null
        }
    }

    /**
     * Handle success
     */
    private fun handleSuccess(result: AnimationResource<T>) {
        if (isCancelled) return

        synchronized(this) {
            if (isCancelled || hasResource || hasLoadFailed) return

            this.resource = result
            hasResource = true
            isComplete = true
        }

        // ✅ Notify engine that task is complete (Engine will call acquire in onJobComplete)
        engine.onJobComplete(key, result)

        // Notify callbacks
        notifyCallbacksOfResult()
    }

    /**
     * Handle error
     */
    private fun handleError(error: Throwable) {
        if (isCancelled) return

        synchronized(this) {
            if (isCancelled || hasResource || hasLoadFailed) return

            this.exception = error
            hasLoadFailed = true
            isComplete = true
        }

        // Notify engine that task is complete (failed)
        engine.onJobComplete<T>(key, null)

        // Notify callbacks
        notifyCallbacksOfException()
    }

    /**
     * Notify success callbacks
     */
    private fun notifyCallbacksOfResult() {
        val resource = this.resource ?: return

        // Copy list to avoid concurrent modification
        val callbacksCopy = synchronized(this) {
            if (isCancelled) {
                resource.recycle()
                return
            } else if (callbacks.isEmpty()) {
                // If no callback, notify target and listener (for backward compatibility)
                mainHandler.post {
                    try {
                        target.onResourceReady(resource.get())
                        listener?.onResourceReady(
                            resource.get(),
                            model,
                            target,
                            dataSource,
                            false
                        )
                    } catch (e: Exception) {
                        AniFluxLog.e(AniFluxLogCategory.ENGINE, "Error in success callback", e)
                    }
                }
                return
            }
            // Note: hasResource has already been set to true in handleSuccess(), no need to set again here
            callbacks.toList()  // Copy list
        }

        // Notify all callbacks
        mainHandler.post {
            callbacksCopy.forEach { cb ->
                try {
                    // Each callback needs to acquire
                    resource.acquire()
                    cb.onResourceReady(resource, dataSource, false)
                } catch (e: Exception) {
                    AniFluxLog.e(AniFluxLogCategory.ENGINE, "Error in callback onResourceReady", e)
                    // If error, release resource
                    resource.release()
                }
            }
        }
    }

    /**
     * Notify failure callbacks
     */
    private fun notifyCallbacksOfException() {
        val exception = this.exception ?: return

        // Copy list to avoid concurrent modification
        val callbacksCopy = synchronized(this) {
            if (isCancelled) {
                return
            } else if (callbacks.isEmpty()) {
                // If no callback, notify target and listener (for backward compatibility)
                mainHandler.post {
                    try {
                        target.onLoadFailed(null)
                        listener?.onLoadFailed(exception, model, target, false)
                    } catch (e: Exception) {
                        AniFluxLog.e(AniFluxLogCategory.ENGINE, "Error in failure callback", e)
                    }
                }
                return
            }
            // Note: hasLoadFailed has already been set to true in handleError(), no need to set again here
            callbacks.toList()  // Copy list
        }

        // Notify all callbacks
        mainHandler.post {
            callbacksCopy.forEach { cb ->
                try {
                    cb.onLoadFailed(exception)
                } catch (e: Exception) {
                    AniFluxLog.e(AniFluxLogCategory.ENGINE, "Error in callback onLoadFailed", e)
                }
            }
        }
    }

    /**
     * Remove callback
     */
    @Synchronized
    fun removeCallback(cb: AnimationResourceCallback?) {
        if (cb != null) {
            callbacks.remove(cb)
        }
    }

    /**
     * Cancel task
     */
    fun cancel() {
        if (isCancelled || isComplete) return

        isCancelled = true

        // ✅ When canceling a task, if there is a resource, release it (Job releases resource)
        val currentResource = resource
        resource = null
        currentResource?.release()

        exception = null
    }
}
