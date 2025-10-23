package com.kernelflux.aniflux.load

import android.content.Context
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.kernelflux.aniflux.util.AnimationTypeDetector
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Lottie动画加载器 - 参考lottie-android库的加载方式
 * 支持从文件路径、文件、资源ID、字节数组、输入流、网络URL加载Lottie
 */
class LottieAnimationLoader : AnimationLoader<LottieDrawable> {

    override fun loadFromPath(context: Context,path: String): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromPath(context,path)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e("LottieAnimationLoader", "Failed to load Lottie from path: $path", e)
            null
        }
    }

    override fun loadFromFile(context: Context,file: File): LottieDrawable? {
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

    override fun loadFromBytes(context: Context,bytes: ByteArray): LottieDrawable? {
        return try {
            val composition = loadLottieCompositionFromBytes(bytes)
            composition?.let { createLottieDrawable(it) }
        } catch (e: Exception) {
            android.util.Log.e("LottieAnimationLoader", "Failed to load Lottie from bytes", e)
            null
        }
    }

    override fun loadFromInputStream(context: Context,inputStream: InputStream): LottieDrawable? {
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
            val tempFile=downloader.download(context,url)
            // 从临时文件加载
            val result = loadFromFile(context,tempFile)
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
    private fun loadLottieCompositionFromPath(context:Context,path: String): LottieComposition? {
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

        ZipInputStream(FileInputStream(file)).use { zis ->
            LottieCompositionFactory.fromZipStream(zis, null)
                .addListener { comp ->
//                    lottieView.repeatCount = LottieDrawable.INFINITE
//                    lottieView.setComposition(comp)
//                    lottieView.playAnimation()
                }
                .addFailureListener { e -> e.printStackTrace() }
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

//        LottieCompositionFactory.fromAsset(String(bytes))
//            .addListener { composition ->
//                result = composition
//                latch.countDown()
//            }
//            .addFailureListener {
//                latch.countDown()
//            }

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

        LottieCompositionFactory.fromJsonInputStream(inputStream, null)
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
