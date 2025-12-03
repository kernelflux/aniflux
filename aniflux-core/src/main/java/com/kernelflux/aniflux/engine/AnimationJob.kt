package com.kernelflux.aniflux.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationExecutor
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.load.OkHttpAnimationDownloader
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.AnimationKey
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.util.concurrent.ExecutorService
import androidx.core.net.toUri
import com.kernelflux.aniflux.cache.AnimationDiskCache

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
    private val animationDiskCache: AnimationDiskCache? = null,
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

    //添加 callback 列表管理
    private val callbacks = mutableListOf<AnimationResourceCallback>()

    init {
        // 将第一个 callback 添加到列表
        callback?.let { callbacks.add(it) }
    }

    /**
     * 添加 callback（供等待的请求使用）
     * 参考 Glide EngineJob.addCallback()
     */
    @Synchronized
    fun addCallback(cb: AnimationResourceCallback) {
        if (isCancelled || isComplete) {
            return
        }

        callbacks.add(cb)

        // 如果资源已经准备好，立即通知新添加的 callback
        if (hasResource && resource != null) {
            // 提前 acquire，避免资源被回收
            resource!!.acquire()
            mainHandler.post {
                try {
                    cb.onResourceReady(resource!!, dataSource, false)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in callback onResourceReady", e)
                    // 如果出错，释放资源
                    resource?.release()
                }
            }
        } else if (hasLoadFailed && exception != null) {
            mainHandler.post {
                try {
                    cb.onLoadFailed(exception!!)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in callback onLoadFailed", e)
                }
            }
        }
    }

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
        return AnimationExecutor.getSourceExecutor()
    }

    /**
     * 执行任务
     */
    private fun executeTask() {
        if (isCancelled) return
        val result = loadAnimation()
        if (isCancelled) return
        handleSuccess(result)
    }

    /**
     * 根据动画类型创建对应的加载器
     */
    /**
     * 创建对应的加载器
     * 从 LoaderRegistry 动态获取，而不是直接实例化
     */
    private fun createLoader(animationType: AnimationTypeDetector.AnimationType): AnimationLoader<*>? {
        return com.kernelflux.aniflux.registry.LoaderRegistry.get(animationType)
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
        return loader.loadFromUrl(context, url, downloader)
    }

    /**
     * 从文件路径加载动画
     */
    private fun loadFromPath(loader: AnimationLoader<*>, path: String): Any? {
        return loader.loadFromPath(context, path)
    }

    /**
     * 从文件加载动画
     */
    private fun loadFromFile(loader: AnimationLoader<*>, file: java.io.File): Any? {
        return loader.loadFromFile(context, file)
    }

    /**
     * 从URI加载动画
     */
    private fun loadFromUri(loader: AnimationLoader<*>, uri: android.net.Uri): Any? {
        return loader.loadFromPath(context, uri.toString())
    }

    /**
     * 从资源ID加载动画
     */
    private fun loadFromResource(loader: AnimationLoader<*>, resourceId: Int): Any? {
        return loader.loadFromResource(context, resourceId)
    }

    /**
     * 从字节数组加载动画
     */
    private fun loadFromBytes(loader: AnimationLoader<*>, bytes: ByteArray): Any? {
        return loader.loadFromBytes(context, bytes)
    }

    /**
     * 从Asset路径加载动画
     */
    private fun loadFromAssetPath(loader: AnimationLoader<*>, assetPath: String): Any? {
        return loader.loadFromAssetPath(context, assetPath)
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

        // 复制列表，避免并发修改
        val callbacksCopy = synchronized(this) {
            if (isCancelled) {
                resource.recycle()
                return
            } else if (callbacks.isEmpty()) {
                // 如果没有 callback，通知 target 和 listener（保持向后兼容）
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
                return
            }
            // 注意：hasResource 已经在 handleSuccess() 中设置为 true，这里不需要再次设置
            callbacks.toList()  // 复制列表
        }

        // 通知所有 callback
        mainHandler.post {
            callbacksCopy.forEach { cb ->
                try {
                    // 每个 callback 都需要 acquire
                    resource.acquire()
                    cb.onResourceReady(resource, dataSource, false)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in callback onResourceReady", e)
                    // 如果出错，释放资源
                    resource.release()
                }
            }
        }
    }

    /**
     * 通知失败回调
     */
    private fun notifyCallbacksOfException() {
        val exception = this.exception ?: return

        // 复制列表，避免并发修改
        val callbacksCopy = synchronized(this) {
            if (isCancelled) {
                return
            } else if (callbacks.isEmpty()) {
                // 如果没有 callback，通知 target 和 listener（保持向后兼容）
                mainHandler.post {
                    try {
                        target.onLoadFailed(null)
                        listener?.onLoadFailed(exception, model, target, false)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error in failure callback", e)
                    }
                }
                return
            }
            // 注意：hasLoadFailed 已经在 handleError() 中设置为 true，这里不需要再次设置
            callbacks.toList()  // 复制列表
        }

        // 通知所有 callback
        mainHandler.post {
            callbacksCopy.forEach { cb ->
                try {
                    cb.onLoadFailed(exception)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in callback onLoadFailed", e)
                }
            }
        }
    }

    /**
     * 移除回调
     */
    @Synchronized
    fun removeCallback(cb: AnimationResourceCallback?) {
        if (cb != null) {
            callbacks.remove(cb)
        }
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
