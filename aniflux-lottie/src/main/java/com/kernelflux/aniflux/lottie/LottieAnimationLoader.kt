package com.kernelflux.aniflux.lottie

import android.content.Context
import com.kernelflux.aniflux.annotation.AutoRegisterLoader
import com.kernelflux.aniflux.load.AnimationDownloader
import com.kernelflux.aniflux.load.AnimationLoader
import com.kernelflux.aniflux.util.AnimationTypeDetector
import com.kernelflux.lottie.LottieComposition
import com.kernelflux.lottie.LottieCompositionFactory
import com.kernelflux.lottie.LottieDrawable
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Lottie动画加载器 - 参考lottie-android库的加载方式
 * 支持从文件路径、文件、资源ID、字节数组、输入流、网络URL加载Lottie
 */
@AutoRegisterLoader(animationType = "LOTTIE")
class LottieAnimationLoader : AnimationLoader<LottieDrawable> {

    override fun loadFromPath(context: Context, path: String): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromPath(context, path)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e("LottieAnimationLoader", "Failed to load Lottie from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context, file: File): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromFile(file)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Failed to load Lottie from file: ${file.absolutePath}",
                e
            )
            null
        }
    }

    override fun loadFromResource(context: Context, resourceId: Int): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromResource(context, resourceId)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Failed to load Lottie from resource: $resourceId",
                e
            )
            null
        }
    }

    override fun loadFromBytes(context: Context, bytes: ByteArray): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromBytes(bytes)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e("LottieAnimationLoader", "Failed to load Lottie from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context, inputStream: InputStream): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromInputStream(inputStream)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Failed to load Lottie from input stream",
                e
            )
            null
        }
    }

    override fun loadFromUrl(
        context: Context,
        url: String,
        downloader: AnimationDownloader
    ): LottieDrawable? {
        return try {
            // 下载文件
            val tempFile = downloader.download(context, url)
            // 从临时文件加载
            val result = loadFromFile(context, tempFile)
            result
        } catch (e: Exception) {
            android.util.Log.e("LottieAnimationLoader", "Failed to load Lottie from URL: $url", e)
            null
        }
    }

    override fun loadFromAssetPath(context: Context, assetPath: String): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromAssetPath(context, assetPath)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Failed to load Lottie from asset path: $assetPath",
                e
            )
            null
        }
    }

    /**
     * 从Asset路径加载LottieComposition
     */
    private fun loadLottieCompositionFromAssetPath(
        context: Context,
        assetPath: String
    ): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromAsset(context, assetPath)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Interrupted while loading Lottie from asset path",
                e
            )
            null
        }
    }

    override fun getAnimationType(): AnimationTypeDetector.AnimationType {
        return AnimationTypeDetector.AnimationType.LOTTIE
    }

    /**
     * 从文件路径加载LottieComposition
     */
    private fun loadLottieCompositionFromPath(context: Context, path: String): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromAsset(context, path)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Interrupted while loading Lottie from path",
                e
            )
            null
        }
    }

    /**
     * 从文件加载LottieComposition
     */
    private fun loadLottieCompositionFromFile(file: File): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        // 先读取文件内容到字节数组，避免流关闭问题
        val bytes = file.readBytes()
        
        // 根据文件扩展名判断是 JSON 还是 ZIP 格式
        val fileName = file.name.lowercase()
        if (fileName.endsWith(".zip") || fileName.endsWith(".lottie")) {
            // ZIP 格式的 Lottie 文件
            val zis = ZipInputStream(bytes.inputStream())
            LottieCompositionFactory.fromZipStream(zis, null)
                .addListener { comp ->
                    result = comp
                    latch.countDown()
                }
                .addFailureListener { e -> 
                    e.printStackTrace()
                    latch.countDown()
                }
        } else {
            // JSON 格式的 Lottie 文件
            val jsonString = String(bytes)
            LottieCompositionFactory.fromJsonString(jsonString, null)
                .addListener { comp ->
                    result = comp
                    latch.countDown()
                }
                .addFailureListener { e -> 
                    e.printStackTrace()
                    latch.countDown()
                }
        }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Interrupted while loading Lottie from file",
                e
            )
            null
        }
    }

    /**
     * 从资源ID加载LottieComposition
     */
    private fun loadLottieCompositionFromResource(
        context: Context,
        resourceId: Int
    ): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromRawRes(context, resourceId)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Interrupted while loading Lottie from resource",
                e
            )
            null
        }
    }

    /**
     * 从字节数组加载LottieComposition
     */
    private fun loadLottieCompositionFromBytes(bytes: ByteArray): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        LottieCompositionFactory.fromJsonString(String(bytes), null)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Interrupted while loading Lottie from bytes",
                e
            )
            null
        }
    }

    /**
     * 从输入流加载LottieComposition
     */
    private fun loadLottieCompositionFromInputStream(inputStream: InputStream): LottieComposition? {
        val latch = CountDownLatch(1)
        var result: LottieComposition? = null

        // 先读取输入流内容到字节数组，避免流关闭问题
        val bytes = inputStream.readBytes()
        val jsonInputStream = bytes.inputStream()

        LottieCompositionFactory.fromJsonInputStream(jsonInputStream, null)
            .addListener { composition ->
                result = composition
                latch.countDown()
            }
            .addFailureListener {
                latch.countDown()
            }

        return try {
            latch.await(10, TimeUnit.SECONDS)
            result
        } catch (e: InterruptedException) {
            android.util.Log.e(
                "LottieAnimationLoader",
                "Interrupted while loading Lottie from input stream",
                e
            )
            null
        }
    }

    /**
     * 创建LottieDrawable
     */
    private fun createLottieDrawable(composition: LottieComposition): LottieDrawable {
        val drawable = LottieDrawable()
        drawable.setComposition(composition)
        return drawable
    }
}

