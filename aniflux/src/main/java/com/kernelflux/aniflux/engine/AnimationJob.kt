package com.kernelflux.aniflux.engine

import android.content.Context
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

/**
 * 动画任务
 * 管理单个动画请求的完整生命周期
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
    private val animationDiskCache: com.kernelflux.aniflux.cache.AnimationDiskCache? = null,
    private val diskCachedFile: java.io.File? = null
) {

    companion object {
        private const val TAG = "AnimationJob"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // 下载器
    private val downloader: AnimationDownloader = OkHttpAnimationDownloader()
    
    // 下载和缓存辅助类
    private val downloadHelper = AnimationJobDownloadHelper(context, key, animationDiskCache, downloader)

    // 状态管理
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

    /**
     * 启动任务
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
     * 选择执行器
     */
    private fun selectExecutor(): ExecutorService {
        return when {
            options.useDiskCache -> AnimationExecutor.getDiskCacheExecutor()
            options.isAnimation -> AnimationExecutor.getAnimationExecutor()
            else -> AnimationExecutor.getSourceExecutor()
        }
    }

    /**
     * 执行任务
     */
    private fun executeTask() {
        if (isCancelled) return

        // 这里应该调用具体的动画加载逻辑
        // 暂时使用简单的模拟
        val result = loadAnimation()

        if (isCancelled) return

        handleSuccess(result)
    }

    /**
     * 根据动画类型创建对应的加载器
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
     * 加载动画 - 集成具体的动画加载逻辑
     * 参考各动画库的加载方式，支持GIF、Lottie、SVGA、PAG、VAP等动画类型
     * 
     * 缓存流程：
     * 1. 如果 diskCachedFile 存在，从磁盘缓存加载
     * 2. 否则，根据 model 类型加载（网络/本地）
     * 3. 如果是网络资源且需要磁盘缓存，保存到磁盘缓存
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadAnimation(): AnimationResource<T> {
        try {
            // 1. 检测动画类型
            val animationType = detectAnimationType()

            // 2. 创建对应的加载器
            val loader = createLoader(animationType)
                ?: throw IllegalArgumentException("Unsupported animation type: $animationType")

            // 3. 根据 diskCachedFile 或 model 类型加载动画
            val animation = when {
                // 优先使用磁盘缓存文件
                diskCachedFile != null -> {
                    dataSource = AnimationDataSource.DISK_CACHE
                    loadFromFile(loader, diskCachedFile)
                }
                model is String -> {
                    val pathType = AnimationTypeDetector.detectPathType(model)
                    when (pathType) {
                        AnimationTypeDetector.PathType.NETWORK_URL -> {
                            // 网络URL：下载并保存到磁盘缓存
                            val (downloadedFile, isFromCache) = downloadHelper.downloadAndCache(model)
                            if (downloadedFile != null) {
                                // 判断数据来源：如果是从缓存获取，则是 DISK_CACHE，否则是 REMOTE
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
                            // 本地文件路径
                            dataSource = AnimationDataSource.LOCAL
                            loadFromPath(loader, model)
                        }

                        AnimationTypeDetector.PathType.ASSET_PATH -> {
                            // Asset路径
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
                            // 默认按文件路径处理
                            dataSource = AnimationDataSource.LOCAL
                            loadFromPath(loader, model)
                        }
                    }
                }
                model is java.io.File -> {
                    // 文件
                    dataSource = AnimationDataSource.LOCAL
                    loadFromFile(loader, model)
                }
                model is android.net.Uri -> {
                    // URI
                    dataSource = AnimationDataSource.LOCAL
                    loadFromUri(loader, model)
                }
                model is Int -> {
                    // 资源ID
                    dataSource = AnimationDataSource.LOCAL
                    loadFromResource(loader, model)
                }
                model is ByteArray -> {
                    // 字节数组
                    dataSource = AnimationDataSource.LOCAL
                    loadFromBytes(loader, model)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported model type: ${model?.javaClass}")
                }
            }
            // 4. 创建AnimationResource
            val animationResult = animation as? T
            if (animationResult != null) {
                // 创建ResourceListener，当资源释放时通知Engine
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
            android.util.Log.e(TAG, "Failed to load animation", e)
            throw e
        }
    }

    /**
     * 检测动画类型
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
     * 判断是否为网络URL
     */
    private fun isNetworkUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    /**
     * 从URL加载动画
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
     * 从文件路径加载动画
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
     * 从文件加载动画
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
     * 从URI加载动画
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
     * 从资源ID加载动画
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
     * 从字节数组加载动画
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
     * 从Asset路径加载动画
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
     * 从Asset URI加载动画
     */
    private fun loadFromAssetUri(loader: AnimationLoader<*>, assetUri: String): Any? {
        // 将 file:///android_asset/animations/loading.gif 转换为 animations/loading.gif
        val assetPath = assetUri.replace("file:///android_asset/", "")
            .replace("asset://","")
        return loadFromAssetPath(loader, assetPath)
    }

    /**
     * 从Content URI加载动画
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
            android.util.Log.e(TAG, "Failed to load from content URI: $contentUri", e)
            null
        }
    }

    /**
     * 处理成功
     */
    private fun handleSuccess(result: AnimationResource<T>) {
        if (isCancelled) return

        synchronized(this) {
            if (isCancelled || hasResource || hasLoadFailed) return

            this.resource = result
            hasResource = true
            isComplete = true
        }

        // ✅ 通知引擎任务完成（Engine 会在 onJobComplete 中调用 acquire）
        engine.onJobComplete(key, result)

        // 通知回调
        notifyCallbacksOfResult()
    }

    /**
     * 处理错误
     */
    private fun handleError(error: Throwable) {
        if (isCancelled) return

        synchronized(this) {
            if (isCancelled || hasResource || hasLoadFailed) return

            this.exception = error
            hasLoadFailed = true
            isComplete = true
        }

        // 通知引擎任务完成（失败）
        engine.onJobComplete<T>(key, null)

        // 通知回调
        notifyCallbacksOfException()
    }

    /**
     * 通知成功回调
     */
    private fun notifyCallbacksOfResult() {
        val resource = this.resource ?: return

        // 如果有callback，优先通知callback
        if (callback != null) {
            mainHandler.post {
                try {
                    callback.onResourceReady(
                        resource,
                        dataSource,
                        false
                    )
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in callback onResourceReady", e)
                }
            }
            return
        }

        // 通知target和listener
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
                android.util.Log.e(TAG, "Error in success callback", e)
            }
        }
    }

    /**
     * 通知失败回调
     */
    private fun notifyCallbacksOfException() {
        val exception = this.exception ?: return

        // 如果有callback，优先通知callback（这是从SingleAnimationRequest传来的）
        if (callback != null) {
            mainHandler.post {
                try {
                    callback.onLoadFailed(exception)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in callback onLoadFailed", e)
                }
            }
            return
        }

        // 通知target和listener
        mainHandler.post {
            try {
                target.onLoadFailed(null)
                listener?.onLoadFailed(exception, model, target, false)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in failure callback", e)
            }
        }
    }

    /**
     * 移除回调（简化实现）
     */
    @Synchronized
    fun removeCallback(cb: AnimationResourceCallback?) {
        // 简化实现，暂时不需要复杂的回调管理
    }

    /**
     * 取消任务
     */
    fun cancel() {
        if (isCancelled || isComplete) return

        isCancelled = true

        // ✅ 取消任务时，如果有资源则 release（Job 释放资源）
        val currentResource = resource
        resource = null
        currentResource?.release()

        exception = null
    }
}
